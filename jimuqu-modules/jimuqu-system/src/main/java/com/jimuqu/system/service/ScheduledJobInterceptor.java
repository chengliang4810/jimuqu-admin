package com.jimuqu.system.service;

import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.java_cron.CronExpressionPlus;
import org.noear.java_cron.CronUtils;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.scheduling.annotation.Scheduled;
import org.noear.solon.scheduling.scheduled.Job;
import org.noear.solon.scheduling.scheduled.JobHandler;
import org.noear.solon.scheduling.scheduled.JobInterceptor;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务集群互斥、重试与执行日志。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobInterceptor implements JobInterceptor {

    public static final String MANUAL_TRIGGER = "jimuqu.scheduled.trigger";
    public static final String MANUAL_RUN_ID = "jimuqu.scheduled.runId";

    private static final String SCHEDULED = "SCHEDULED";
    private static final String MANUAL = "MANUAL";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String RETRY = "RETRY";
    private static final String SKIPPED = "SKIPPED";
    private static final long MARKER_TTL_GRACE_MS = TimeUnit.MINUTES.toMillis(1);
    private static final int ERROR_SUMMARY_LENGTH = 1000;
    private static final String REDIS_TIME_SCRIPT =
            "local t=redis.call('TIME'); return (t[1] * 1000) + math.floor(t[2] / 1000)";
    private static final String INSTANCE_ID = truncate(
            ManagementFactory.getRuntimeMXBean().getName(), 128);

    private final ScheduledJobConfigService configService;
    private final SysScheduledJobLogMapper logMapper;

    @Override
    public void doIntercept(Job job, JobHandler handler) throws Throwable {
        doIntercept(RedisUtils.getClient(), job, handler);
    }

    void doIntercept(RedissonClient client, Job job, JobHandler handler) throws Throwable {
        long startedAt = System.currentTimeMillis();
        String triggerType = MANUAL.equals(job.getContext().paramMap().get(MANUAL_TRIGGER))
                ? MANUAL : SCHEDULED;
        SysScheduledJobConfig config;
        try {
            config = configService.getOrCreate(job.getName(), job.getScheduled().enable());
        } catch (Throwable e) {
            recordInfrastructureFailure(job.getName(), triggerType, startedAt, e);
            throw e;
        }
        if (SCHEDULED.equals(triggerType) && !Boolean.TRUE.equals(config.getEnabled())) {
            record(job.getName(), SKIPPED, triggerType, 1, startedAt, null);
            return;
        }

        RLock lock = null;
        boolean executionStarted = false;
        Throwable interceptedFailure = null;
        try {
            String keyBase = redisKey("scheduled-job:{" + job.getName() + "}");
            boolean holdLockDuringExecution = holdsLockDuringExecution(job, triggerType);
            lock = client.getLock(keyBase
                    + (holdLockDuringExecution ? ":execution-lock" : ":claim-lock"));
            boolean lockAcquired = lock.tryLock();
            if (!lockAcquired) {
                record(job.getName(), SKIPPED, triggerType, 1, startedAt, null);
                return;
            }
            boolean claimed = claimExecution(client, keyBase, job, triggerType);
            if (!holdLockDuringExecution) {
                unlock(lock, null);
                lock = null;
            }
            if (!claimed) {
                record(job.getName(), SKIPPED, triggerType, 1, startedAt, null);
                return;
            }
            executionStarted = true;
            ExecutionSuccess success = null;
            Throwable executionFailure = null;
            try {
                try {
                    success = executeWithRetry(job, handler, config, triggerType);
                } catch (Throwable failure) {
                    executionFailure = failure;
                    throw failure;
                }
            } finally {
                long completedAtNanos = System.nanoTime();
                try {
                    completeFixedDelayExecution(client, keyBase, job, triggerType);
                } catch (RuntimeException completionFailure) {
                    awaitRemainingFixedDelay(
                            job.getScheduled().fixedDelay(), completedAtNanos);
                    if (executionFailure == null) {
                        recordInfrastructureFailure(
                                job.getName(), triggerType, success.attempt(),
                                success.startedAt(), completionFailure);
                        throw completionFailure;
                    }
                    executionFailure.addSuppressed(completionFailure);
                    log.error(
                            "定时任务执行失败，且更新 fixedDelay 完成标记失败，jobName={}",
                            job.getName(), completionFailure);
                }
            }
            recordSuccess(job.getName(), triggerType, success);
        } catch (Throwable e) {
            interceptedFailure = e;
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (!executionStarted) {
                recordInfrastructureFailure(job.getName(), triggerType, startedAt, e);
            }
            throw e;
        } finally {
            unlock(lock, interceptedFailure);
        }
    }

    private static boolean holdsLockDuringExecution(Job job, String triggerType) {
        return MANUAL.equals(triggerType) || job.getScheduled().fixedDelay() > 0;
    }

    private static void unlock(RLock lock, Throwable primaryFailure) throws Throwable {
        if (lock == null) {
            return;
        }
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Throwable unlockFailure) {
            if (primaryFailure == null) {
                throw unlockFailure;
            }
            if (primaryFailure != unlockFailure) {
                primaryFailure.addSuppressed(unlockFailure);
            }
            log.error("释放定时任务集群锁失败", unlockFailure);
        }
    }

    private static void awaitRemainingFixedDelay(long fixedDelayMs, long completedAtNanos) {
        if (fixedDelayMs <= 0) {
            return;
        }
        long deadline = completedAtNanos + TimeUnit.MILLISECONDS.toNanos(fixedDelayMs);
        boolean interrupted = false;
        try {
            long remaining;
            while ((remaining = deadline - System.nanoTime()) > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(remaining);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean claimExecution(RedissonClient client, String keyBase,
                                          Job job, String triggerType) {
        if (MANUAL.equals(triggerType)) {
            String runId = manualRunId(job);
            String markerKey = keyBase + ":manual:" + runId + ":marker";
            long claimTime = redisTimeMillis(client, markerKey);
            RBucket<Long> marker = client.getBucket(markerKey);
            return marker.setIfAbsent(claimTime, Duration.ofDays(1));
        }

        Scheduled scheduled = job.getScheduled();
        String markerKey = keyBase + ":scheduled:marker";
        long claimTime = redisTimeMillis(client, markerKey);
        RBucket<Long> marker = client.getBucket(markerKey);
        Long claimedCycle = marker.get();
        long markerTtlMillis;
        long cycleId;
        if (scheduled.fixedDelay() > 0) {
            long interval = scheduled.fixedDelay();
            if (claimedCycle != null && claimTime - claimedCycle < interval) {
                return false;
            }
            cycleId = claimTime;
            markerTtlMillis = addSaturated(interval, MARKER_TTL_GRACE_MS);
        } else if (scheduled.fixedRate() > 0) {
            long interval = scheduled.fixedRate();
            cycleId = Math.floorDiv(claimTime, interval);
            if (Objects.equals(claimedCycle, cycleId)) {
                return false;
            }
            markerTtlMillis = addSaturated(interval, MARKER_TTL_GRACE_MS);
        } else {
            Date nextFireTime = nextCronFireTime(scheduled, claimTime);
            cycleId = nextFireTime == null ? Long.MAX_VALUE : nextFireTime.getTime();
            if (Objects.equals(claimedCycle, cycleId)) {
                return false;
            }
            markerTtlMillis = nextFireTime == null
                    ? TimeUnit.DAYS.toMillis(366)
                    : addSaturated(Math.max(1L, nextFireTime.getTime() - claimTime),
                    MARKER_TTL_GRACE_MS);
        }
        marker.set(cycleId, Duration.ofMillis(markerTtlMillis));
        return true;
    }

    private static void completeFixedDelayExecution(
            RedissonClient client, String keyBase, Job job, String triggerType) {
        Scheduled scheduled = job.getScheduled();
        if (!SCHEDULED.equals(triggerType) || scheduled.fixedDelay() <= 0) {
            return;
        }
        String markerKey = keyBase + ":scheduled:marker";
        long completionTime = redisTimeMillis(client, markerKey);
        long markerTtlMillis = addSaturated(scheduled.fixedDelay(), MARKER_TTL_GRACE_MS);
        client.getBucket(markerKey).set(completionTime, Duration.ofMillis(markerTtlMillis));
    }

    private static long redisTimeMillis(RedissonClient client, String markerKey) {
        Number value = client.getScript().eval(
                RScript.Mode.READ_WRITE, REDIS_TIME_SCRIPT, RScript.ReturnType.INTEGER,
                List.of(markerKey));
        return value.longValue();
    }

    private static Date nextCronFireTime(Scheduled scheduled, long afterTime) {
        CronExpressionPlus cron = new CronExpressionPlus(CronUtils.get(scheduled.cron()));
        if (scheduled.zone() != null && !scheduled.zone().isBlank()) {
            cron.setTimeZone(TimeZone.getTimeZone(ZoneId.of(scheduled.zone())));
        }
        return cron.getNextValidTimeAfter(new Date(afterTime));
    }

    private static String manualRunId(Job job) {
        String runId = job.getContext().paramMap().get(MANUAL_RUN_ID);
        if (runId == null || runId.isBlank()) {
            runId = UUID.randomUUID().toString();
            job.getContext().paramMap().put(MANUAL_RUN_ID, runId);
        }
        return runId;
    }

    private static long addSaturated(long value, long increment) {
        return value > Long.MAX_VALUE - increment ? Long.MAX_VALUE : value + increment;
    }

    private ExecutionSuccess executeWithRetry(
            Job job, JobHandler handler, SysScheduledJobConfig config, String triggerType)
            throws Throwable {
        int maxAttempts = config.getMaxRetries() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.currentTimeMillis();
            Throwable businessFailure;
            long endedAt;
            try {
                handler.handle(job.getContext());
                businessFailure = null;
            } catch (Throwable failure) {
                businessFailure = failure;
            }
            endedAt = System.currentTimeMillis();
            if (businessFailure == null) {
                return new ExecutionSuccess(attempt, startedAt, endedAt);
            }

            boolean retry = attempt < maxAttempts
                    && !(businessFailure instanceof Error)
                    && !(businessFailure instanceof InterruptedException)
                    && !Thread.currentThread().isInterrupted();
            if (!retry) {
                recordBusinessFailure(job.getName(), FAILED, triggerType,
                        attempt, startedAt, endedAt, businessFailure);
                if (businessFailure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw businessFailure;
            }
            try {
                Thread.sleep(config.getRetryIntervalMs());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                businessFailure.addSuppressed(interrupted);
                recordBusinessFailure(job.getName(), FAILED, triggerType,
                        attempt, startedAt, endedAt, businessFailure);
                throw interrupted;
            }
            recordBusinessFailure(job.getName(), RETRY, triggerType,
                    attempt, startedAt, endedAt, businessFailure);
        }
        throw new IllegalStateException("定时任务重试流程异常结束");
    }

    private void recordSuccess(String jobName, String triggerType, ExecutionSuccess success) {
        try {
            record(jobName, SUCCESS, triggerType, success.attempt(),
                    success.startedAt(), success.endedAt(), null);
        } catch (RuntimeException logFailure) {
            log.error("定时任务已执行成功，但写入成功日志失败，jobName={}, attempt={}",
                    jobName, success.attempt(), logFailure);
        }
    }

    private void recordBusinessFailure(String jobName, String status, String triggerType,
                                       int attempt, long startedAt, long endedAt,
                                       Throwable businessFailure) {
        try {
            record(jobName, status, triggerType, attempt, startedAt, endedAt, businessFailure);
        } catch (RuntimeException logFailure) {
            businessFailure.addSuppressed(logFailure);
            log.error("写入定时任务失败日志失败，jobName={}, attempt={}",
                    jobName, attempt, logFailure);
        }
    }

    private void recordInfrastructureFailure(
            String jobName, String triggerType, long startedAt, Throwable failure) {
        recordInfrastructureFailure(jobName, triggerType, 1, startedAt, failure);
    }

    private void recordInfrastructureFailure(
            String jobName, String triggerType, int attempt, long startedAt, Throwable failure) {
        try {
            record(jobName, FAILED, triggerType, attempt, startedAt, failure);
        } catch (RuntimeException logFailure) {
            failure.addSuppressed(logFailure);
            log.error("写入定时任务基础设施失败日志失败，jobName={}", jobName, logFailure);
        }
    }

    private void record(String jobName, String status, String triggerType, int attempt,
                        long startedAt, Throwable failure) {
        record(jobName, status, triggerType, attempt, startedAt,
                System.currentTimeMillis(), failure);
    }

    private void record(String jobName, String status, String triggerType, int attempt,
                        long startedAt, long endedAt, Throwable failure) {
        SysScheduledJobLog logEntity = new SysScheduledJobLog()
                .setJobName(jobName)
                .setStatus(status)
                .setTriggerType(triggerType)
                .setAttempt(attempt)
                .setInstanceId(INSTANCE_ID)
                .setStartTime(new Date(startedAt))
                .setEndTime(new Date(endedAt))
                .setDurationMs(Math.max(0L, endedAt - startedAt))
                .setErrorSummary(errorSummary(failure));
        logEntity.setCreateDept(0L);
        logEntity.setCreateBy(0L);
        logEntity.setUpdateBy(0L);
        logMapper.save(logEntity);
    }

    private static String redisKey(String suffix) {
        String header = Solon.cfg() == null
                ? "jimuqu" : Solon.cfg().get("jimuqu.cache.keyHeader", "jimuqu");
        return header.endsWith(":") ? header + suffix : header + ":" + suffix;
    }

    private static String errorSummary(Throwable failure) {
        if (failure == null) {
            return null;
        }
        String message = failure.getMessage();
        String summary = failure.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return truncate(summary.replaceAll("[\\r\\n\\t]+", " "), ERROR_SUMMARY_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ExecutionSuccess(int attempt, long startedAt, long endedAt) {
    }
}
