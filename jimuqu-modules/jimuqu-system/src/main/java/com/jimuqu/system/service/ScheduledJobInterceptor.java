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
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.scheduling.ScheduledException;
import org.noear.solon.scheduling.annotation.Scheduled;
import org.noear.solon.scheduling.scheduled.Job;
import org.noear.solon.scheduling.scheduled.JobHandler;
import org.noear.solon.scheduling.scheduled.JobInterceptor;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 定时任务集群互斥、重试与执行日志。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobInterceptor implements JobInterceptor {

    /**
     * 手动触发类型上下文键。
     */
    public static final String MANUAL_TRIGGER = "jimuqu.scheduled.trigger";

    /**
     * 手动触发执行标识上下文键。
     */
    public static final String MANUAL_RUN_ID = "jimuqu.scheduled.runId";

    /**
     * 恢复执行的确定性错过周期上下文键。
     */
    public static final String RECOVERY_CYCLE = "jimuqu.scheduled.recoveryCycle";

    /**
     * 数据库动态任务来源上下文键。
     */
    public static final String DYNAMIC_SOURCE = "jimuqu.scheduled.dynamic";

    /**
     * 动态任务创建时的配置与定义代际上下文键。
     */
    static final String DYNAMIC_GENERATION =
            "jimuqu.scheduled.dynamicGeneration";

    /**
     * 正常调度触发类型。
     */
    private static final String SCHEDULED = "SCHEDULED";

    /**
     * 手动触发类型。
     */
    private static final String MANUAL = "MANUAL";

    /**
     * 故障恢复触发类型。
     */
    private static final String RECOVERY = "RECOVERY";

    /**
     * 执行成功状态。
     */
    private static final String SUCCESS = "SUCCESS";

    /**
     * 执行失败状态。
     */
    private static final String FAILED = "FAILED";

    /**
     * 等待重试状态。
     */
    private static final String RETRY = "RETRY";

    /**
     * 跳过执行状态。
     */
    private static final String SKIPPED = "SKIPPED";

    /**
     * 调度边界标记的过期宽限时间。
     */
    private static final long MARKER_TTL_GRACE_MS =
            TimeUnit.MINUTES.toMillis(1);

    /**
     * 默认执行认领租约时长。
     */
    private static final long DEFAULT_CLAIM_LEASE_MS =
            TimeUnit.SECONDS.toMillis(30);

    /**
     * 最小执行认领租约时长。
     */
    private static final long MIN_CLAIM_LEASE_MS = 100L;

    /**
     * 最大执行认领租约时长。
     */
    private static final long MAX_CLAIM_LEASE_MS =
            TimeUnit.DAYS.toMillis(1);

    /**
     * 调度定义代际状态保留时长。
     */
    private static final long GENERATION_STATE_TTL_MS =
            TimeUnit.DAYS.toMillis(366);

    /**
     * 跨实例锁最小重试间隔。
     */
    private static final long MIN_LOCK_RETRY_DELAY_MS = 100L;

    /**
     * 租约执行被其他跨实例锁阻塞时的最大重试间隔。
     */
    private static final long LOCK_RETRY_MAX_DELAY_MS =
            TimeUnit.SECONDS.toMillis(5);

    /**
     * 执行异常摘要最大长度。
     */
    private static final int ERROR_SUMMARY_LENGTH = 1000;

    /**
     * 获取 Redis 服务端时间的脚本。
     */
    private static final String REDIS_TIME_SCRIPT =
            "local t=redis.call('TIME'); return (t[1] * 1000) + math.floor(t[2] / 1000)";

    /**
     * 原子认领正常调度周期的脚本。
     */
    private static final String CLAIM_SCHEDULED_SCRIPT = """
            local last = redis.call('GET', KEYS[4])
            local now = tonumber(ARGV[1])
            local interval = tonumber(ARGV[2])
            local mode = ARGV[3]
            local cycle = ARGV[4]
            if mode == 'CRON' then
                if last and now < tonumber(last) then
                    return 0
                end
            elseif last and now - tonumber(last) < interval then
                return 0
            end
            local pending = 'PENDING|' .. ARGV[5] .. '|' .. ARGV[1] .. '|' .. ARGV[6]
            redis.call('ZADD', KEYS[1], ARGV[6], cycle)
            redis.call('HSET', KEYS[2], cycle, pending)
            redis.call('SET', KEYS[4], ARGV[9], 'PX', ARGV[7])
            redis.call('SET', KEYS[5], ARGV[9], 'PX', ARGV[7])
            redis.call('SET', KEYS[6], ARGV[9], 'PX', ARGV[7])
            redis.call('PEXPIRE', KEYS[1], ARGV[8])
            redis.call('PEXPIRE', KEYS[2], ARGV[8])
            redis.call('PEXPIRE', KEYS[3], ARGV[8])
            return 1
            """;

    /**
     * 查找租约已经过期的待执行周期脚本。
     */
    private static final String FIND_EXPIRED_SCRIPT = """
            local completed = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[1])
            for _, cycle in ipairs(completed) do
                redis.call('HDEL', KEYS[2], cycle)
                redis.call('ZREM', KEYS[3], cycle)
            end
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            redis.call('PEXPIRE', KEYS[2], ARGV[2])
            redis.call('PEXPIRE', KEYS[3], ARGV[2])
            return redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            """;

    /**
     * 原子接管租约已经过期的执行周期脚本。
     */
    private static final String RECLAIM_EXPIRED_SCRIPT = """
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not score or tonumber(score) > tonumber(ARGV[2]) then
                return 0
            end
            local state = redis.call('HGET', KEYS[2], ARGV[1])
            if not state or string.sub(state, 1, 8) ~= 'PENDING|' then
                return 0
            end
            local pending = 'PENDING|' .. ARGV[3] .. '|' .. ARGV[2] .. '|' .. ARGV[4]
            redis.call('HSET', KEYS[2], ARGV[1], pending)
            redis.call('ZADD', KEYS[1], ARGV[4], ARGV[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[5])
            redis.call('PEXPIRE', KEYS[2], ARGV[5])
            return 1
            """;

    /**
     * 原子认领错过周期恢复执行的脚本。
     */
    private static final String CLAIM_RECOVERY_SCRIPT = """
            local completedAt = redis.call('ZSCORE', KEYS[3], ARGV[1])
            if completedAt and tonumber(completedAt) <= tonumber(ARGV[2]) then
                redis.call('ZREM', KEYS[3], ARGV[1])
                redis.call('HDEL', KEYS[2], ARGV[1])
                completedAt = nil
            end
            if completedAt then
                return 0
            end
            local state = redis.call('HGET', KEYS[2], ARGV[1])
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if state and string.sub(state, 1, 8) == 'PENDING|'
                    and score and tonumber(score) > tonumber(ARGV[2]) then
                return 0
            end
            if state and string.sub(state, 1, 8) ~= 'PENDING|' then
                return 0
            end
            if not state then
                local recoveryWatermark = redis.call('GET', KEYS[6])
                if recoveryWatermark
                        and tonumber(recoveryWatermark) >= tonumber(ARGV[1]) then
                    return 0
                end
                local waterline = redis.call('GET', KEYS[5])
                if waterline and tonumber(waterline) >= tonumber(ARGV[1]) then
                    return 0
                end
            end
            local pending = 'PENDING|' .. ARGV[3] .. '|' .. ARGV[2] .. '|' .. ARGV[4]
            redis.call('HSET', KEYS[2], ARGV[1], pending)
            redis.call('ZADD', KEYS[1], ARGV[4], ARGV[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[5])
            redis.call('PEXPIRE', KEYS[2], ARGV[5])
            redis.call('PEXPIRE', KEYS[3], ARGV[5])
            redis.call('SET', KEYS[4], ARGV[2], 'PX', ARGV[6])
            redis.call('SET', KEYS[5], ARGV[2], 'PX', ARGV[6])
            if tonumber(ARGV[8]) > 0 then
                redis.call('SET', KEYS[6], ARGV[7], 'PX', ARGV[8])
            else
                redis.call('SET', KEYS[6], ARGV[7])
            end
            redis.call('SET', KEYS[7], ARGV[2], 'PX', ARGV[6])
            return 1
            """;

    /**
     * 原子完成已认领调度周期的脚本。
     */
    private static final String COMPLETE_SCHEDULED_SCRIPT = """
            local state = redis.call('HGET', KEYS[2], ARGV[1])
            local expected = 'PENDING|' .. ARGV[2] .. '|'
            if not state or string.sub(state, 1, string.len(expected)) ~= expected then
                return 0
            end
            redis.call('HSET', KEYS[2], ARGV[1],
                    'COMPLETED|' .. ARGV[2] .. '|' .. ARGV[3])
            redis.call('ZREM', KEYS[1], ARGV[1])
            redis.call('ZADD', KEYS[3], ARGV[4], ARGV[1])
            if ARGV[5] == 'FIXED_DELAY' then
                redis.call('SET', KEYS[4], ARGV[3], 'PX', ARGV[6])
                redis.call('SET', KEYS[5], ARGV[3], 'PX', ARGV[6])
                redis.call('SET', KEYS[6], ARGV[3], 'PX', ARGV[6])
            end
            redis.call('PEXPIRE', KEYS[1], ARGV[7])
            redis.call('PEXPIRE', KEYS[2], ARGV[7])
            redis.call('PEXPIRE', KEYS[3], ARGV[7])
            return 1
            """;

    /**
     * 原子续期正在执行周期租约的脚本。
     */
    private static final String RENEW_SCHEDULED_SCRIPT = """
            local state = redis.call('HGET', KEYS[2], ARGV[1])
            local expected = 'PENDING|' .. ARGV[2] .. '|'
            if not state or string.sub(state, 1, string.len(expected)) ~= expected then
                return 0
            end
            local t = redis.call('TIME')
            local now = (t[1] * 1000) + math.floor(t[2] / 1000)
            local leaseUntil = now + tonumber(ARGV[3])
            redis.call('HSET', KEYS[2], ARGV[1],
                    expected .. now .. '|' .. leaseUntil)
            redis.call('ZADD', KEYS[1], leaseUntil, ARGV[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            redis.call('PEXPIRE', KEYS[2], ARGV[4])
            return leaseUntil
            """;

    /**
     * 获取最近待执行周期剩余租约时间的脚本。
     */
    private static final String PENDING_RETRY_DELAY_SCRIPT = """
            local t = redis.call('TIME')
            local now = (t[1] * 1000) + math.floor(t[2] / 1000)
            local entries = redis.call('ZRANGE', KEYS[1], 0, -1, 'WITHSCORES')
            for index = 1, #entries, 2 do
                local cycle = entries[index]
                local score = tonumber(entries[index + 1])
                local state = redis.call('HGET', KEYS[2], cycle)
                if state and string.sub(state, 1, 8) == 'PENDING|' then
                    if score <= now then
                        return 1
                    end
                    return math.floor(score - now) + 1
                end
            end
            return 0
            """;

    /**
     * 当前应用实例标识。
     */
    private static final String INSTANCE_ID = truncate(
            ManagementFactory.getRuntimeMXBean().getName(), 128);

    /**
     * 定时任务持久化配置服务。
     */
    private final ScheduledJobConfigService configService;

    /**
     * 定时任务执行日志 Mapper。
     */
    private final SysScheduledJobLogMapper logMapper;

    /**
     * 租约续期与接管使用的调度执行器。
     */
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * 已登记的租约接管重试。
     *
     * <p>同一任务定义和触发类型最多保留一个待执行重试，避免高频任务在
     * 长租约或锁竞争期间向共享调度线程池无界写入延迟任务。</p>
     */
    private final Set<String> pendingLeaseRetries =
            ConcurrentHashMap.newKeySet();

    /**
     * 拦截 Solon 调度任务并应用集群认领、重试和日志语义。
     *
     * @param job 当前任务
     * @param handler 当前任务处理器
     * @throws Throwable 任务执行或基础设施异常
     */
    @Override
    public void doIntercept(Job job, JobHandler handler) throws Throwable {
        doIntercept(RedisUtils.getClient(), job, handler);
    }

    /**
     * 使用指定 Redis 客户端执行一次外部调度触发。
     *
     * @param client Redis 客户端
     * @param job 当前任务
     * @param handler 当前任务处理器
     * @throws Throwable 任务执行或基础设施异常
     */
    void doIntercept(RedissonClient client, Job job, JobHandler handler) throws Throwable {
        doIntercept(client, job, handler, false);
    }

    /**
     * 执行任务拦截逻辑，并区分外部触发与内部租约接管重试。
     *
     * @param client Redis 客户端
     * @param job 当前任务
     * @param handler 当前任务处理器
     * @param leaseRetry 是否为内部租约接管重试
     * @throws Throwable 任务执行或基础设施异常
     */
    private void doIntercept(
            RedissonClient client, Job job, JobHandler handler,
            boolean leaseRetry) throws Throwable {
        long startedAt = System.currentTimeMillis();
        String requestedTrigger = job.getContext().paramMap().get(MANUAL_TRIGGER);
        String triggerType = MANUAL.equals(requestedTrigger)
                ? MANUAL : RECOVERY.equals(requestedTrigger) ? RECOVERY : SCHEDULED;
        String executionId = executionId(job, triggerType);
        SysScheduledJobConfig config;
        try {
            boolean dynamic = "true".equals(
                    job.getContext().paramMap().get(DYNAMIC_SOURCE));
            config = dynamic
                    ? configService.requireDynamic(job.getName())
                    : configService.getOrCreate(
                            job.getName(), job.getScheduled().enable());
            if (dynamic && !matchesDynamicGeneration(job, config)) {
                log.warn(
                        "忽略已过期的动态定时任务回调，jobName={}, expectedGeneration={}, currentGeneration={}",
                        job.getName(),
                        job.getContext().paramMap().get(DYNAMIC_GENERATION),
                        dynamicGeneration(config));
                if (!leaseRetry) {
                    record(job.getName(), executionId, SKIPPED,
                            triggerType, 1, startedAt, null);
                }
                return;
            }
        } catch (Throwable e) {
            recordInfrastructureFailure(
                    job.getName(), executionId, triggerType, startedAt, e);
            throw e;
        }
        if (!MANUAL.equals(triggerType)
                && !Boolean.TRUE.equals(config.getEnabled())) {
            record(job.getName(), executionId, SKIPPED,
                    triggerType, 1, startedAt, null);
            return;
        }

        RLock lock = null;
        boolean executionStarted = false;
        Throwable interceptedFailure = null;
        try {
            String keyBase = redisKey("scheduled-job:{" + job.getName() + "}");
            boolean holdLockDuringExecution =
                    holdsLockDuringExecution(job, triggerType, config);
            lock = client.getLock(keyBase
                    + (holdLockDuringExecution ? ":execution-lock" : ":claim-lock"));
            boolean lockAcquired = lock.tryLock();
            if (!lockAcquired) {
                if (leaseRetrySupported(triggerType)) {
                    long pendingDelay = pendingRetryDelayMillis(
                            client, keyBase, job, config, triggerType);
                    scheduleLeaseRetry(
                            client, job, handler, triggerType,
                            lockRetryDelayMillis(pendingDelay));
                }
                if (MANUAL.equals(triggerType)) {
                    record(job.getName(), executionId, SKIPPED,
                            triggerType, 1, startedAt, null);
                }
                return;
            }
            List<ExecutionClaim> claims = claimExecutions(
                    client, keyBase, job, config, triggerType, executionId);
            if (!holdLockDuringExecution) {
                unlock(lock, null);
                lock = null;
            }
            if (claims.isEmpty()) {
                if (leaseRetrySupported(triggerType)) {
                    long retryDelay = pendingRetryDelayMillis(
                            client, keyBase, job, config, triggerType);
                    if (retryDelay > 0L) {
                        scheduleLeaseRetry(
                                client, job, handler,
                                triggerType, retryDelay);
                    }
                }
                if (MANUAL.equals(triggerType)) {
                    record(job.getName(), executionId, SKIPPED,
                            triggerType, 1, startedAt, null);
                }
                return;
            }
            executionStarted = true;
            Throwable firstFailure = executeClaims(
                    client, job, handler, config, claims);
            if (firstFailure != null) {
                throw firstFailure;
            }
        } catch (Throwable e) {
            interceptedFailure = e;
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (!executionStarted) {
                recordInfrastructureFailure(
                        job.getName(), executionId, triggerType, startedAt, e);
            }
            throw e;
        } finally {
            unlock(lock, interceptedFailure);
        }
    }

    /**
     * 在当前租约到期后重新竞争同一个执行周期。
     *
     * @param client Redis 客户端
     * @param job 当前任务
     * @param handler 当前任务处理器
     * @param triggerType 触发类型
     * @param retryDelayMs 重试延迟毫秒数
     */
    private void scheduleLeaseRetry(
            RedissonClient client, Job job, JobHandler handler,
            String triggerType, long retryDelayMs) {
        String retryKey = leaseRetryKey(job, triggerType);
        if (!pendingLeaseRetries.add(retryKey)) {
            return;
        }
        try {
            scheduledExecutorService.schedule(
                    () -> {
                        pendingLeaseRetries.remove(retryKey);
                        try {
                            doIntercept(client, job, handler, true);
                        } catch (Throwable failure) {
                            if (failure instanceof InterruptedException) {
                                Thread.currentThread().interrupt();
                            }
                            log.error(
                                    "定时任务租约到期重试失败，jobName={}, triggerType={}",
                                    job.getName(), triggerType,
                                    failure);
                        }
                    },
                    Math.max(1L, retryDelayMs),
                    TimeUnit.MILLISECONDS);
        } catch (RuntimeException schedulingFailure) {
            pendingLeaseRetries.remove(retryKey);
            throw schedulingFailure;
        }
    }

    /**
     * 构建租约接管重试去重键。
     *
     * @param job 当前任务
     * @param triggerType 触发类型
     * @return 任务名称、定义代际和触发类型组成的键
     */
    private static String leaseRetryKey(Job job, String triggerType) {
        String generation =
                job.getContext().paramMap().get(DYNAMIC_GENERATION);
        if (generation == null) {
            generation = "SYSTEM:"
                    + systemDefinitionId(job.getScheduled());
        }
        return job.getName() + '\u0000'
                + generation
                + '\u0000' + triggerType;
    }

    /**
     * 读取当前触发类型相关 PENDING 租约的最短剩余时间。
     *
     * @param client Redis 客户端
     * @param keyBase Redis 任务键前缀
     * @param job 当前任务
     * @param config 任务配置
     * @param triggerType 触发类型
     * @return 剩余毫秒数，不存在有效 PENDING 状态时返回零
     */
    private static long pendingRetryDelayMillis(
            RedissonClient client, String keyBase,
            Job job, SysScheduledJobConfig config, String triggerType) {
        ClaimKeys primaryKeys = claimKeys(
                keyBase, job, config, RECOVERY.equals(triggerType));
        long primaryDelay = pendingRetryDelayMillis(client, primaryKeys);
        if (!SCHEDULED.equals(triggerType)) {
            return primaryDelay;
        }
        long recoveryDelay = pendingRetryDelayMillis(
                client, claimKeys(keyBase, job, config, true));
        if (primaryDelay <= 0L) {
            return recoveryDelay;
        }
        if (recoveryDelay <= 0L) {
            return primaryDelay;
        }
        return Math.min(primaryDelay, recoveryDelay);
    }

    /**
     * 读取一组 PENDING 状态中最早到期租约的剩余时间。
     *
     * @param client Redis 客户端
     * @param keys Redis 认领键
     * @return 剩余毫秒数，不存在有效 PENDING 状态时返回零
     */
    private static long pendingRetryDelayMillis(
            RedissonClient client, ClaimKeys keys) {
        Number delay = client.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_ONLY,
                PENDING_RETRY_DELAY_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(keys.pending(), keys.states()));
        return Math.max(0L, delay.longValue());
    }

    /**
     * 判断当前触发是否使用可接管的周期租约。
     *
     * @param triggerType 触发类型
     * @return 正常调度或恢复执行返回 true
     */
    private static boolean leaseRetrySupported(String triggerType) {
        return SCHEDULED.equals(triggerType)
                || RECOVERY.equals(triggerType);
    }

    /**
     * 将锁竞争重试限制在 100 毫秒到 5 秒，避免 watchdog
     * 晚于业务租约释放时形成毫秒级忙循环。
     *
     * @param pendingDelayMs 当前 PENDING 租约剩余时间
     * @return 有界重试延迟
     */
    private static long lockRetryDelayMillis(long pendingDelayMs) {
        if (pendingDelayMs <= 1L) {
            return MIN_LOCK_RETRY_DELAY_MS;
        }
        return Math.max(
                MIN_LOCK_RETRY_DELAY_MS,
                Math.min(LOCK_RETRY_MAX_DELAY_MS, pendingDelayMs));
    }

    /**
     * 执行本次认领的全部周期，并保证每个周期独立完成租约。
     *
     * @param client Redis 客户端
     * @param job 当前任务
     * @param handler 任务处理器
     * @param config 任务配置
     * @param claims 已认领周期
     * @return 首个执行失败，无失败时返回空
     */
    private Throwable executeClaims(
            RedissonClient client, Job job, JobHandler handler,
            SysScheduledJobConfig config, List<ExecutionClaim> claims) {
        Throwable firstFailure = null;
        List<LeaseHeartbeat> heartbeats = new ArrayList<>();
        int processedClaims = 0;
        try {
            for (ExecutionClaim claim : claims) {
                heartbeats.add(startLeaseHeartbeat(client, claim));
            }
            for (int index = 0; index < claims.size(); index++) {
                ExecutionClaim claim = claims.get(index);
                LeaseHeartbeat heartbeat = heartbeats.get(index);
                ExecutionSuccess success = null;
                Throwable executionFailure = null;
                boolean completionSucceeded = true;
                try {
                    success = executeWithRetry(
                            job, handler, config, claim.executionId(),
                            claim.triggerType());
                } catch (Throwable failure) {
                    executionFailure = failure;
                    if (firstFailure == null) {
                        firstFailure = failure;
                    }
                } finally {
                    long completedAtNanos = System.nanoTime();
                    try {
                        completionSucceeded = completeScheduledExecution(
                                client, job, claim);
                        if (!completionSucceeded) {
                            IllegalStateException ownershipLost =
                                    new IllegalStateException(
                                            "定时任务执行完成时已失去周期所有权");
                            if (executionFailure == null) {
                                recordInfrastructureFailure(
                                        job.getName(), claim.executionId(),
                                        claim.triggerType(),
                                        success == null ? 1 : success.attempt(),
                                        success == null
                                                ? System.currentTimeMillis()
                                                : success.startedAt(),
                                        ownershipLost);
                                if (firstFailure == null) {
                                    firstFailure = ownershipLost;
                                }
                            } else {
                                executionFailure.addSuppressed(ownershipLost);
                                log.error(
                                        "定时任务业务执行失败后发现 owner token 已失效，jobName={}, executionId={}",
                                        job.getName(), claim.executionId(),
                                        ownershipLost);
                            }
                        }
                    } catch (RuntimeException completionFailure) {
                        completionSucceeded = false;
                        awaitRemainingFixedDelay(
                                job.getScheduled().fixedDelay(),
                                completedAtNanos);
                        if (executionFailure == null) {
                            recordInfrastructureFailure(
                                    job.getName(), claim.executionId(),
                                    claim.triggerType(),
                                    success == null ? 1 : success.attempt(),
                                    success == null
                                            ? System.currentTimeMillis()
                                            : success.startedAt(),
                                    completionFailure);
                            if (firstFailure == null) {
                                firstFailure = completionFailure;
                            }
                        } else {
                            executionFailure.addSuppressed(completionFailure);
                            log.error(
                                    "定时任务执行失败，且完成集群认领失败，jobName={}, executionId={}",
                                    job.getName(), claim.executionId(),
                                    completionFailure);
                        }
                    }
                    heartbeat.stop();
                }
                processedClaims++;
                if (success != null && completionSucceeded) {
                    recordSuccess(
                            job.getName(), claim.executionId(),
                            claim.triggerType(), success);
                }
                if (executionFailure instanceof Error
                        || executionFailure instanceof InterruptedException) {
                    break;
                }
            }
        } finally {
            for (int index = processedClaims;
                 index < heartbeats.size(); index++) {
                heartbeats.get(index).stop();
            }
        }
        return firstFailure;
    }

    /**
     * 为一个已认领周期启动 token 条件的租约心跳。
     *
     * @param client Redis 客户端
     * @param claim 周期认领
     * @return 可停止的租约心跳
     */
    private LeaseHeartbeat startLeaseHeartbeat(
            RedissonClient client, ExecutionClaim claim) {
        if (claim.cycleId() == null || claim.token() == null) {
            return LeaseHeartbeat.NONE;
        }
        long leaseMs = claimLeaseMs();
        long heartbeatMs = Math.max(1L, leaseMs / 3L);
        AtomicReference<ScheduledFuture<?>> futureReference =
                new AtomicReference<>();
        ScheduledFuture<?> future =
                scheduledExecutorService.scheduleWithFixedDelay(
                        () -> {
                            try {
                                boolean renewed = renewScheduledExecution(
                                        client, claim, leaseMs);
                                if (!renewed) {
                                    log.warn(
                                            "定时任务租约心跳发现 owner token 已失效，executionId={}",
                                            claim.executionId());
                                    ScheduledFuture<?> currentFuture =
                                            futureReference.get();
                                    if (currentFuture != null) {
                                        currentFuture.cancel(false);
                                    }
                                }
                            } catch (RuntimeException renewalFailure) {
                                log.error(
                                        "续租定时任务周期失败，executionId={}",
                                        claim.executionId(),
                                        renewalFailure);
                            }
                        },
                        heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
        futureReference.set(future);
        return new LeaseHeartbeat(future);
    }

    /**
     * 使用 Lua 原子校验 owner token 并延长 PENDING 租约。
     *
     * @param client Redis 客户端
     * @param claim 周期认领
     * @param leaseMs 新租约时长
     * @return token 仍有效并续租成功时返回 true
     */
    private static boolean renewScheduledExecution(
            RedissonClient client, ExecutionClaim claim, long leaseMs) {
        Number renewed = client.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                RENEW_SCHEDULED_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(claim.keys().pending(), claim.keys().states()),
                String.valueOf(claim.cycleId()), claim.token(),
                String.valueOf(leaseMs),
                String.valueOf(GENERATION_STATE_TTL_MS));
        return renewed.longValue() > 0L;
    }

    /**
     * 判断是否必须持有跨实例执行锁直到任务结束。
     *
     * @param job 当前任务
     * @param triggerType 触发类型
     * @param config 任务配置
     * @return 是否全程持有锁
     */
    private static boolean holdsLockDuringExecution(
            Job job, String triggerType, SysScheduledJobConfig config) {
        return (SCHEDULED.equals(triggerType)
                && job.getScheduled().fixedDelay() > 0)
                || (config.getMaxRetries() != null
                && config.getMaxRetries() > 0)
                || ScheduledJobConfigService.CONCURRENT_FORBID.equals(
                        config.getConcurrentPolicy());
    }

    /**
     * 释放当前线程持有的跨实例执行锁。
     *
     * @param lock 跨实例执行锁
     * @param primaryFailure 业务链路已有异常
     * @throws Throwable 没有主异常时向上抛出解锁异常
     */
    private static void unlock(
            RLock lock, Throwable primaryFailure) throws Throwable {
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

    /**
     * 在释放固定延迟任务锁前等待剩余延迟窗口。
     *
     * @param fixedDelayMs 固定延迟毫秒数
     * @param completedAtNanos 业务完成时的单调时钟值
     */
    private static void awaitRemainingFixedDelay(
            long fixedDelayMs, long completedAtNanos) {
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

    /**
     * 认领手工、恢复或正常调度执行。
     *
     * @param client Redis 客户端
     * @param keyBase Redis 任务键前缀
     * @param job 当前任务
     * @param config 任务配置
     * @param triggerType 触发类型
     * @param executionId 当前执行链标识
     * @return 已认领执行
     */
    private static List<ExecutionClaim> claimExecutions(
            RedissonClient client, String keyBase, Job job,
            SysScheduledJobConfig config, String triggerType,
            String executionId) {
        if (MANUAL.equals(triggerType)) {
            String runId = manualRunId(job);
            String markerKey = keyBase + ":manual:" + runId + ":marker";
            long claimTime = redisTimeMillis(client, markerKey);
            RBucket<Long> marker = client.getBucket(markerKey);
            return marker.setIfAbsent(claimTime, Duration.ofDays(1))
                    ? List.of(new ExecutionClaim(
                    executionId, null, null, MANUAL,
                    null))
                    : List.of();
        }
        if (RECOVERY.equals(triggerType)) {
            return claimRecoveryExecution(
                    client, keyBase, job, config, executionId);
        }
        return claimScheduledExecutions(client, keyBase, job, config);
    }

    /**
     * 使用 PENDING/COMPLETED 状态和周期 watchdog 锁认领恢复执行。
     *
     * @param client Redis 客户端
     * @param keyBase Redis 任务键前缀
     * @param job 当前任务
     * @param config 任务配置
     * @param executionId 恢复执行链标识
     * @return 已认领恢复执行
     */
    private static List<ExecutionClaim> claimRecoveryExecution(
            RedissonClient client, String keyBase, Job job,
            SysScheduledJobConfig config, String executionId) {
        long cycleId = recoveryCycle(job);
        ClaimKeys keys = claimKeys(keyBase, job, config, true);
        ClaimKeys scheduledKeys = claimKeys(keyBase, job, config, false);
        long claimTime = redisTimeMillis(client, keys.waterline());
        long leaseUntil = addSaturated(claimTime, claimLeaseMs());
        long anchorTtlMillis =
                scheduleAnchorTtlMillis(job.getScheduled(), claimTime);
        RecoveryWatermark recoveryWatermark = recoveryWatermark(
                job.getScheduled(), cycleId, claimTime);
        long configId =
                config.getConfigId() == null ? 0L : config.getConfigId();
        String recoveryWatermarkKey = keyBase
                + ":recovery-watermark:" + configId
                + ":" + effectiveDefinitionId(job, config);
        String token = UUID.randomUUID().toString();
        Number result = client.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                CLAIM_RECOVERY_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(
                        keys.pending(), keys.states(),
                        keys.completed(), scheduledKeys.anchor(),
                        scheduledKeys.waterline(), recoveryWatermarkKey,
                        scheduledKeys.legacyWaterline()),
                String.valueOf(cycleId), String.valueOf(claimTime),
                token, String.valueOf(leaseUntil),
                String.valueOf(GENERATION_STATE_TTL_MS),
                String.valueOf(anchorTtlMillis),
                String.valueOf(recoveryWatermark.coverageThrough()),
                String.valueOf(recoveryWatermark.ttlMillis()));
        if (result.longValue() <= 0L) {
            return List.of();
        }
        return List.of(new ExecutionClaim(
                executionId, cycleId, token, RECOVERY, keys));
    }

    /**
     * 接管全部过期周期，并认领当前应执行周期。
     *
     * @param client Redis 客户端
     * @param keyBase Redis 任务键前缀
     * @param job 当前任务
     * @param config 任务配置
     * @return 已认领执行
     */
    private static List<ExecutionClaim> claimScheduledExecutions(
            RedissonClient client, String keyBase, Job job,
            SysScheduledJobConfig config) {
        Scheduled scheduled = job.getScheduled();
        ClaimKeys keys = claimKeys(keyBase, job, config, false);
        ClaimKeys recoveryKeys = claimKeys(keyBase, job, config, true);
        long claimTime = redisTimeMillis(client, keys.waterline());
        long leaseMs = claimLeaseMs();
        long leaseUntil = addSaturated(claimTime, leaseMs);
        String reclaimTokenPrefix = UUID.randomUUID().toString();
        List<ExecutionClaim> claims = new ArrayList<>();
        try {
            claims.addAll(reclaimExpiredClaims(
                    client, recoveryKeys, claimTime, leaseUntil,
                    reclaimTokenPrefix + ":recovery", RECOVERY));
            claims.addAll(reclaimExpiredClaims(
                    client, keys, claimTime, leaseUntil,
                    reclaimTokenPrefix + ":scheduled", SCHEDULED));
            if (scheduled.fixedDelay() > 0 && !claims.isEmpty()) {
                return claims;
            }

            String mode;
            long interval;
            long cycleId;
            long anchorValue;
            long anchorTtlMillis;
            if (scheduled.fixedDelay() > 0) {
                mode = "FIXED_DELAY";
                interval = scheduled.fixedDelay();
                cycleId = claimTime;
                anchorValue = cycleId;
                anchorTtlMillis =
                        addSaturated(interval, MARKER_TTL_GRACE_MS);
            } else if (scheduled.fixedRate() > 0) {
                mode = "FIXED_RATE";
                interval = scheduled.fixedRate();
                cycleId = claimTime;
                anchorValue = cycleId;
                anchorTtlMillis =
                        addSaturated(interval, MARKER_TTL_GRACE_MS);
            } else {
                mode = "CRON";
                interval = 0L;
                CronExpressionPlus cron = cronExpression(scheduled);
                Date currentSecond = new Date(
                        Math.floorDiv(claimTime, 1_000L) * 1_000L);
                Date nextFireTime = cron.getNextValidTimeAfter(
                        new Date(claimTime));
                anchorValue = nextFireTime == null
                        ? Long.MAX_VALUE : nextFireTime.getTime();
                if (cron.isSatisfiedBy(currentSecond)) {
                    cycleId = currentSecond.getTime();
                } else {
                    Date previousFireTime = previousCronFireTime(
                            cron, claimTime);
                    if (previousFireTime == null) {
                        throw new IllegalStateException(
                                "Cron 回调早于首个合法调度边界");
                    }
                    cycleId = previousFireTime.getTime();
                }
                anchorTtlMillis = nextFireTime == null
                        ? TimeUnit.DAYS.toMillis(366)
                        : addSaturated(
                                Math.max(
                                        1L,
                                        nextFireTime.getTime() - claimTime),
                                MARKER_TTL_GRACE_MS);
            }
            String token = UUID.randomUUID().toString();
            Number result = client.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    CLAIM_SCHEDULED_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    List.of(
                            keys.pending(), keys.states(),
                            keys.completed(), keys.anchor(),
                            keys.waterline(), keys.legacyWaterline()),
                    String.valueOf(claimTime),
                    String.valueOf(interval), mode,
                    String.valueOf(cycleId), token,
                    String.valueOf(leaseUntil),
                    String.valueOf(anchorTtlMillis),
                    String.valueOf(GENERATION_STATE_TTL_MS),
                    String.valueOf(anchorValue));
            if (result.longValue() > 0L) {
                claims.add(new ExecutionClaim(
                        scheduledExecutionId(cycleId), cycleId,
                        token, SCHEDULED, keys));
            }
            return claims;
        } catch (RuntimeException claimFailure) {
            throw claimFailure;
        }
    }

    /**
     * 原子接管全部已经到期的 PENDING 周期。
     *
     * @param client Redis 客户端
     * @param keys Redis 认领键
     * @param claimTime 当前 Redis 时间
     * @param leaseUntil 新租约到期时间
     * @param tokenPrefix 新 owner token 前缀
     * @param triggerType 周期对应触发类型
     * @return 已接管执行
     */
    private static List<ExecutionClaim> reclaimExpiredClaims(
            RedissonClient client, ClaimKeys keys, long claimTime,
            long leaseUntil, String tokenPrefix, String triggerType) {
        Object result = client.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                FIND_EXPIRED_SCRIPT,
                RScript.ReturnType.MULTI,
                List.of(keys.pending(), keys.states(), keys.completed()),
                String.valueOf(claimTime),
                String.valueOf(GENERATION_STATE_TTL_MS));
        List<ExecutionClaim> claims = new ArrayList<>();
        if (!(result instanceof List<?> entries)) {
            return claims;
        }
        for (Object entry : entries) {
            long cycleId = Long.parseLong(String.valueOf(entry));
            String token = tokenPrefix + ":" + cycleId;
            Number reclaimed = client.getScript(
                    StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    RECLAIM_EXPIRED_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    List.of(keys.pending(), keys.states()),
                    String.valueOf(cycleId),
                    String.valueOf(claimTime), token,
                    String.valueOf(leaseUntil),
                    String.valueOf(GENERATION_STATE_TTL_MS));
            if (reclaimed.longValue() > 0L) {
                String executionId = RECOVERY.equals(triggerType)
                        ? "recovery:" + cycleId
                        : scheduledExecutionId(cycleId);
                claims.add(new ExecutionClaim(
                        executionId, cycleId, token,
                        triggerType, keys));
            }
        }
        return claims;
    }

    /**
     * 将当前 owner 的 PENDING 周期原子完成。
     *
     * @param client Redis 客户端
     * @param job 当前任务
     * @param claim 当前认领
     * @return owner token 仍有效并成功完成时返回 true
     */
    private static boolean completeScheduledExecution(
            RedissonClient client, Job job, ExecutionClaim claim) {
        if (claim.cycleId() == null || claim.token() == null) {
            return true;
        }
        Scheduled scheduled = job.getScheduled();
        ClaimKeys keys = claim.keys();
        long completionTime =
                redisTimeMillis(client, keys.waterline());
        long anchorTtlMillis = SCHEDULED.equals(claim.triggerType())
                && scheduled.fixedDelay() > 0
                ? addSaturated(
                        scheduled.fixedDelay(), MARKER_TTL_GRACE_MS)
                : SCHEDULED.equals(claim.triggerType())
                && scheduled.fixedRate() > 0
                ? addSaturated(
                        scheduled.fixedRate(), MARKER_TTL_GRACE_MS)
                : TimeUnit.DAYS.toMillis(366);
        long stateExpiresAt = addSaturated(
                completionTime,
                Math.max(
                        anchorTtlMillis,
                        addSaturated(claimLeaseMs(),
                                MARKER_TTL_GRACE_MS)));
        String mode = !SCHEDULED.equals(claim.triggerType())
                ? RECOVERY : scheduled.fixedDelay() > 0
                ? "FIXED_DELAY" : scheduled.fixedRate() > 0
                ? "FIXED_RATE" : "CRON";
        Number completed = client.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                COMPLETE_SCHEDULED_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(
                        keys.pending(), keys.states(), keys.completed(),
                        keys.anchor(), keys.waterline(),
                        keys.legacyWaterline()),
                String.valueOf(claim.cycleId()), claim.token(),
                String.valueOf(completionTime),
                String.valueOf(stateExpiresAt), mode,
                String.valueOf(anchorTtlMillis),
                String.valueOf(GENERATION_STATE_TTL_MS));
        return completed.longValue() > 0L;
    }

    /**
     * 获取 Redis 服务端毫秒时间，避免节点时钟漂移影响调度认领。
     *
     * @param client Redis 客户端
     * @param markerKey 脚本执行使用的槽位键
     * @return Redis 服务端毫秒时间
     */
    private static long redisTimeMillis(
            RedissonClient client, String markerKey) {
        Number value = client.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, REDIS_TIME_SCRIPT, RScript.ReturnType.INTEGER,
                List.of(markerKey));
        return value.longValue();
    }

    /**
     * 构建同一配置代际的 Redis 认领键。
     *
     * @param keyBase Redis 任务键前缀
     * @param job 当前任务
     * @param config 任务配置
     * @param recovery 是否构建恢复执行状态键
     * @return Redis 认领键
     */
    private static ClaimKeys claimKeys(
            String keyBase, Job job, SysScheduledJobConfig config,
            boolean recovery) {
        long configId =
                config.getConfigId() == null ? 0L : config.getConfigId();
        String generationBase =
                keyBase + ":generation:" + configId
                        + ":" + effectiveDefinitionId(job, config)
                        + (recovery ? ":recovery" : "");
        return new ClaimKeys(
                generationBase + ":pending",
                generationBase + ":states",
                generationBase + ":completed",
                generationBase + ":anchor",
                generationBase + ":waterline",
                keyBase + ":scheduled:marker");
    }

    /**
     * 固定动态 Job 首次执行时的配置与定义代际，并拒绝旧回调借用新代际。
     *
     * @param job 当前任务
     * @param config 当前持久化配置
     * @return 当前回调是否仍属于持久化定义
     */
    private static boolean matchesDynamicGeneration(
            Job job, SysScheduledJobConfig config) {
        String currentGeneration = dynamicGeneration(config);
        String expectedGeneration =
                job.getContext().paramMap().get(DYNAMIC_GENERATION);
        return expectedGeneration != null
                && expectedGeneration.equals(currentGeneration);
    }

    /**
     * 组合配置主键与可执行定义指纹。
     *
     * @param config 任务配置
     * @return 动态任务创建代际
     */
    static String dynamicGeneration(SysScheduledJobConfig config) {
        long configId =
                config.getConfigId() == null ? 0L : config.getConfigId();
        return configId + ":" + definitionId(config);
    }

    /**
     * 计算只随可执行调度定义变化的稳定代际指纹。
     *
     * @param config 任务配置
     * @return SHA-256 前 128 位十六进制指纹
     */
    static String definitionId(SysScheduledJobConfig config) {
        return definitionId(
                config.getHandlerKey(),
                config.getScheduleType(),
                config.getScheduleExpression(),
                config.getZone(),
                config.getInitialDelayMs() == null
                        ? 0L : config.getInitialDelayMs());
    }

    /**
     * 计算当前任务实际执行定义的代际指纹。
     *
     * <p>动态任务以数据库定义为准；代码任务必须使用当前节点的
     * {@link Scheduled} 定义，避免滚动升级期间新旧节点互相借用代际。</p>
     *
     * @param job 当前任务
     * @param config 当前持久化运行配置
     * @return 当前任务实际执行定义指纹
     */
    private static String effectiveDefinitionId(
            Job job, SysScheduledJobConfig config) {
        if (ScheduledJobConfigService.SOURCE_DYNAMIC.equals(
                config.getJobSource())) {
            return definitionId(config);
        }
        return systemDefinitionId(job.getScheduled());
    }

    /**
     * 计算代码任务运行时调度定义的代际指纹。
     *
     * @param scheduled 当前节点的 Solon 调度定义
     * @return 代码任务定义指纹
     */
    private static String systemDefinitionId(Scheduled scheduled) {
        String scheduleType;
        String scheduleExpression;
        if (scheduled.fixedDelay() > 0) {
            scheduleType = "FIXED_DELAY";
            scheduleExpression = Long.toString(scheduled.fixedDelay());
        } else if (scheduled.fixedRate() > 0) {
            scheduleType = "FIXED_RATE";
            scheduleExpression = Long.toString(scheduled.fixedRate());
        } else {
            scheduleType = "CRON";
            scheduleExpression = scheduled.cron();
        }
        return definitionId(
                "SYSTEM",
                scheduleType,
                scheduleExpression,
                scheduled.zone(),
                scheduled.initialDelay());
    }

    /**
     * 计算一组规范化调度字段的稳定代际指纹。
     *
     * @param handlerKey 处理器标识
     * @param scheduleType 调度类型
     * @param scheduleExpression 调度表达式
     * @param zone Cron 时区
     * @param initialDelayMs 首次执行延迟毫秒数
     * @return SHA-256 前 128 位十六进制指纹
     */
    private static String definitionId(
            String handlerKey, String scheduleType,
            String scheduleExpression, String zone,
            long initialDelayMs) {
        StringBuilder canonical = new StringBuilder();
        appendDefinitionPart(canonical, handlerKey);
        appendDefinitionPart(canonical, scheduleType);
        appendDefinitionPart(canonical, scheduleExpression);
        appendDefinitionPart(canonical, zone);
        appendDefinitionPart(
                canonical,
                Long.toString(initialDelayMs));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException unavailableAlgorithm) {
            throw new IllegalStateException(
                    "当前 JDK 不支持 SHA-256", unavailableAlgorithm);
        }
    }

    /**
     * 用长度前缀写入一个指纹字段，避免字段分隔歧义。
     *
     * @param canonical 指纹原文
     * @param value 字段值
     */
    private static void appendDefinitionPart(
            StringBuilder canonical, String value) {
        String normalized = value == null ? "" : value;
        canonical.append(normalized.length())
                .append(':')
                .append(normalized);
    }

    /**
     * 获取 PENDING 认领租约时长。
     *
     * @return 租约时长毫秒数
     */
    private static long claimLeaseMs() {
        long configured = Solon.cfg() == null
                ? DEFAULT_CLAIM_LEASE_MS
                : Solon.cfg().getLong(
                        "jimuqu.scheduling.claimLeaseMs",
                        DEFAULT_CLAIM_LEASE_MS);
        return Math.max(
                MIN_CLAIM_LEASE_MS,
                Math.min(MAX_CLAIM_LEASE_MS, configured));
    }

    /**
     * 生成稳定的调度周期执行链标识。
     *
     * @param cycleId 调度周期
     * @return 执行链标识
     */
    private static String scheduledExecutionId(long cycleId) {
        return "scheduled:" + cycleId;
    }

    /**
     * 计算调度 anchor 和兼容 waterline 的存活时间。
     *
     * @param scheduled 调度定义
     * @param claimTime 当前 Redis 时间
     * @return anchor 存活时间毫秒数
     */
    private static long scheduleAnchorTtlMillis(
            Scheduled scheduled, long claimTime) {
        if (scheduled.fixedDelay() > 0) {
            return addSaturated(
                    scheduled.fixedDelay(), MARKER_TTL_GRACE_MS);
        }
        if (scheduled.fixedRate() > 0) {
            return addSaturated(
                    scheduled.fixedRate(), MARKER_TTL_GRACE_MS);
        }
        Date nextFireTime = nextCronFireTime(scheduled, claimTime);
        return nextFireTime == null
                ? TimeUnit.DAYS.toMillis(366)
                : addSaturated(
                        Math.max(1L, nextFireTime.getTime() - claimTime),
                        MARKER_TTL_GRACE_MS);
    }

    /**
     * 计算恢复水位及其至少存活到下一调度边界后的时间。
     *
     * @param scheduled 调度定义
     * @param missedCycle 本次补偿周期
     * @param claimTime 当前 Redis 时间
     * @return 恢复水位；没有未来边界的有限 Cron 使用永久终态
     */
    private static RecoveryWatermark recoveryWatermark(
            Scheduled scheduled, long missedCycle, long claimTime) {
        long nextBoundary;
        if (scheduled.cron() != null && !scheduled.cron().isBlank()) {
            Date nextFireTime = nextCronFireTime(scheduled, claimTime);
            if (nextFireTime == null) {
                return new RecoveryWatermark(
                        Math.max(missedCycle, claimTime), 0L);
            }
            nextBoundary = nextFireTime.getTime();
        } else {
            long interval = scheduled.fixedDelay() > 0
                    ? scheduled.fixedDelay() : scheduled.fixedRate();
            nextBoundary = addSaturated(claimTime, interval);
        }
        long coverageThrough = Math.max(missedCycle, claimTime);
        long expiresAfter = addSaturated(
                nextBoundary, MARKER_TTL_GRACE_MS);
        long coverageDuration = expiresAfter <= claimTime
                ? 1L : expiresAfter - claimTime;
        return new RecoveryWatermark(
                coverageThrough,
                Math.max(GENERATION_STATE_TTL_MS, coverageDuration));
    }

    /**
     * 获取当前 Cron 周期结束时的下一合法触发边界。
     *
     * @param scheduled 调度定义
     * @param afterTime 当前 Redis 时间
     * @return 下一合法触发边界
     */
    private static Date nextCronFireTime(Scheduled scheduled, long afterTime) {
        CronExpressionPlus cron = cronExpression(scheduled);
        return cron.getNextValidTimeAfter(new Date(afterTime));
    }

    /**
     * 获取当前时间之前最近的 Cron 合法触发边界。
     *
     * <p>java-cron 1.0.3 的 {@code getTimeBefore} 尚未实现。这里先指数扩大
     * 查询窗口，再利用“窗口起点的下一次触发是否早于当前时间”这一单调条件
     * 二分定位，避免对跨年 Cron 逐秒扫描。</p>
     *
     * @param cron Cron 表达式
     * @param beforeTime 当前回调时间毫秒数
     * @return 最近一次合法触发边界，不存在时返回 null
     */
    private static Date previousCronFireTime(
            CronExpressionPlus cron, long beforeTime) {
        if (beforeTime <= 0L) {
            return null;
        }
        long lookback = Math.min(1_000L, beforeTime);
        long lowerBound;
        Date candidate;
        while (true) {
            lowerBound = beforeTime - lookback;
            candidate = cron.getNextValidTimeAfter(
                    new Date(lowerBound));
            if (candidate != null
                    && candidate.getTime() <= beforeTime) {
                break;
            }
            if (lowerBound == 0L) {
                return null;
            }
            lookback = lookback > beforeTime / 2L
                    ? beforeTime : lookback * 2L;
        }

        long upperBound = beforeTime;
        while (lowerBound + 1L < upperBound) {
            long midpoint = lowerBound
                    + (upperBound - lowerBound) / 2L;
            Date next = cron.getNextValidTimeAfter(
                    new Date(midpoint));
            if (next != null && next.getTime() <= beforeTime) {
                lowerBound = midpoint;
                candidate = next;
            } else {
                upperBound = midpoint;
            }
        }
        return candidate;
    }

    /**
     * 按配置时区创建 Cron 表达式。
     *
     * @param scheduled 调度定义
     * @return Cron 表达式
     */
    private static CronExpressionPlus cronExpression(
            Scheduled scheduled) {
        CronExpressionPlus cron =
                new CronExpressionPlus(CronUtils.get(scheduled.cron()));
        if (scheduled.zone() != null && !scheduled.zone().isBlank()) {
            cron.setTimeZone(TimeZone.getTimeZone(ZoneId.of(scheduled.zone())));
        }
        return cron;
    }

    /**
     * 获取或生成一次手动触发共享的执行标识。
     *
     * @param job 当前任务
     * @return 手动执行标识
     */
    private static String manualRunId(Job job) {
        String runId = job.getContext().paramMap().get(MANUAL_RUN_ID);
        if (runId == null || runId.isBlank()) {
            runId = UUID.randomUUID().toString();
            job.getContext().paramMap().put(MANUAL_RUN_ID, runId);
        }
        return runId;
    }

    /**
     * 生成一次触发及其重试共享的执行链标识。
     *
     * @param job 当前任务
     * @param triggerType 触发类型
     * @return 执行链标识
     */
    private static String executionId(Job job, String triggerType) {
        if (MANUAL.equals(triggerType)) {
            return manualRunId(job);
        }
        if (RECOVERY.equals(triggerType)) {
            return "recovery:" + recoveryCycle(job);
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 获取恢复执行对应的确定性错过周期。
     *
     * @param job 当前任务
     * @return 错过周期时间
     */
    private static long recoveryCycle(Job job) {
        String cycle = job.getContext().paramMap().get(RECOVERY_CYCLE);
        if (cycle == null || cycle.isBlank()) {
            throw new IllegalArgumentException("恢复执行缺少错过周期");
        }
        try {
            long parsed = Long.parseLong(cycle);
            if (parsed < 0) {
                throw new IllegalArgumentException("恢复执行错过周期不能为负数");
            }
            return parsed;
        } catch (NumberFormatException invalidCycle) {
            throw new IllegalArgumentException(
                    "恢复执行错过周期格式错误", invalidCycle);
        }
    }

    /**
     * 饱和相加，避免时间计算溢出。
     *
     * @param value 当前值
     * @param increment 增量
     * @return 相加结果，溢出时返回长整数最大值
     */
    private static long addSaturated(long value, long increment) {
        return value > Long.MAX_VALUE - increment ? Long.MAX_VALUE : value + increment;
    }

    /**
     * 在相同执行链内按配置执行并重试业务处理器。
     *
     * @param job 当前任务
     * @param handler 任务处理器
     * @param config 任务配置
     * @param executionId 执行链标识
     * @param triggerType 触发类型
     * @return 成功执行信息
     * @throws Throwable 最终业务异常或不可重试异常
     */
    private ExecutionSuccess executeWithRetry(
            Job job, JobHandler handler, SysScheduledJobConfig config,
            String executionId, String triggerType)
            throws Throwable {
        int maxAttempts = config.getMaxRetries() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.currentTimeMillis();
            Throwable businessFailure;
            long endedAt;
            try (ScheduledJobExecutionContext.Scope ignored =
                         ScheduledJobExecutionContext.open(
                                 job.getName(), executionId,
                                 triggerType, attempt)) {
                handler.handle(job.getContext());
                businessFailure = null;
            } catch (Throwable failure) {
                businessFailure = unwrapHandlerFailure(failure);
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
                recordBusinessFailure(
                        job.getName(), executionId, FAILED, triggerType,
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
                recordBusinessFailure(
                        job.getName(), executionId, FAILED, triggerType,
                        attempt, startedAt, endedAt, businessFailure);
                throw interrupted;
            }
            recordBusinessFailure(
                    job.getName(), executionId, RETRY, triggerType,
                    attempt, startedAt, endedAt, businessFailure);
        }
        throw new IllegalStateException("定时任务重试流程异常结束");
    }

    /**
     * 解开 Solon 方法任务代理对业务异常的包装。
     *
     * @param failure 代理抛出的异常
     * @return 原始业务异常
     */
    private static Throwable unwrapHandlerFailure(Throwable failure) {
        Throwable unwrapped = Utils.throwableUnwrap(failure);
        while (unwrapped instanceof ScheduledException
                && unwrapped.getCause() != null
                && unwrapped.getCause() != unwrapped) {
            unwrapped = Utils.throwableUnwrap(unwrapped.getCause());
        }
        return unwrapped;
    }

    /**
     * 记录业务成功，日志写入失败时不重复执行业务。
     *
     * @param jobName 任务名称
     * @param executionId 执行链标识
     * @param triggerType 触发类型
     * @param success 成功执行信息
     */
    private void recordSuccess(
            String jobName, String executionId, String triggerType,
            ExecutionSuccess success) {
        try {
            record(jobName, executionId, SUCCESS, triggerType, success.attempt(),
                    success.startedAt(), success.endedAt(), null);
        } catch (RuntimeException logFailure) {
            log.error("定时任务已执行成功，但写入成功日志失败，jobName={}, attempt={}",
                    jobName, success.attempt(), logFailure);
        }
    }

    /**
     * 记录业务失败，并把日志异常附加到原业务异常。
     *
     * @param jobName 任务名称
     * @param executionId 执行链标识
     * @param status 执行状态
     * @param triggerType 触发类型
     * @param attempt 当前尝试次数
     * @param startedAt 开始时间
     * @param endedAt 结束时间
     * @param businessFailure 业务异常
     */
    private void recordBusinessFailure(
            String jobName, String executionId, String status,
            String triggerType, int attempt, long startedAt, long endedAt,
            Throwable businessFailure) {
        try {
            record(jobName, executionId, status, triggerType,
                    attempt, startedAt, endedAt, businessFailure);
        } catch (RuntimeException logFailure) {
            businessFailure.addSuppressed(logFailure);
            log.error("写入定时任务失败日志失败，jobName={}, attempt={}",
                    jobName, attempt, logFailure);
        }
    }

    /**
     * 记录首次尝试的基础设施异常。
     *
     * @param jobName 任务名称
     * @param executionId 执行链标识
     * @param triggerType 触发类型
     * @param startedAt 开始时间
     * @param failure 基础设施异常
     */
    private void recordInfrastructureFailure(
            String jobName, String executionId, String triggerType,
            long startedAt, Throwable failure) {
        recordInfrastructureFailure(
                jobName, executionId, triggerType, 1, startedAt, failure);
    }

    /**
     * 记录指定尝试次数的基础设施异常。
     *
     * @param jobName 任务名称
     * @param executionId 执行链标识
     * @param triggerType 触发类型
     * @param attempt 当前尝试次数
     * @param startedAt 开始时间
     * @param failure 基础设施异常
     */
    private void recordInfrastructureFailure(
            String jobName, String executionId, String triggerType,
            int attempt, long startedAt, Throwable failure) {
        try {
            record(jobName, executionId, FAILED,
                    triggerType, attempt, startedAt, failure);
        } catch (RuntimeException logFailure) {
            failure.addSuppressed(logFailure);
            log.error("写入定时任务基础设施失败日志失败，jobName={}", jobName, logFailure);
        }
    }

    /**
     * 以当前时间作为结束时间写入执行记录。
     *
     * @param jobName 任务名称
     * @param executionId 执行链标识
     * @param status 执行状态
     * @param triggerType 触发类型
     * @param attempt 当前尝试次数
     * @param startedAt 开始时间
     * @param failure 执行异常
     */
    private void record(
            String jobName, String executionId, String status,
            String triggerType, int attempt, long startedAt,
            Throwable failure) {
        record(jobName, executionId, status, triggerType, attempt, startedAt,
                System.currentTimeMillis(), failure);
    }

    /**
     * 写入一条完整的任务执行尝试记录。
     *
     * @param jobName 任务名称
     * @param executionId 执行链标识
     * @param status 执行状态
     * @param triggerType 触发类型
     * @param attempt 当前尝试次数
     * @param startedAt 开始时间
     * @param endedAt 结束时间
     * @param failure 执行异常
     */
    private void record(
            String jobName, String executionId, String status,
            String triggerType, int attempt, long startedAt,
            long endedAt, Throwable failure) {
        SysScheduledJobLog logEntity = new SysScheduledJobLog()
                .setJobName(jobName)
                .setExecutionId(executionId)
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

    /**
     * 根据项目缓存前缀构建定时任务 Redis 键。
     *
     * @param suffix 键后缀
     * @return 完整 Redis 键
     */
    private static String redisKey(String suffix) {
        String header = Solon.cfg() == null
                ? "jimuqu" : Solon.cfg().get("jimuqu.cache.keyHeader", "jimuqu");
        return header.endsWith(":") ? header + suffix : header + ":" + suffix;
    }

    /**
     * 生成可持久化的单行异常摘要。
     *
     * @param failure 执行异常
     * @return 异常摘要，无异常时返回 null
     */
    private static String errorSummary(Throwable failure) {
        if (failure == null) {
            return null;
        }
        String message = failure.getMessage();
        String summary = failure.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return truncate(summary.replaceAll("[\\r\\n\\t]+", " "), ERROR_SUMMARY_LENGTH);
    }

    /**
     * 截断超过字段上限的文本。
     *
     * @param value 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 一次已认领执行。
     *
     * @param executionId 执行链标识
     * @param cycleId 调度周期，非正常调度时为空
     * @param token 当前租约 owner token，非正常调度时为空
     * @param triggerType 本次周期的触发类型
     * @param keys 本次周期的 Redis 状态键
     */
    private record ExecutionClaim(
            String executionId, Long cycleId, String token,
            String triggerType, ClaimKeys keys) {
    }

    /**
     * 同一配置代际使用的 Redis 状态键。
     *
     * @param pending PENDING 租约索引
     * @param states 周期认领状态
     * @param completed COMPLETED 清理索引
     * @param anchor 当前代际调度水位
     * @param waterline 当前调度定义代际的任务调度水位
     * @param legacyWaterline 兼容旧启动恢复逻辑的任务调度水位
     */
    private record ClaimKeys(
            String pending, String states, String completed,
            String anchor, String waterline,
            String legacyWaterline) {
    }

    /**
     * 恢复执行水位。
     *
     * @param coverageThrough 已覆盖的调度时间
     * @param ttlMillis 水位存活毫秒数，零表示有限 Cron 的永久终态
     */
    private record RecoveryWatermark(
            long coverageThrough, long ttlMillis) {
    }

    /**
     * 一个可停止的周期租约心跳。
     *
     * @param future 共享调度线程池中的心跳任务
     */
    private record LeaseHeartbeat(ScheduledFuture<?> future) {

        /**
         * 非周期执行使用的空心跳。
         */
        private static final LeaseHeartbeat NONE =
                new LeaseHeartbeat(null);

        /**
         * 停止后续心跳，不中断已经开始的 Redis 原子续租。
         */
        private void stop() {
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    /**
     * 一次处理器成功执行结果。
     *
     * @param attempt 成功尝试次数
     * @param startedAt 开始时间
     * @param endedAt 结束时间
     */
    private record ExecutionSuccess(int attempt, long startedAt, long endedAt) {
    }
}
