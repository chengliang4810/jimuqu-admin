package com.jimuqu.system.service;

import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.scheduling.ScheduledAnno;
import org.noear.solon.scheduling.ScheduledException;
import org.noear.solon.scheduling.scheduled.Job;
import org.noear.solon.scheduling.scheduled.JobHandler;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduledJobInterceptorLockingTest {

    @Test
    void allowsOnlyOneNodeToClaimTheSameScheduledCycle() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        ScheduledJobInterceptor interceptor = interceptor(redis);
        Job job = job("clusterJob", new ScheduledAnno().fixedRate(1_000L), new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Future<?>> results = new ArrayList<>();
            for (int node = 0; node < 3; node++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    await(start);
                    invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());
                }));
            }
            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> result : results) {
                result.get(2, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, executions.get());
        assertTrue(redis.state(false, 1_000L)
                        .startsWith("COMPLETED|"),
                "正常完成必须从 PENDING 转为 COMPLETED");
    }

    @Test
    void nextCronCycleCanRunWhileThePreviousCycleIsStillRunning() throws Exception {
        assertNextCycleIsNotExecutionLocked(
                new ScheduledAnno().cron("0/1 * * * * ? *"), 1_100L, 2_000L);
    }

    @Test
    void nextFixedRateCycleCanRunWhileThePreviousCycleIsStillRunning() throws Exception {
        assertNextCycleIsNotExecutionLocked(
                new ScheduledAnno().fixedRate(1_000L), 1_100L, 2_100L);
    }

    /** 验证跨越 epoch 时间桶不足一个完整周期时 fixedRate 不会重复执行。 */
    @Test
    void fixedRateDoesNotRunTwiceWhenEpochBucketsChangeBeforeTheIntervalElapses() {
        RedisFixture redis = new RedisFixture(1_999L);
        ScheduledJobInterceptor interceptor = interceptor(redis);
        Job job = job("fixedRateRollingWindowJob",
                new ScheduledAnno().fixedRate(1_000L), new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();

        invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());
        redis.time.set(2_001L);
        invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());

        assertEquals(1, executions.get(),
                "跨越 epoch 时间桶但未经过完整 fixedRate 周期时不得重复执行");
    }

    @Test
    void fixedDelayKeepsTheExecutionLockAndStartsDelayAtCompletion() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        ScheduledJobInterceptor interceptor = interceptor(redis);
        Job job = job("fixedDelayJob",
                new ScheduledAnno().fixedDelay(1_000L), new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> invoke(interceptor, redis.client, job, ignored -> {
            executions.incrementAndGet();
            firstEntered.countDown();
            await(releaseFirst);
        }));
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());
            assertEquals(1, executions.get(), "fixedDelay must not overlap");

            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);

            redis.time.set(1_999L);
            invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());
            assertEquals(1, executions.get(), "delay starts when the previous run completes");

            redis.time.set(2_000L);
            invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());
            assertEquals(2, executions.get());
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentManualRunIsSkippedWithoutWaiting() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setEnabled(true)
                .setJobSource(ScheduledJobConfigService.SOURCE_SYSTEM)
                .setConcurrentPolicy(ScheduledJobConfigService.CONCURRENT_FORBID)
                .setMaxRetries(0)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, logs, config);
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        JobHandler handler = ignored -> {
            if (executions.incrementAndGet() == 1) {
                firstEntered.countDown();
                await(releaseFirst);
            }
        };
        Future<?> first = pool.submit(() -> invoke(
                interceptor, redis.client, manualJob("manual-1"), handler));
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            Future<?> second = pool.submit(() -> invoke(
                    interceptor, redis.client, manualJob("manual-2"), handler));
            second.get(1, TimeUnit.SECONDS);
            assertEquals(1, executions.get());
            assertEquals(List.of("SKIPPED"),
                    logs.stream().map(SysScheduledJobLog::getStatus).toList());

            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            assertEquals(1, executions.get());
            assertEquals(List.of("SKIPPED", "SUCCESS"),
                    logs.stream().map(SysScheduledJobLog::getStatus).toList());
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    /**
     * 验证系统任务配置为允许并发时，两次手动执行可以同时进入处理器。
     */
    @Test
    void concurrentManualRunIsAllowedByPolicy() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setEnabled(true)
                .setJobSource(ScheduledJobConfigService.SOURCE_SYSTEM)
                .setConcurrentPolicy(ScheduledJobConfigService.CONCURRENT_ALLOW)
                .setMaxRetries(0)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, logs, config);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        JobHandler handler = ignored -> {
            int current = executions.incrementAndGet();
            if (current == 1) {
                firstEntered.countDown();
            } else {
                secondEntered.countDown();
            }
            await(release);
        };
        Future<?> first = pool.submit(() -> invoke(
                interceptor, redis.client, manualJob("allow-manual-1"), handler));
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            Future<?> second = pool.submit(() -> invoke(
                    interceptor, redis.client,
                    manualJob("allow-manual-2"), handler));
            assertTrue(secondEntered.await(1, TimeUnit.SECONDS),
                    "允许并发的手动执行不得被系统任务来源强制互斥");
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
            assertEquals(2, executions.get());
            assertEquals(2L, logs.stream()
                    .filter(log -> "SUCCESS".equals(log.getStatus()))
                    .count());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    /** 验证启用失败重试后，即使旧配置仍为 ALLOW 也必须强制互斥。 */
    @Test
    void retryingJobCannotExecuteConcurrentlyWithLegacyAllowPolicy()
            throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setEnabled(true)
                .setJobSource(ScheduledJobConfigService.SOURCE_SYSTEM)
                .setConcurrentPolicy(ScheduledJobConfigService.CONCURRENT_ALLOW)
                .setMaxRetries(1)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, logs, config);
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        JobHandler handler = ignored -> {
            if (executions.incrementAndGet() == 1) {
                firstEntered.countDown();
                await(releaseFirst);
            }
        };
        Future<?> first = pool.submit(() -> invoke(
                interceptor, redis.client,
                manualJob("retrying-manual-1"), handler));
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            Future<?> second = pool.submit(() -> invoke(
                    interceptor, redis.client,
                    manualJob("retrying-manual-2"), handler));
            second.get(1, TimeUnit.SECONDS);

            assertEquals(1, executions.get(),
                    "长间隔重试不得与后续触发并发占用线程");
            assertEquals(List.of("SKIPPED"),
                    logs.stream()
                            .map(SysScheduledJobLog::getStatus)
                            .toList());
        } finally {
            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
    }

    /** 验证多节点恢复去重不会占用正常调度周期。 */
    @Test
    void recoveryIsClusterUniqueAndDoesNotConsumeScheduledCycles() {
        RedisFixture redis = new RedisFixture(1_500L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor firstNode = interceptor(redis, logs);
        ScheduledJobInterceptor secondNode = interceptor(redis, logs);
        AtomicInteger executions = new AtomicInteger();
        JobHandler handler = ignored -> executions.incrementAndGet();

        invoke(firstNode, redis.client, recoveryJob("recoveryJob", 1_000L), handler);
        invoke(secondNode, redis.client, recoveryJob("recoveryJob", 1_000L), handler);
        assertEquals(1, executions.get(), "同一错过周期只能由一个节点恢复执行");
        assertEquals(1L, logs.stream()
                .filter(log -> "RECOVERY".equals(log.getTriggerType()))
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .count());
        assertEquals(0L, logs.stream()
                .filter(log -> "RECOVERY".equals(log.getTriggerType()))
                .filter(log -> "SKIPPED".equals(log.getStatus()))
                .count());
        assertTrue(logs.stream()
                        .filter(log -> "RECOVERY".equals(log.getTriggerType()))
                        .allMatch(log -> "recovery:1000".equals(
                                log.getExecutionId())),
                "同一错过周期的多节点恢复日志必须共用 recovery:<cycle> executionId");

        Job scheduled = job(
                "recoveryJob", new ScheduledAnno().fixedRate(1_000L), new ContextEmpty());
        invoke(firstNode, redis.client, scheduled, handler);
        invoke(secondNode, redis.client, scheduled, handler);
        assertEquals(1, executions.get(),
                "恢复执行必须原子预占零延迟首次触发，避免 fallback 与 Quartz 双执行");

        redis.time.set(2_500L);
        invoke(secondNode, redis.client, scheduled, handler);
        assertEquals(2, executions.get(), "恢复后的下一正常调度周期必须继续执行");
        assertEquals(1L, logs.stream()
                .filter(log -> "SCHEDULED".equals(log.getTriggerType()))
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .count());
    }

    /**
     * 验证恢复水位只覆盖已经发生的周期，不会把下一边界后的短期故障
     * 误判为已补偿。
     */
    @Test
    void recoveryWatermarkDoesNotCoverTheNextUnexecutedCycle() {
        RedisFixture redis = new RedisFixture(1_500L);
        AtomicInteger executions = new AtomicInteger();
        ScheduledJobInterceptor interceptor = interceptor(redis);
        ScheduledAnno scheduled = new ScheduledAnno().fixedRate(1_000L);

        invoke(
                interceptor, redis.client,
                recoveryJob("consecutiveRecoveryJob", 1_000L, scheduled),
                ignored -> executions.incrementAndGet());
        redis.time.set(2_600L);
        invoke(
                interceptor, redis.client,
                recoveryJob("consecutiveRecoveryJob", 2_500L, scheduled),
                ignored -> executions.incrementAndGet());

        assertEquals(2, executions.get(),
                "下一调度边界后的新故障必须允许再次补偿");
    }

    /** 验证正常调度先认领时会原子阻止同一历史周期的恢复执行。 */
    @Test
    void scheduledClaimSuppressesRecoveryForTheCoveredCycle() {
        RedisFixture redis = new RedisFixture(1_500L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        AtomicInteger executions = new AtomicInteger();
        JobHandler handler = ignored -> executions.incrementAndGet();
        Job scheduled = job(
                "scheduledWinsJob",
                new ScheduledAnno().fixedRate(1_000L),
                new ContextEmpty());

        invoke(interceptor, redis.client, scheduled, handler);
        invoke(interceptor, redis.client,
                recoveryJob("scheduledWinsJob", 1_000L), handler);

        assertEquals(1, executions.get());
        assertEquals(0L, logs.stream()
                .filter(log -> "RECOVERY".equals(log.getTriggerType()))
                .filter(log -> "SKIPPED".equals(log.getStatus()))
                .count());
    }

    /**
     * 验证活跃心跳跨过原租约仍不会被接管，心跳停止后可接管，
     * 且旧 owner 迟到完成不能覆盖新 owner 的 COMPLETED 状态。
     */
    @Test
    void heartbeatProtectsLongTaskAndExpiredOwnerIsTakenOver()
            throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        Job job = job(
                "longLeaseJob",
                new ScheduledAnno().fixedRate(60_000L),
                new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> {
            try {
                interceptor.doIntercept(redis.client, job, ignored -> {
                    executions.incrementAndGet();
                    firstEntered.countDown();
                    await(releaseFirst);
                });
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        });
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            redis.time.set(30_000L);
            redis.runHeartbeats();
            redis.time.set(40_000L);
            invoke(interceptor, redis.client, job,
                    ignored -> executions.incrementAndGet());
            assertEquals(1, executions.get(),
                    "心跳已经延长租约时不得接管仍在运行的周期");

            redis.stopHeartbeats();
            redis.time.set(60_000L);
            invoke(interceptor, redis.client, job,
                    ignored -> executions.incrementAndGet());
            assertEquals(2, executions.get(),
                    "心跳停止且续租到期后必须接管同一周期");

            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            assertInstanceOf(
                    IllegalStateException.class, firstFailure.get());
            assertEquals(
                    "定时任务执行完成时已失去周期所有权",
                    firstFailure.get().getMessage());
            assertTrue(redis.state(false, 1_000L)
                            .startsWith("COMPLETED|"),
                    "旧 owner 迟到完成不得覆盖新 owner 的终态");
            assertEquals(1L, logs.stream()
                    .filter(log -> "scheduled:1000".equals(
                            log.getExecutionId()))
                    .filter(log -> "SUCCESS".equals(log.getStatus()))
                    .count());
            assertEquals(1L, logs.stream()
                    .filter(log -> "scheduled:1000".equals(
                            log.getExecutionId()))
                    .filter(log -> "FAILED".equals(log.getStatus()))
                    .count());
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    /** 验证一次正常触发会接管全部到期重叠周期而不是只处理最近一个。 */
    @Test
    void allExpiredOverlappingCyclesAreTakenOver() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        Job job = job(
                "overlappingLeaseJob",
                new ScheduledAnno().fixedRate(1_000L),
                new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch ownersEntered = new CountDownLatch(2);
        CountDownLatch releaseOwners = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> owners = new ArrayList<>();
        try {
            owners.add(pool.submit(() -> invoke(
                    interceptor, redis.client, job, ignored -> {
                        executions.incrementAndGet();
                        ownersEntered.countDown();
                        await(releaseOwners);
                    })));
            while (ownersEntered.getCount() == 2L) {
                Thread.yield();
            }
            redis.time.set(2_000L);
            owners.add(pool.submit(() -> invoke(
                    interceptor, redis.client, job, ignored -> {
                        executions.incrementAndGet();
                        ownersEntered.countDown();
                        await(releaseOwners);
                    })));
            assertTrue(ownersEntered.await(2, TimeUnit.SECONDS));

            redis.stopHeartbeats();
            redis.time.set(32_000L);
            invoke(interceptor, redis.client, job,
                    ignored -> executions.incrementAndGet());
            assertEquals(5, executions.get(),
                    "两个到期周期和当前周期都必须执行");
            assertEquals(1L, logs.stream()
                    .filter(log -> "scheduled:1000".equals(
                            log.getExecutionId()))
                    .filter(log -> "SUCCESS".equals(log.getStatus()))
                    .count());
            assertEquals(1L, logs.stream()
                    .filter(log -> "scheduled:2000".equals(
                            log.getExecutionId()))
                    .filter(log -> "SUCCESS".equals(log.getStatus()))
                    .count());
        } finally {
            releaseOwners.countDown();
            for (Future<?> owner : owners) {
                try {
                    owner.get(2, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // 旧 owner 迟到完成按预期报告失去所有权。
                }
            }
            pool.shutdownNow();
        }
    }

    /** 验证恢复任务 owner 崩溃后同一恢复周期能够由另一节点接管。 */
    @Test
    void crashedRecoveryOwnerCanBeTakenOver() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        Job recovery = recoveryJob("recoveryTakeoverJob", 500L);
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> {
            try {
                interceptor.doIntercept(
                        redis.client, recovery, ignored -> {
                            executions.incrementAndGet();
                            firstEntered.countDown();
                            await(releaseFirst);
                        });
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        });
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            redis.stopHeartbeats();
            redis.time.set(31_000L);
            invoke(interceptor, redis.client,
                    recoveryJob("recoveryTakeoverJob", 500L),
                    ignored -> executions.incrementAndGet());
            assertEquals(2, executions.get());

            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            assertInstanceOf(
                    IllegalStateException.class, firstFailure.get());
            assertTrue(redis.state(true, 500L)
                    .startsWith("COMPLETED|"));
            assertEquals(1L, logs.stream()
                    .filter(log -> "recovery:500".equals(
                            log.getExecutionId()))
                    .filter(log -> "SUCCESS".equals(log.getStatus()))
                    .count());
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    /**
     * 验证第二节点在恢复租约仍有效时会登记到期重试，
     * 首节点随后崩溃也不需要等待下一次正常业务调度。
     */
    @Test
    void recoveryLeaseExpiryAutomaticallyTriggersTakeover()
            throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor firstNode = interceptor(redis, logs);
        ScheduledJobInterceptor secondNode = interceptor(redis, logs);
        Job recovery = recoveryJob("automaticRecoveryJob", 500L);
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> {
            try {
                firstNode.doIntercept(
                        redis.client, recovery, ignored -> {
                            executions.incrementAndGet();
                            firstEntered.countDown();
                            await(releaseFirst);
                        });
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        });
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            invoke(
                    secondNode, redis.client,
                    recoveryJob("automaticRecoveryJob", 500L),
                    ignored -> executions.incrementAndGet());
            assertEquals(1, redis.leaseRetryTaskCount(),
                    "未取得恢复租约的节点必须登记一次到期重试");

            redis.time.set(20_000L);
            redis.runHeartbeats();
            redis.time.set(31_001L);
            redis.runLeaseRetries();
            assertEquals(1, executions.get(),
                    "活跃 owner 续租后不得被原到期重试接管");
            assertEquals(1, redis.leaseRetryTaskCount(),
                    "原到期重试发现新租约后必须继续登记下一次检查");

            redis.stopHeartbeats();
            redis.time.set(50_001L);
            redis.runLeaseRetries();

            assertEquals(2, executions.get(),
                    "恢复租约到期后必须自动接管，不得依赖下一业务周期");
            assertTrue(redis.state(true, 500L)
                    .startsWith("COMPLETED|"));

            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            assertInstanceOf(
                    IllegalStateException.class, firstFailure.get());
            assertEquals(1L, logs.stream()
                    .filter(log -> "recovery:500".equals(
                            log.getExecutionId()))
                    .filter(log -> "SUCCESS".equals(log.getStatus()))
                    .count());
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    /**
     * 验证普通调度的其他节点会登记租约到期重试，
     * owner 崩溃后无需等待下一个调度周期。
     */
    @Test
    void scheduledLeaseExpiryAutomaticallyTriggersTakeover()
            throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor firstNode = interceptor(redis, logs);
        ScheduledJobInterceptor secondNode = interceptor(redis, logs);
        Job scheduled = job(
                "automaticScheduledJob",
                new ScheduledAnno().fixedRate(60_000L),
                new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> {
            try {
                firstNode.doIntercept(
                        redis.client, scheduled, ignored -> {
                            executions.incrementAndGet();
                            firstEntered.countDown();
                            await(releaseFirst);
                        });
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        });
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            invoke(
                    secondNode, redis.client, scheduled,
                    ignored -> executions.incrementAndGet());
            assertEquals(1, redis.leaseRetryTaskCount(),
                    "普通调度未取得租约时必须登记到期重试");

            redis.time.set(20_000L);
            redis.runHeartbeats();
            redis.time.set(31_001L);
            int logCountBeforeRetry = logs.size();
            redis.runLeaseRetries();
            assertEquals(1, executions.get(),
                    "活跃 owner 续租后普通调度不得提前接管");
            assertEquals(logCountBeforeRetry, logs.size(),
                    "内部租约重试未认领周期时不得重复写入跳过日志");
            assertEquals(1, redis.leaseRetryTaskCount(),
                    "普通调度必须按最新租约继续登记检查");

            redis.stopHeartbeats();
            redis.time.set(50_001L);
            redis.runLeaseRetries();

            assertEquals(2, executions.get(),
                    "普通调度租约到期后必须自动接管原周期");
            assertTrue(redis.state(false, 1_000L)
                    .startsWith("COMPLETED|"));

            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            assertInstanceOf(
                    IllegalStateException.class, firstFailure.get());
            assertEquals(1L, logs.stream()
                    .filter(log -> "scheduled:1000".equals(
                            log.getExecutionId()))
                    .filter(log -> "SUCCESS".equals(log.getStatus()))
                    .count());
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    /**
     * 验证旧动态 Job 登记的租约回调不会借新定义代际执行旧处理器。
     */
    @Test
    void staleDynamicLeaseRetryCannotExecuteOldHandler()
            throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        AtomicReference<SysScheduledJobConfig> currentConfig =
                new AtomicReference<>(dynamicConfig(7L, "handlerA"));
        ScheduledJobInterceptor firstNode =
                dynamicInterceptor(redis, logs, currentConfig);
        ScheduledJobInterceptor secondNode =
                dynamicInterceptor(redis, logs, currentConfig);
        Job ownerJob = dynamicJob("staleDynamicJob", currentConfig.get());
        Job staleRetryJob = dynamicJob(
                "staleDynamicJob", currentConfig.get());
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger staleHandlerExecutions = new AtomicInteger();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> invoke(
                firstNode, redis.client, ownerJob, ignored -> {
                    firstEntered.countDown();
                    await(releaseFirst);
                }));
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            invoke(
                    secondNode, redis.client, staleRetryJob,
                    ignored -> staleHandlerExecutions.incrementAndGet());
            assertEquals(1, redis.leaseRetryTaskCount());

            currentConfig.set(dynamicConfig(7L, "handlerB"));
            redis.stopHeartbeats();
            redis.time.set(31_001L);
            redis.runLeaseRetries();

            assertEquals(0, staleHandlerExecutions.get(),
                    "旧回调不得用新定义代际执行旧处理器");
            assertEquals(0, redis.leaseRetryTaskCount(),
                    "代际失效的回调不得继续登记租约重试");
        } finally {
            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
    }

    /**
     * 验证任务定义在首次回调前变更时，旧 Job 不会借用新定义代际。
     */
    @Test
    void staleDynamicJobBeforeFirstCallbackCannotUseNewGeneration() {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        SysScheduledJobConfig original = dynamicConfig(7L, "handlerA");
        AtomicReference<SysScheduledJobConfig> currentConfig =
                new AtomicReference<>(original);
        ScheduledJobInterceptor interceptor =
                dynamicInterceptor(redis, logs, currentConfig);
        Job staleJob = dynamicJob("staleDynamicJob", original);
        AtomicInteger executions = new AtomicInteger();

        currentConfig.set(dynamicConfig(7L, "handlerB"));
        invoke(
                interceptor, redis.client, staleJob,
                ignored -> executions.incrementAndGet());

        assertEquals(0, executions.get());
        assertEquals(1L, logs.stream()
                .filter(log -> "SKIPPED".equals(log.getStatus()))
                .count());
    }

    /**
     * 验证执行锁仍由 watchdog 占用时，重试延迟始终位于
     * 100 毫秒到 5 秒之间，不会在租约到期后形成 1ms 忙循环。
     */
    @Test
    void executionLockRetryUsesBoundedBackoff()
            throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setEnabled(true)
                .setConcurrentPolicy("FORBID")
                .setMaxRetries(0)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, null, config);
        Job scheduled = job(
                "boundedLockRetryJob",
                new ScheduledAnno().fixedRate(60_000L),
                new ContextEmpty());
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> invoke(
                interceptor, redis.client, scheduled, ignored -> {
                    firstEntered.countDown();
                    await(releaseFirst);
                }));
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            invoke(interceptor, redis.client, scheduled, ignored -> {
            });
            redis.time.set(31_000L);
            invoke(interceptor, redis.client, scheduled, ignored -> {
            });

            assertEquals(List.of(5_000L),
                    redis.leaseRetryDelays(),
                    "同一任务代际只能保留一个待执行租约重试");

            redis.runLeaseRetries();

            assertEquals(List.of(5_000L, 100L),
                    redis.leaseRetryDelays(),
                    "前一个重试开始执行后才能登记下一次有界退避");
        } finally {
            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
    }

    /**
     * 验证恢复水位覆盖跨年 Cron 的下一边界，
     * 没有未来边界的有限 Cron 使用永久终态。
     */
    @Test
    void recoveryWatermarkTtlCoversLongAndFiniteCron() {
        long leapClaimTime =
                Instant.parse("2025-03-01T00:00:00Z").toEpochMilli();
        RedisFixture redis = new RedisFixture(leapClaimTime);
        ScheduledJobInterceptor interceptor = interceptor(redis);
        ScheduledAnno leapCron = new ScheduledAnno()
                .cron("0 0 0 29 2 ? *")
                .zone("UTC");

        invoke(
                interceptor, redis.client,
                recoveryJob(
                        "leapCronRecoveryJob",
                        leapClaimTime - 1_000L, leapCron),
                ignored -> {
                });

        assertTrue(
                redis.lastRecoveryWatermarkTtlMs()
                        > TimeUnit.DAYS.toMillis(366),
                "跨闰年的恢复水位必须存活到下一 Cron 边界");

        long finiteClaimTime =
                Instant.parse("2026-01-02T00:00:00Z").toEpochMilli();
        redis.time.set(finiteClaimTime);
        ScheduledAnno finiteCron = new ScheduledAnno()
                .cron("0 0 0 1 1 ? 2025")
                .zone("UTC");
        invoke(
                interceptor, redis.client,
                recoveryJob(
                        "finiteCronRecoveryJob",
                        finiteClaimTime - 1_000L, finiteCron),
                ignored -> {
                });

        assertEquals(0L, redis.lastRecoveryWatermarkTtlMs(),
                "没有未来边界的有限 Cron 必须保留永久恢复终态");
    }

    /** 验证 Cron 合法触发秒保留当前边界，并由下一边界阻止窗口内重复执行。 */
    @Test
    void cronClaimUsesCurrentBoundaryAndNextBoundaryAnchor() {
        RedisFixture redis = new RedisFixture(1_100L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        Job cronJob = job(
                "cronBoundaryJob",
                new ScheduledAnno().cron("0/1 * * * * ? *"),
                new ContextEmpty());

        invoke(interceptor, redis.client, cronJob, ignored -> {
        });
        redis.time.set(1_999L);
        invoke(interceptor, redis.client, cronJob, ignored -> {
        });

        assertEquals(1L, logs.stream()
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .count());
        assertTrue(logs.stream()
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .allMatch(log -> "scheduled:1000".equals(
                        log.getExecutionId())));
    }

    /** 验证延迟到非触发秒的 Cron 回调仍归属于上一真实调度边界。 */
    @Test
    void delayedCronCallbackUsesPreviousFireBoundary() {
        RedisFixture redis = new RedisFixture(65_500L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        Job cronJob = job(
                "delayedCronJob",
                new ScheduledAnno()
                        .cron("0 * * * * ? *")
                        .zone("UTC"),
                new ContextEmpty());

        invoke(interceptor, redis.client, cronJob, ignored -> {
        });

        assertEquals(List.of("scheduled:60000"),
                logs.stream()
                        .filter(log -> "SUCCESS".equals(log.getStatus()))
                        .map(SysScheduledJobLog::getExecutionId)
                        .toList());
    }

    /** 验证长周期 Cron 在非触发秒也会持续去重到下一合法边界。 */
    @Test
    void longCronWindowDoesNotFallBackToCurrentMilliseconds() {
        long firstClaim = Instant.parse(
                "2026-07-25T00:00:00.100Z").toEpochMilli();
        RedisFixture redis = new RedisFixture(firstClaim);
        AtomicInteger executions = new AtomicInteger();
        ScheduledJobInterceptor interceptor = interceptor(redis);
        Job cronJob = job(
                "longCronWindowJob",
                new ScheduledAnno()
                        .cron("0 0 0 1 1 ? *")
                        .zone("UTC"),
                new ContextEmpty());

        invoke(interceptor, redis.client, cronJob,
                ignored -> executions.incrementAndGet());
        redis.time.set(firstClaim + 1_100L);
        invoke(interceptor, redis.client, cronJob,
                ignored -> executions.incrementAndGet());

        assertEquals(1, executions.get(),
                "Cron 去重必须覆盖到下一合法触发边界");
    }

    /** 验证 Redis 代际只随可执行调度定义变化。 */
    @Test
    void definitionGenerationIgnoresOperationalOnlyChanges() {
        SysScheduledJobConfig baseline = definitionConfig(
                "handlerA", "FIXED_RATE", "1000", "Asia/Shanghai", 10L);
        SysScheduledJobConfig operationalOnly = definitionConfig(
                "handlerA", "FIXED_RATE", "1000", "Asia/Shanghai", 10L)
                .setDescription("新的说明")
                .setEnabled(false)
                .setMaxRetries(8)
                .setRetryIntervalMs(999L)
                .setConcurrentPolicy("FORBID");

        assertEquals(
                ScheduledJobInterceptor.definitionId(baseline),
                ScheduledJobInterceptor.definitionId(operationalOnly));
        assertNotEquals(
                ScheduledJobInterceptor.definitionId(baseline),
                ScheduledJobInterceptor.definitionId(definitionConfig(
                        "handlerB", "FIXED_RATE", "1000",
                        "Asia/Shanghai", 10L)));
        assertNotEquals(
                ScheduledJobInterceptor.definitionId(baseline),
                ScheduledJobInterceptor.definitionId(definitionConfig(
                        "handlerA", "CRON", "0/1 * * * * ? *",
                        "Asia/Shanghai", 10L)));
        assertNotEquals(
                ScheduledJobInterceptor.definitionId(baseline),
                ScheduledJobInterceptor.definitionId(definitionConfig(
                        "handlerA", "FIXED_RATE", "2000",
                        "Asia/Shanghai", 10L)));
        assertNotEquals(
                ScheduledJobInterceptor.definitionId(baseline),
                ScheduledJobInterceptor.definitionId(definitionConfig(
                        "handlerA", "FIXED_RATE", "1000",
                        "UTC", 10L)));
        assertNotEquals(
                ScheduledJobInterceptor.definitionId(baseline),
                ScheduledJobInterceptor.definitionId(definitionConfig(
                        "handlerA", "FIXED_RATE", "1000",
                        "Asia/Shanghai", 20L)));
    }

    /** 验证代码任务调度注解变化后不会复用旧 Redis 调度代际。 */
    @Test
    void systemScheduleChangeStartsANewClaimGeneration() {
        RedisFixture redis = new RedisFixture(1_000L);
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setConfigId(91L)
                .setJobSource(ScheduledJobConfigService.SOURCE_SYSTEM)
                .setEnabled(true)
                .setConcurrentPolicy("ALLOW")
                .setMaxRetries(0)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, null, config);
        AtomicInteger executions = new AtomicInteger();

        invoke(
                interceptor, redis.client,
                job(
                        "systemGenerationJob",
                        new ScheduledAnno().fixedRate(1_000L),
                        new ContextEmpty()),
                ignored -> executions.incrementAndGet());
        invoke(
                interceptor, redis.client,
                job(
                        "systemGenerationJob",
                        new ScheduledAnno().fixedRate(2_000L),
                        new ContextEmpty()),
                ignored -> executions.incrementAndGet());

        assertEquals(2, executions.get(),
                "代码任务的新调度定义不得被旧 anchor 抑制");
    }

    /** 验证代码任务调度注解变化后不会复用旧恢复水位。 */
    @Test
    void systemScheduleChangeStartsANewRecoveryGeneration() {
        RedisFixture redis = new RedisFixture(1_500L);
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setConfigId(92L)
                .setJobSource(ScheduledJobConfigService.SOURCE_SYSTEM)
                .setEnabled(true)
                .setConcurrentPolicy("ALLOW")
                .setMaxRetries(0)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, null, config);
        AtomicInteger executions = new AtomicInteger();

        invoke(
                interceptor, redis.client,
                recoveryJob(
                        "systemRecoveryGenerationJob", 1_000L,
                        new ScheduledAnno().fixedRate(1_000L)),
                ignored -> executions.incrementAndGet());
        invoke(
                interceptor, redis.client,
                recoveryJob(
                        "systemRecoveryGenerationJob", 1_000L,
                        new ScheduledAnno().fixedRate(2_000L)),
                ignored -> executions.incrementAndGet());

        assertEquals(2, executions.get(),
                "代码任务的新调度定义不得复用旧恢复水位");
    }

    @Test
    void fixedDelayCompletionMarkerFailureKeepsLockUntilDelayEnds() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        redis.failCompletionScript(
                1, new IllegalStateException("completion marker unavailable"));
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        Job job = job("fixedDelayMarkerJob",
                new ScheduledAnno().fixedDelay(200L), new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        long startedAt = System.nanoTime();
        Future<?> first = pool.submit(() -> {
            try {
                interceptor.doIntercept(redis.client, job, ignored -> {
                    executions.incrementAndGet();
                    redis.clearMarkers();
                });
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        try {
            assertTrue(redis.completionFailureObserved.await(
                    2, TimeUnit.SECONDS));
            invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());
            assertEquals(1, executions.get(), "marker 失效后仍必须由执行锁守住 fixedDelay");

            first.get(2, TimeUnit.SECONDS);
            assertInstanceOf(IllegalStateException.class, failure.get());
            assertEquals("completion marker unavailable", failure.get().getMessage());
            assertTrue(System.nanoTime() - startedAt >= TimeUnit.MILLISECONDS.toNanos(180L),
                    "完成标记失败也必须从任务完成时刻等待 fixedDelay");

            invoke(interceptor, redis.client, job, ignored -> executions.incrementAndGet());
            assertEquals(2, executions.get(), "fixedDelay 到期后必须释放锁");
            assertEquals(1L, logs.stream().filter(log -> "FAILED".equals(log.getStatus())).count());
            assertEquals(0L, logs.stream().filter(log -> "SKIPPED".equals(log.getStatus())).count());
            assertEquals(1L, logs.stream().filter(log -> "SUCCESS".equals(log.getStatus())).count());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void fixedDelayFallbackWaitIgnoresInterruptAndRestoresIt() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        redis.failCompletionScript(
                1, new IllegalStateException("completion marker unavailable"));
        ScheduledJobInterceptor interceptor = interceptor(redis);
        Job job = job("interruptedFixedDelayJob",
                new ScheduledAnno().fixedDelay(150L), new ContextEmpty());
        AtomicBoolean interruptedAfterWait = new AtomicBoolean();
        AtomicLong elapsedNanos = new AtomicLong();
        Thread worker = new Thread(() -> {
            long startedAt = System.nanoTime();
            try {
                interceptor.doIntercept(redis.client, job, ignored -> redis.clearMarkers());
            } catch (Throwable ignored) {
                elapsedNanos.set(System.nanoTime() - startedAt);
                interruptedAfterWait.set(Thread.currentThread().isInterrupted());
            }
        }, "fixed-delay-fallback-interrupt-test");

        worker.start();
        assertTrue(redis.completionFailureObserved.await(
                2, TimeUnit.SECONDS));
        worker.interrupt();
        Thread.sleep(30L);
        assertTrue(worker.isAlive(), "中断不得提前释放 fixedDelay 执行锁");
        worker.join(2_000L);

        assertFalse(worker.isAlive());
        assertTrue(elapsedNanos.get() >= TimeUnit.MILLISECONDS.toNanos(130L));
        assertTrue(interruptedAfterWait.get(), "等待结束后必须恢复中断标志");
    }

    @Test
    void markerFailureDoesNotReplaceBusinessFailureOrDuplicateFailedLog() {
        RedisFixture redis = new RedisFixture(1_000L);
        redis.failCompletionScript(
                1, new IllegalStateException("completion marker unavailable"));
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(redis, logs);
        Job job = job("failedFixedDelayJob",
                new ScheduledAnno().fixedDelay(1_000L), new ContextEmpty());

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> interceptor.doIntercept(
                        redis.client, job, ignored -> {
                            throw new IllegalArgumentException("business failure");
                        }));

        assertEquals("business failure", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("completion marker unavailable", failure.getSuppressed()[0].getMessage());
        assertEquals(List.of("FAILED"),
                logs.stream().map(SysScheduledJobLog::getStatus).toList());
    }

    @Test
    void unlockFailureDoesNotReplaceBusinessFailure() {
        RedisFixture redis = new RedisFixture(1_000L);
        redis.failUnlock(new IllegalStateException("unlock unavailable"));
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setEnabled(true)
                .setJobSource(ScheduledJobConfigService.SOURCE_SYSTEM)
                .setConcurrentPolicy(ScheduledJobConfigService.CONCURRENT_FORBID)
                .setMaxRetries(0)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, null, config);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> interceptor.doIntercept(
                        redis.client, manualJob("unlock-failure"), ignored -> {
                            throw new IllegalArgumentException("business failure");
                        }));

        assertEquals("business failure", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("unlock unavailable", failure.getSuppressed()[0].getMessage());
    }

    @Test
    void interruptedRetryWaitDoesNotInventAnUnexecutedAttempt() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        ScheduledJobConfigService configService = mock(ScheduledJobConfigService.class);
        SysScheduledJobLogMapper logMapper = mock(SysScheduledJobLogMapper.class);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        CountDownLatch handlerFailed = new CountDownLatch(1);
        when(configService.getOrCreate(anyString(), anyBoolean())).thenReturn(
                new SysScheduledJobConfig()
                        .setEnabled(true)
                        .setMaxRetries(1)
                        .setRetryIntervalMs(60_000L));
        doAnswer(invocation -> {
            SysScheduledJobLog entry = invocation.getArgument(0);
            logs.add(entry);
            return 1;
        }).when(logMapper).save(any(SysScheduledJobLog.class));
        ScheduledJobInterceptor interceptor = new ScheduledJobInterceptor(
                configService, logMapper, redis.scheduler);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                interceptor.doIntercept(
                        redis.client,
                        manualJob("interrupted-retry"),
                        ignored -> {
                            handlerFailed.countDown();
                            throw new IllegalStateException("planned retry");
                        });
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        }, "scheduled-job-interrupted-retry-test");

        worker.start();
        try {
            assertTrue(handlerFailed.await(2, TimeUnit.SECONDS));
            Thread.sleep(100L);
            worker.interrupt();
            worker.join(2_000L);

            assertFalse(worker.isAlive());
            assertInstanceOf(InterruptedException.class, failure.get());
            assertEquals(List.of("FAILED"),
                    logs.stream().map(SysScheduledJobLog::getStatus).toList());
            assertEquals(1, logs.get(0).getAttempt());
            assertTrue(logs.get(0).getErrorSummary().contains("planned retry"));
        } finally {
            worker.interrupt();
            worker.join(2_000L);
        }
    }

    /** 验证代理包装的中断异常不会进入重试且保留真实异常。 */
    @Test
    void proxiedInterruptedExceptionIsUnwrappedAndNeverRetried() {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor =
                retryingInterceptor(redis, logs);
        AtomicInteger attempts = new AtomicInteger();
        InterruptedException planned =
                new InterruptedException("planned proxied interruption");
        try {
            InterruptedException failure = assertThrows(
                    InterruptedException.class,
                    () -> interceptor.doIntercept(
                            redis.client,
                            manualJob("proxied-interruption"),
                            ignored -> {
                                attempts.incrementAndGet();
                                throw new ScheduledException(planned);
                            }));

            assertSame(planned, failure);
            assertEquals(1, attempts.get());
            assertEquals(List.of("FAILED"),
                    logs.stream().map(SysScheduledJobLog::getStatus).toList());
            assertEquals(1, logs.get(0).getAttempt());
            assertTrue(Thread.currentThread().isInterrupted(),
                    "解包后的 InterruptedException 必须恢复线程中断标志");
        } finally {
            Thread.interrupted();
        }
    }

    /** 验证代理包装的 Error 不会进入重试且保留真实错误。 */
    @Test
    void proxiedErrorIsUnwrappedAndNeverRetried() {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor =
                retryingInterceptor(redis, logs);
        AtomicInteger attempts = new AtomicInteger();
        AssertionError planned = new AssertionError("planned proxied error");

        AssertionError failure = assertThrows(
                AssertionError.class,
                () -> interceptor.doIntercept(
                        redis.client,
                        manualJob("proxied-error"),
                        ignored -> {
                            attempts.incrementAndGet();
                            throw new ScheduledException(planned);
                        }));

        assertSame(planned, failure);
        assertEquals(1, attempts.get());
        assertEquals(List.of("FAILED"),
                logs.stream().map(SysScheduledJobLog::getStatus).toList());
        assertEquals(1, logs.get(0).getAttempt());
    }

    /**
     * 验证业务处理器可读取稳定执行上下文，且退出后不会污染工作线程。
     */
    @Test
    void exposesStableExecutionContextAcrossRetriesAndClearsIt() {
        RedisFixture redis = new RedisFixture(1_000L);
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setEnabled(true)
                .setConcurrentPolicy(ScheduledJobConfigService.CONCURRENT_ALLOW)
                .setMaxRetries(1)
                .setRetryIntervalMs(0L);
        ScheduledJobInterceptor interceptor =
                interceptor(redis, logs, config);
        List<ScheduledJobExecutionContext.Execution> contexts =
                new CopyOnWriteArrayList<>();
        AtomicInteger attempts = new AtomicInteger();

        invoke(interceptor, redis.client, manualJob("context-run"),
                ignored -> {
                    contexts.add(ScheduledJobExecutionContext.current()
                            .orElseThrow());
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("planned retry");
                    }
                });

        assertEquals(2, contexts.size());
        assertEquals(List.of(1, 2),
                contexts.stream()
                        .map(ScheduledJobExecutionContext.Execution::attempt)
                        .toList());
        assertTrue(contexts.stream().allMatch(
                context -> "manualJob".equals(context.jobName())));
        assertTrue(contexts.stream().allMatch(
                context -> "context-run".equals(context.executionId())));
        assertTrue(contexts.stream().allMatch(
                context -> "MANUAL".equals(context.triggerType())));
        assertTrue(ScheduledJobExecutionContext.current().isEmpty(),
                "处理器退出后必须清理线程上下文");
        assertEquals(List.of("RETRY", "SUCCESS"),
                logs.stream().map(SysScheduledJobLog::getStatus).toList());
    }

    private void assertNextCycleIsNotExecutionLocked(
            ScheduledAnno scheduled, long firstCycleTime, long secondCycleTime) throws Exception {
        RedisFixture redis = new RedisFixture(firstCycleTime);
        ScheduledJobInterceptor interceptor = interceptor(redis);
        Job job = job("parallelJob", scheduled, new ContextEmpty());
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> first = pool.submit(() -> invoke(interceptor, redis.client, job, ignored -> {
            int execution = executions.incrementAndGet();
            if (execution == 1) {
                firstEntered.countDown();
                await(releaseFirst);
            } else {
                secondEntered.countDown();
            }
        }));
        try {
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            redis.time.set(secondCycleTime);
            invoke(interceptor, redis.client, job, ignored -> {
                executions.incrementAndGet();
                secondEntered.countDown();
            });
            assertTrue(secondEntered.await(1, TimeUnit.SECONDS),
                    "the next schedule cycle must not wait for the previous handler");
            assertEquals(2, executions.get());
        } finally {
            releaseFirst.countDown();
            first.get(2, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
    }

    /**
     * 使用默认系统任务配置构造拦截器。
     *
     * @param redis Redis 测试夹具
     * @return 定时任务拦截器
     */
    private static ScheduledJobInterceptor interceptor(
            RedisFixture redis) {
        return interceptor(redis, null);
    }

    /**
     * 使用默认系统任务配置和执行日志容器构造拦截器。
     *
     * @param redis Redis 测试夹具
     * @param logs 执行日志
     * @return 定时任务拦截器
     */
    private static ScheduledJobInterceptor interceptor(
            RedisFixture redis, List<SysScheduledJobLog> logs) {
        return interceptor(
                redis, logs,
                new SysScheduledJobConfig()
                        .setEnabled(true)
                        .setMaxRetries(0)
                        .setRetryIntervalMs(0L));
    }

    /**
     * 使用指定系统任务配置构造拦截器。
     *
     * @param redis Redis 测试夹具
     * @param logs 执行日志
     * @param config 系统任务配置
     * @return 定时任务拦截器
     */
    private static ScheduledJobInterceptor interceptor(
            RedisFixture redis, List<SysScheduledJobLog> logs,
            SysScheduledJobConfig config) {
        ScheduledJobConfigService configService = mock(ScheduledJobConfigService.class);
        SysScheduledJobLogMapper logMapper = mock(SysScheduledJobLogMapper.class);
        when(configService.getOrCreate(anyString(), anyBoolean()))
                .thenReturn(config);
        if (logs != null) {
            doAnswer(invocation -> {
                logs.add(invocation.getArgument(0));
                return 1;
            }).when(logMapper).save(any(SysScheduledJobLog.class));
        }
        return new ScheduledJobInterceptor(
                configService, logMapper, redis.scheduler);
    }

    /**
     * 使用可变持久化配置构造动态任务拦截器。
     *
     * @param redis Redis 测试夹具
     * @param logs 执行日志
     * @param currentConfig 当前持久化配置
     * @return 动态任务拦截器
     */
    private static ScheduledJobInterceptor dynamicInterceptor(
            RedisFixture redis, List<SysScheduledJobLog> logs,
            AtomicReference<SysScheduledJobConfig> currentConfig) {
        ScheduledJobConfigService configService =
                mock(ScheduledJobConfigService.class);
        SysScheduledJobLogMapper logMapper =
                mock(SysScheduledJobLogMapper.class);
        when(configService.requireDynamic(anyString()))
                .thenAnswer(ignored -> currentConfig.get());
        doAnswer(invocation -> {
            logs.add(invocation.getArgument(0));
            return 1;
        }).when(logMapper).save(any(SysScheduledJobLog.class));
        return new ScheduledJobInterceptor(
                configService, logMapper, redis.scheduler);
    }

    /** 构造允许重试的拦截器以验证不可重试异常。 */
    private static ScheduledJobInterceptor retryingInterceptor(
            RedisFixture redis, List<SysScheduledJobLog> logs) {
        ScheduledJobConfigService configService =
                mock(ScheduledJobConfigService.class);
        SysScheduledJobLogMapper logMapper =
                mock(SysScheduledJobLogMapper.class);
        when(configService.getOrCreate(anyString(), anyBoolean())).thenReturn(
                new SysScheduledJobConfig()
                        .setEnabled(true)
                        .setMaxRetries(3)
                        .setRetryIntervalMs(0L));
        doAnswer(invocation -> {
            logs.add(invocation.getArgument(0));
            return 1;
        }).when(logMapper).save(any(SysScheduledJobLog.class));
        return new ScheduledJobInterceptor(
                configService, logMapper, redis.scheduler);
    }

    private static Job manualJob(String runId) {
        Context context = new ContextEmpty();
        context.paramMap().put(ScheduledJobInterceptor.MANUAL_TRIGGER, "MANUAL");
        context.paramMap().put(ScheduledJobInterceptor.MANUAL_RUN_ID, runId);
        return job("manualJob", new ScheduledAnno().fixedDelay(1_000L), context);
    }

    /** 构造指定错过周期的恢复执行任务。 */
    private static Job recoveryJob(String jobName, long missedCycle) {
        return recoveryJob(
                jobName, missedCycle,
                new ScheduledAnno().fixedRate(1_000L));
    }

    /**
     * 构造指定调度定义与错过周期的恢复任务。
     *
     * @param jobName 任务名称
     * @param missedCycle 错过周期
     * @param scheduled 调度定义
     * @return 恢复任务
     */
    private static Job recoveryJob(
            String jobName, long missedCycle,
            ScheduledAnno scheduled) {
        Context context = new ContextEmpty();
        context.paramMap().put(ScheduledJobInterceptor.MANUAL_TRIGGER, "RECOVERY");
        context.paramMap().put(
                ScheduledJobInterceptor.RECOVERY_CYCLE, Long.toString(missedCycle));
        return job(jobName, scheduled, context);
    }

    /**
     * 构造动态普通调度任务。
     *
     * @param jobName 任务名称
     * @param config 创建 Job 时的动态任务配置
     * @return 动态任务
     */
    private static Job dynamicJob(
            String jobName, SysScheduledJobConfig config) {
        Context context = new ContextEmpty();
        context.paramMap().put(
                ScheduledJobInterceptor.DYNAMIC_SOURCE, "true");
        context.paramMap().put(
                ScheduledJobInterceptor.DYNAMIC_GENERATION,
                ScheduledJobInterceptor.dynamicGeneration(config));
        return job(
                jobName,
                new ScheduledAnno().fixedRate(60_000L),
                context);
    }

    /**
     * 构造指定处理器代际的动态配置。
     *
     * @param configId 配置主键
     * @param handlerKey 处理器标识
     * @return 动态任务配置
     */
    private static SysScheduledJobConfig dynamicConfig(
            long configId, String handlerKey) {
        return new SysScheduledJobConfig()
                .setConfigId(configId)
                .setJobSource("DYNAMIC")
                .setJobName("staleDynamicJob")
                .setHandlerKey(handlerKey)
                .setScheduleType("FIXED_RATE")
                .setScheduleExpression("60000")
                .setZone("")
                .setInitialDelayMs(0L)
                .setEnabled(true)
                .setConcurrentPolicy("ALLOW")
                .setMisfirePolicy("FIRE_ONCE")
                .setMaxRetries(0)
                .setRetryIntervalMs(0L);
    }

    /**
     * 构造用于验证稳定调度定义指纹的配置。
     *
     * @param handlerKey 处理器标识
     * @param scheduleType 调度类型
     * @param scheduleExpression 调度表达式
     * @param zone 时区
     * @param initialDelayMs 首次延迟
     * @return 任务配置
     */
    private static SysScheduledJobConfig definitionConfig(
            String handlerKey, String scheduleType,
            String scheduleExpression, String zone,
            long initialDelayMs) {
        return new SysScheduledJobConfig()
                .setHandlerKey(handlerKey)
                .setScheduleType(scheduleType)
                .setScheduleExpression(scheduleExpression)
                .setZone(zone)
                .setInitialDelayMs(initialDelayMs);
    }

    private static Job job(String name, ScheduledAnno scheduled, Context context) {
        Job job = mock(Job.class);
        when(job.getName()).thenReturn(name);
        when(job.getScheduled()).thenReturn(scheduled);
        when(job.getContext()).thenReturn(context);
        return job;
    }

    private static void invoke(ScheduledJobInterceptor interceptor, RedissonClient client,
                               Job job, JobHandler handler) {
        try {
            interceptor.doIntercept(client, job, handler);
        } catch (Throwable failure) {
            throw new AssertionError(failure);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for test coordination");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    /** 提供可控时间、锁、脚本与延迟任务的 Redis 测试夹具。 */
    private static final class RedisFixture {
        /** 当前模拟 Redis 时间。 */
        private final AtomicLong time;
        /** 普通字符串键值。 */
        private final Map<String, Object> values = new ConcurrentHashMap<>();
        /** Hash 类型键值。 */
        private final Map<String, Map<String, String>> hashes =
                new ConcurrentHashMap<>();
        /** Sorted Set 类型键值及分值。 */
        private final Map<String, Map<String, Long>> sortedSets =
                new ConcurrentHashMap<>();
        /** 按 Redis 键保存的锁代理。 */
        private final Map<String, FixtureLock> locks =
                new ConcurrentHashMap<>();
        /** 已登记的租约心跳任务。 */
        private final List<FixtureScheduledFuture> heartbeatTasks =
                new CopyOnWriteArrayList<>();
        /** 已登记的一次性租约接管任务。 */
        private final List<FixtureScheduledFuture> leaseRetryTasks =
                new CopyOnWriteArrayList<>();
        /** 一次性租约接管任务的延迟记录。 */
        private final List<Long> leaseRetryDelays =
                new CopyOnWriteArrayList<>();
        /** 最近一次恢复水位使用的过期时间。 */
        private final AtomicLong lastRecoveryWatermarkTtlMs =
                new AtomicLong(Long.MIN_VALUE);
        /** 完成脚本调用次数。 */
        private final AtomicInteger completionScriptCalls =
                new AtomicInteger();
        /** 完成脚本按计划失败时的同步信号。 */
        private final CountDownLatch completionFailureObserved =
                new CountDownLatch(1);
        /** Redisson 脚本代理。 */
        private final RScript script;
        /** Redisson 客户端代理。 */
        private final RedissonClient client;
        /** 心跳与租约接管使用的调度线程池代理。 */
        private final ScheduledExecutorService scheduler;
        /** 计划抛出异常的完成脚本调用序号。 */
        private volatile int failingCompletionScriptCall = -1;
        /** 完成脚本计划抛出的异常。 */
        private volatile RuntimeException completionScriptFailure;
        /** 解锁操作计划抛出的异常。 */
        private volatile RuntimeException unlockFailure;

        private RedisFixture(long initialTime) {
            time = new AtomicLong(initialTime);
            script = proxy(RScript.class, (method, args) ->
                    "eval".equals(method.getName())
                            ? evalScript(args)
                            : defaultValue(method.getReturnType()));
            client = proxy(RedissonClient.class, (method, args) -> switch (method.getName()) {
                case "getLock" -> locks.computeIfAbsent(
                        String.valueOf(args[0]), FixtureLock::new).proxy;
                case "getBucket" -> bucket(String.valueOf(args[0]));
                case "getScript" -> script;
                default -> defaultValue(method.getReturnType());
            });
            scheduler = proxy(
                    ScheduledExecutorService.class,
                    (method, args) -> switch (method.getName()) {
                        case "scheduleWithFixedDelay" -> {
                            FixtureScheduledFuture future =
                                    new FixtureScheduledFuture(
                                            (Runnable) args[0]);
                            heartbeatTasks.add(future);
                            yield future.proxy;
                        }
                        case "schedule" -> {
                            FixtureScheduledFuture future =
                                    new FixtureScheduledFuture(
                                            (Runnable) args[0]);
                            leaseRetryTasks.add(future);
                            leaseRetryDelays.add(
                                    ((Number) args[1]).longValue());
                            yield future.proxy;
                        }
                        case "isShutdown", "isTerminated" -> false;
                        default -> defaultValue(method.getReturnType());
                    });
        }

        /**
         * 指定第几次完成脚本调用抛出基础设施异常。
         *
         * @param call 完成脚本调用序号
         * @param failure 计划抛出的异常
         */
        private void failCompletionScript(
                int call, RuntimeException failure) {
            failingCompletionScriptCall = call;
            completionScriptFailure = failure;
        }

        /** 清理普通字符串键，模拟 anchor 或 waterline 丢失。 */
        private void clearMarkers() {
            values.clear();
        }

        /** 执行一次全部活跃租约心跳。 */
        private void runHeartbeats() {
            heartbeatTasks.forEach(FixtureScheduledFuture::runOnce);
        }

        /** 停止全部租约心跳，模拟 owner 进程崩溃。 */
        private void stopHeartbeats() {
            heartbeatTasks.forEach(FixtureScheduledFuture::cancel);
        }

        /** @return 当前登记的一次性租约接管任务数 */
        private int leaseRetryTaskCount() {
            return (int) leaseRetryTasks.stream()
                    .filter(task -> !task.cancelled)
                    .count();
        }

        /** 执行并结束当前登记的全部一次性租约接管任务。 */
        private void runLeaseRetries() {
            leaseRetryTasks.forEach(task -> {
                task.runOnce();
                task.cancel();
            });
        }

        /** @return 已登记租约重试的延迟快照 */
        private List<Long> leaseRetryDelays() {
            return List.copyOf(leaseRetryDelays);
        }

        /** @return 最近一次恢复水位使用的 TTL，零表示永久 */
        private long lastRecoveryWatermarkTtlMs() {
            return lastRecoveryWatermarkTtlMs.get();
        }

        /**
         * 读取指定周期状态。
         *
         * @param recovery 是否读取恢复执行状态
         * @param cycleId 周期标识
         * @return 周期状态
         */
        private String state(boolean recovery, long cycleId) {
            String marker = recovery ? ":recovery:states" : ":states";
            return hashes.entrySet().stream()
                    .filter(entry -> entry.getKey().endsWith(marker))
                    .filter(entry -> recovery
                            || !entry.getKey().endsWith(
                            ":recovery:states"))
                    .map(entry -> entry.getValue().get(
                            Long.toString(cycleId)))
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse(null);
        }

        /** 配置全部锁释放时抛出的异常。 */
        private void failUnlock(RuntimeException failure) {
            unlockFailure = failure;
        }

        /**
         * 模拟 Redisson Lua 脚本的原子状态变更。
         *
         * @param args RScript.eval 反射参数
         * @return 脚本返回值
         */
        private synchronized Object evalScript(Object[] args) {
            String source = String.valueOf(args[1]);
            @SuppressWarnings("unchecked")
            List<Object> keys = (List<Object>) args[3];
            Object[] argv = args.length > 4 && args[4] instanceof Object[] values
                    ? values : new Object[0];
            if (source.contains(
                    "local leaseUntil = now + tonumber(ARGV[3])")) {
                return renew(keys, argv);
            }
            if (source.contains(
                    "local entries = redis.call('ZRANGE'")) {
                return pendingRetryDelay(keys);
            }
            if (source.contains(
                    "return (t[1] * 1000) + math.floor")) {
                return time.get();
            }
            if (source.contains(
                    "local last = redis.call('GET', KEYS[4])")) {
                return claimScheduled(keys, argv);
            }
            if (source.contains(
                    "local completedAt = redis.call('ZSCORE'")) {
                return claimRecovery(keys, argv);
            }
            if (source.contains(
                    "local completed = redis.call('ZRANGEBYSCORE'")) {
                return findExpired(keys, argv);
            }
            if (source.contains(
                    "local score = redis.call('ZSCORE'")) {
                return reclaimExpired(keys, argv);
            }
            if (source.contains(
                    "local expected = 'PENDING|'")) {
                return complete(keys, argv);
            }
            throw new AssertionError("未模拟的 Redis Lua 脚本");
        }

        /**
         * 模拟 token 条件的 PENDING 租约心跳。
         *
         * @param keys Redis 键
         * @param argv 脚本参数
         * @return 新租约到期时间，token 失效时返回零
         */
        private Long renew(List<Object> keys, Object[] argv) {
            String cycle = String.valueOf(argv[0]);
            String token = String.valueOf(argv[1]);
            Map<String, String> states = hash(keys.get(1));
            String state = states.get(cycle);
            if (state == null
                    || !state.startsWith(
                    "PENDING|" + token + "|")) {
                return 0L;
            }
            long now = time.get();
            long leaseUntil = now + longValue(argv[2]);
            states.put(cycle, "PENDING|" + token + "|" + now
                    + "|" + leaseUntil);
            sortedSet(keys.get(0)).put(cycle, leaseUntil);
            return leaseUntil;
        }

        /**
         * 模拟读取当前 PENDING 租约的最短剩余时间。
         *
         * @param keys Redis 键
         * @return 剩余租约毫秒数
         */
        private Long pendingRetryDelay(List<Object> keys) {
            return sortedSet(keys.get(0)).entrySet().stream()
                    .filter(entry -> {
                        String state = hash(keys.get(1))
                                .get(entry.getKey());
                        return state != null
                                && state.startsWith("PENDING|");
                    })
                    .min(Comparator.comparingLong(Map.Entry::getValue))
                    .map(entry -> entry.getValue() <= time.get()
                            ? 1L : entry.getValue() - time.get() + 1L)
                    .orElse(0L);
        }

        /**
         * 模拟正常调度周期认领脚本。
         *
         * @param keys Redis 键
         * @param argv 脚本参数
         * @return 认领结果
         */
        private Long claimScheduled(
                List<Object> keys, Object[] argv) {
            long now = longValue(argv[0]);
            long interval = longValue(argv[1]);
            String mode = String.valueOf(argv[2]);
            String cycle = String.valueOf(argv[3]);
            Object last = values.get(String.valueOf(keys.get(3)));
            if ("CRON".equals(mode)) {
                if (last != null
                        && now < longValue(last)) {
                    return 0L;
                }
            } else if (last != null
                    && now - longValue(last) < interval) {
                return 0L;
            }
            String token = String.valueOf(argv[4]);
            long leaseUntil = longValue(argv[5]);
            sortedSet(keys.get(0)).put(cycle, leaseUntil);
            hash(keys.get(1)).put(
                    cycle, "PENDING|" + token + "|" + now
                            + "|" + leaseUntil);
            String anchor = String.valueOf(argv[8]);
            values.put(String.valueOf(keys.get(3)), anchor);
            values.put(String.valueOf(keys.get(4)), anchor);
            values.put(String.valueOf(keys.get(5)), anchor);
            return 1L;
        }

        /**
         * 模拟恢复周期首次认领或租约到期后的重新认领。
         *
         * @param keys Redis 键
         * @param argv 脚本参数
         * @return 认领结果
         */
        private Long claimRecovery(
                List<Object> keys, Object[] argv) {
            String cycle = String.valueOf(argv[0]);
            long now = longValue(argv[1]);
            Map<String, String> states = hash(keys.get(1));
            Map<String, Long> completed = sortedSet(keys.get(2));
            Long completedAt = completed.get(cycle);
            if (completedAt != null && completedAt <= now) {
                completed.remove(cycle);
                states.remove(cycle);
                completedAt = null;
            }
            if (completedAt != null) {
                return 0L;
            }
            String state = states.get(cycle);
            Long score = sortedSet(keys.get(0)).get(cycle);
            if (state != null && state.startsWith("PENDING|")
                    && score != null && score > now) {
                return 0L;
            }
            if (state != null && !state.startsWith("PENDING|")) {
                return 0L;
            }
            Object recoveryWatermark =
                    values.get(String.valueOf(keys.get(5)));
            if (state == null && recoveryWatermark != null
                    && longValue(recoveryWatermark) >= longValue(cycle)) {
                return 0L;
            }
            Object waterline = values.get(String.valueOf(keys.get(4)));
            if (state == null && waterline != null
                    && longValue(waterline) >= longValue(cycle)) {
                return 0L;
            }
            String token = String.valueOf(argv[2]);
            long leaseUntil = longValue(argv[3]);
            states.put(cycle, "PENDING|" + token + "|" + now
                    + "|" + leaseUntil);
            sortedSet(keys.get(0)).put(cycle, leaseUntil);
            values.put(String.valueOf(keys.get(3)),
                    Long.toString(now));
            values.put(String.valueOf(keys.get(4)),
                    Long.toString(now));
            values.put(String.valueOf(keys.get(5)),
                    String.valueOf(argv[6]));
            lastRecoveryWatermarkTtlMs.set(
                    longValue(argv[7]));
            values.put(String.valueOf(keys.get(6)),
                    Long.toString(now));
            return 1L;
        }

        /**
         * 模拟清理已完成状态并读取全部到期 PENDING 周期。
         *
         * @param keys Redis 键
         * @param argv 脚本参数
         * @return 到期周期
         */
        private List<String> findExpired(
                List<Object> keys, Object[] argv) {
            long now = longValue(argv[0]);
            Map<String, Long> completed = sortedSet(keys.get(2));
            List<String> expiredCompleted = completed.entrySet().stream()
                    .filter(entry -> entry.getValue() <= now)
                    .map(Map.Entry::getKey)
                    .toList();
            expiredCompleted.forEach(cycle -> {
                completed.remove(cycle);
                hash(keys.get(1)).remove(cycle);
            });
            return sortedSet(keys.get(0)).entrySet().stream()
                    .filter(entry -> entry.getValue() <= now)
                    .sorted(Comparator
                            .comparingLong(
                                    (Map.Entry<String, Long> entry) ->
                                            entry.getValue())
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey)
                    .toList();
        }

        /**
         * 模拟 owner 获得周期锁后的原子 token 接管。
         *
         * @param keys Redis 键
         * @param argv 脚本参数
         * @return 接管结果
         */
        private Long reclaimExpired(
                List<Object> keys, Object[] argv) {
            String cycle = String.valueOf(argv[0]);
            long now = longValue(argv[1]);
            Map<String, Long> pending = sortedSet(keys.get(0));
            Long score = pending.get(cycle);
            if (score == null || score > now) {
                return 0L;
            }
            Map<String, String> states = hash(keys.get(1));
            String state = states.get(cycle);
            if (state == null || !state.startsWith("PENDING|")) {
                return 0L;
            }
            String token = String.valueOf(argv[2]);
            long leaseUntil = longValue(argv[3]);
            states.put(cycle, "PENDING|" + token + "|" + now
                    + "|" + leaseUntil);
            pending.put(cycle, leaseUntil);
            return 1L;
        }

        /**
         * 模拟 token 条件完成脚本。
         *
         * @param keys Redis 键
         * @param argv 脚本参数
         * @return 完成结果
         */
        private Long complete(List<Object> keys, Object[] argv) {
            if (completionScriptCalls.incrementAndGet()
                    == failingCompletionScriptCall) {
                completionFailureObserved.countDown();
                throw completionScriptFailure;
            }
            String cycle = String.valueOf(argv[0]);
            String token = String.valueOf(argv[1]);
            Map<String, String> states = hash(keys.get(1));
            String state = states.get(cycle);
            if (state == null
                    || !state.startsWith(
                    "PENDING|" + token + "|")) {
                return 0L;
            }
            long completionTime = longValue(argv[2]);
            states.put(cycle, "COMPLETED|" + token
                    + "|" + completionTime);
            sortedSet(keys.get(0)).remove(cycle);
            sortedSet(keys.get(2)).put(
                    cycle, longValue(argv[3]));
            if ("FIXED_DELAY".equals(String.valueOf(argv[4]))) {
                values.put(String.valueOf(keys.get(3)),
                        Long.toString(completionTime));
                values.put(String.valueOf(keys.get(4)),
                        Long.toString(completionTime));
                values.put(String.valueOf(keys.get(5)),
                        Long.toString(completionTime));
            }
            return 1L;
        }

        /**
         * 获取哈希结构。
         *
         * @param key Redis 键
         * @return 哈希结构
         */
        private Map<String, String> hash(Object key) {
            return hashes.computeIfAbsent(
                    String.valueOf(key), ignored -> new HashMap<>());
        }

        /**
         * 获取有序集合结构。
         *
         * @param key Redis 键
         * @return 有序集合结构
         */
        private Map<String, Long> sortedSet(Object key) {
            return sortedSets.computeIfAbsent(
                    String.valueOf(key), ignored -> new HashMap<>());
        }

        /**
         * 将脚本参数转换为长整数。
         *
         * @param value 脚本参数
         * @return 长整数值
         */
        private static long longValue(Object value) {
            return value instanceof Number number
                    ? number.longValue()
                    : Long.parseLong(String.valueOf(value));
        }

        /**
         * 模拟 Redis 普通字符串 bucket。
         *
         * @param key Redis 键
         * @return bucket 代理
         */
        private RBucket<Object> bucket(String key) {
            return proxy(RBucket.class, (method, args) -> switch (method.getName()) {
                case "get" -> values.get(key);
                case "set" -> {
                    values.put(key, args[0]);
                    yield null;
                }
                case "setIfAbsent" -> values.putIfAbsent(key, args[0]) == null;
                case "getName" -> key;
                default -> defaultValue(method.getReturnType());
            });
        }

        /**
         * 可由测试主动触发的共享线程池心跳任务。
         */
        private static final class FixtureScheduledFuture {
            /** 受控执行的任务逻辑。 */
            private final Runnable task;
            /** 对外暴露的延迟任务代理。 */
            private final ScheduledFuture<?> proxy;
            /** 任务是否已经取消。 */
            private volatile boolean cancelled;

            /**
             * 创建可控心跳任务。
             *
             * @param task 心跳逻辑
             */
            private FixtureScheduledFuture(Runnable task) {
                this.task = task;
                this.proxy = proxy(
                        ScheduledFuture.class,
                        (method, args) -> switch (method.getName()) {
                            case "cancel" -> {
                                cancel();
                                yield true;
                            }
                            case "isCancelled", "isDone" -> cancelled;
                            case "getDelay" -> 0L;
                            case "compareTo" -> 0;
                            default ->
                                    defaultValue(method.getReturnType());
                        });
            }

            /** 在任务仍活跃时执行一次心跳。 */
            private void runOnce() {
                if (!cancelled) {
                    task.run();
                }
            }

            /** 标记心跳任务已停止。 */
            private void cancel() {
                cancelled = true;
            }
        }

        /**
         * 支持强制过期的 Redisson watchdog 锁模拟。
         */
        private final class FixtureLock {
            /** Redis 锁名。 */
            private final String name;
            /** 对外暴露的 Redisson 锁代理。 */
            private final RLock proxy;
            /** 当前锁持有线程。 */
            private Thread owner;
            /** 当前线程的可重入次数。 */
            private int holdCount;

            /**
             * 创建锁模拟。
             *
             * @param name Redis 锁名
             */
            private FixtureLock(String name) {
                this.name = name;
                this.proxy = proxy(RLock.class, this::invoke);
            }

            /**
             * 处理锁代理调用。
             *
             * @param method 调用方法
             * @param args 调用参数
             * @return 调用结果
             */
            private synchronized Object invoke(
                    Method method, Object[] args) {
                return switch (method.getName()) {
                    case "tryLock" -> tryLock();
                    case "isHeldByCurrentThread" ->
                            owner == Thread.currentThread();
                    case "unlock" -> {
                        unlock();
                        yield null;
                    }
                    case "getName" -> name;
                    default -> defaultValue(method.getReturnType());
                };
            }

            /** @return 当前线程是否成功取得锁 */
            private boolean tryLock() {
                Thread current = Thread.currentThread();
                if (owner == current) {
                    holdCount++;
                    return true;
                }
                if (owner != null) {
                    return false;
                }
                owner = current;
                holdCount = 1;
                return true;
            }

            /** 释放当前线程持有的一层锁。 */
            private void unlock() {
                if (unlockFailure != null) {
                    throw unlockFailure;
                }
                if (owner != Thread.currentThread()) {
                    throw new IllegalMonitorStateException(
                            "当前线程未持有锁 " + name);
                }
                holdCount--;
                if (holdCount == 0) {
                    owner = null;
                }
            }

            /** 模拟进程终止后 watchdog 停止且 Redis 锁到期。 */
            private synchronized void expire() {
                owner = null;
                holdCount = 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(Method method, Object[] args) throws Throwable;
    }
}
