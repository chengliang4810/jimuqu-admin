package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.domain.bo.ScheduledJobConfigBo;
import com.jimuqu.system.domain.query.ScheduledJobLogQuery;
import com.jimuqu.system.domain.vo.ScheduledJobLogVo;
import com.jimuqu.system.domain.vo.ScheduledJobVo;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.core.event.AppLoadEndEvent;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.scheduling.annotation.Scheduled;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Solon 运行时定时任务管理。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobService {

    private static final String CONTROL_TOPIC = "scheduled-job:control";

    private final IJobManager jobManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ScheduledJobConfigService configService;
    private final SysScheduledJobLogMapper logMapper;
    private volatile Integer topicListenerId;
    private volatile ScheduledFuture<?> reconcileFuture;
    private volatile boolean destroyed;

    @Init
    public void initialize() {
        Solon.context().onEvent(AppLoadEndEvent.class, ignored -> {
            reconcileLocalJobs();
            startPeriodicReconciliation();
        });
        Solon.context().getBeanAsync(RedissonCacheService.class, ignored -> subscribeControlTopic());
    }

    @Destroy
    public synchronized void destroy() {
        destroyed = true;
        if (topicListenerId != null) {
            RedisUtils.unsubscribe(CONTROL_TOPIC, topicListenerId);
            topicListenerId = null;
        }
        if (reconcileFuture != null) {
            reconcileFuture.cancel(false);
            reconcileFuture = null;
        }
    }

    public List<ScheduledJobVo> list() {
        return jobManager.jobGetAll().values().stream()
                .map(this::toVo)
                .sorted(Comparator.comparing(ScheduledJobVo::getJobName))
                .toList();
    }

    public void start(String jobName) {
        JobHolder job = requireJob(jobName);
        SysScheduledJobConfig config = configService.updateEnabled(
                jobName, true, job.getScheduled().enable());
        publishControl(config);
    }

    public void stop(String jobName) {
        JobHolder job = requireJob(jobName);
        SysScheduledJobConfig config = configService.updateEnabled(
                jobName, false, job.getScheduled().enable());
        publishControl(config);
    }

    public void updateConfig(String jobName, ScheduledJobConfigBo bo) {
        JobHolder job = requireJob(jobName);
        configService.updateRetry(jobName, job.getScheduled().enable(), bo);
    }

    public void run(String jobName) {
        JobHolder job = requireJob(jobName);
        executorService.execute(() -> {
            try {
                ContextEmpty context = new ContextEmpty();
                context.paramMap().put(ScheduledJobInterceptor.MANUAL_TRIGGER, "MANUAL");
                context.paramMap().put(ScheduledJobInterceptor.MANUAL_RUN_ID, UUID.randomUUID().toString());
                job.handle(context);
            } catch (Throwable e) {
                log.error("手动执行定时任务失败，jobName={}", jobName, e);
            }
        });
    }

    public Page<ScheduledJobLogVo> queryLogPage(ScheduledJobLogQuery query, PageQuery pageQuery) {
        QueryChain<SysScheduledJobLog> chain = pageQuery.applyOrder(
                QueryChain.of(logMapper).forSearch(true).where(query),
                queryChain -> queryChain.orderByDesc(SysScheduledJobLog::getLogId));
        return chain.returnType(ScheduledJobLogVo.class).paging(pageQuery.build());
    }

    public int deleteLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Assert.isFalse(ids.stream().anyMatch(java.util.Objects::isNull), "执行日志ID不能为空");
        List<Long> requested = ids.stream().distinct().toList();
        long existing = QueryChain.of(logMapper)
                .in(SysScheduledJobLog::getLogId, requested)
                .count();
        Assert.isTrue(existing == requested.size(), "执行日志不存在");
        return logMapper.deleteByIds(requested);
    }

    public int cleanLogs() {
        return logMapper.delete(where -> where.isNotNull(SysScheduledJobLog::getLogId));
    }

    private JobHolder requireJob(String jobName) {
        JobHolder job = jobManager.jobGet(jobName);
        if (job == null) {
            throw new ServiceException("定时任务不存在: " + jobName);
        }
        return job;
    }

    private ScheduledJobVo toVo(JobHolder job) {
        Scheduled scheduled = job.getScheduled();
        SysScheduledJobConfig config = configService.getOrCreate(job.getName(), scheduled.enable());
        return new ScheduledJobVo()
                .setJobName(job.getName())
                .setDescription(job.getSimpleName() == null ? job.getName() : job.getSimpleName())
                .setScheduleType(scheduleType(scheduled))
                .setScheduleExpression(scheduleExpression(scheduled))
                .setEnabled(Boolean.TRUE.equals(config.getEnabled()))
                .setMaxRetries(config.getMaxRetries())
                .setRetryIntervalMs(config.getRetryIntervalMs());
    }

    private void publishControl(SysScheduledJobConfig config) {
        String message = new ControlMessage(config.getControlVersion(), config.getJobName()).encode();
        applyControl(message);
        try {
            RedisUtils.publish(CONTROL_TOPIC, message);
        } catch (RuntimeException publishFailure) {
            log.error("发布定时任务启停消息失败，将由周期对账恢复，jobName={}, version={}",
                    config.getJobName(), config.getControlVersion(), publishFailure);
        }
    }

    private synchronized void subscribeControlTopic() {
        if (!destroyed && topicListenerId == null) {
            topicListenerId = RedisUtils.subscribe(
                    CONTROL_TOPIC, String.class, this::applyControlSafely);
        }
    }

    private void applyControlSafely(String message) {
        try {
            applyControl(message);
        } catch (RuntimeException failure) {
            log.error("应用定时任务启停消息失败，将由周期对账恢复，message={}", message, failure);
        }
    }

    private void applyControl(String message) {
        ControlMessage control = ControlMessage.parse(message);
        if (control == null) {
            return;
        }
        JobHolder job = jobManager.jobGet(control.jobName());
        if (job == null) {
            return;
        }
        SysScheduledJobConfig config = configService.getOrCreate(
                job.getName(), job.getScheduled().enable());
        long databaseVersion = config.getControlVersion() == null
                ? 0L : config.getControlVersion();
        if (databaseVersion < control.version()) {
            return;
        }
        applyLocalState(job, config);
    }

    private void reconcileLocalJobs() {
        jobManager.jobGetAll().values().forEach(job -> {
            SysScheduledJobConfig config = configService.getOrCreate(
                    job.getName(), job.getScheduled().enable());
            applyLocalState(job, config);
        });
    }

    private synchronized void applyLocalState(JobHolder job, SysScheduledJobConfig config) {
        if (Boolean.TRUE.equals(config.getEnabled())) {
            jobManager.jobStart(job.getName(), job.getData());
        } else {
            jobManager.jobStop(job.getName());
        }
    }

    private synchronized void startPeriodicReconciliation() {
        if (destroyed || reconcileFuture != null) {
            return;
        }
        long interval = Math.max(100L,
                Solon.cfg().getLong("jimuqu.scheduling.reconcileIntervalMs", 30_000L));
        reconcileFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::reconcileSafely, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void reconcileSafely() {
        if (destroyed) {
            return;
        }
        try {
            reconcileLocalJobs();
        } catch (RuntimeException failure) {
            log.error("定时任务状态周期对账失败", failure);
        }
    }

    private static String scheduleType(Scheduled scheduled) {
        if (scheduled.fixedDelay() > 0) {
            return "FIXED_DELAY";
        }
        if (scheduled.fixedRate() > 0) {
            return "FIXED_RATE";
        }
        return "CRON";
    }

    private static String scheduleExpression(Scheduled scheduled) {
        if (scheduled.fixedDelay() > 0) {
            return Long.toString(scheduled.fixedDelay());
        }
        if (scheduled.fixedRate() > 0) {
            return Long.toString(scheduled.fixedRate());
        }
        return scheduled.cron();
    }

    private record ControlMessage(long version, String jobName) {

        private String encode() {
            return version + ":" + jobName;
        }

        private static ControlMessage parse(String message) {
            if (message == null) {
                return null;
            }
            int separator = message.indexOf(':');
            if (separator <= 0 || separator == message.length() - 1) {
                return null;
            }
            try {
                long version = Long.parseLong(message.substring(0, separator));
                if (version < 0) {
                    return null;
                }
                return new ControlMessage(version, message.substring(separator + 1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
