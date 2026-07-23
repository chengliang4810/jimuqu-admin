package com.jimuqu.system.service;

import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.scheduling.ScheduledAnno;
import org.noear.solon.scheduling.scheduled.Job;
import org.noear.solon.scheduling.scheduled.JobHandler;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
        ScheduledJobInterceptor interceptor = interceptor();
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
    }

    @Test
    void nextCronCycleCanRunWhileThePreviousCycleIsStillRunning() throws Exception {
        assertNextCycleIsNotExecutionLocked(
                new ScheduledAnno().cron("0/1 * * * * ? *"), 1_100L, 2_000L);
    }

    @Test
    void nextFixedRateCycleCanRunWhileThePreviousCycleIsStillRunning() throws Exception {
        assertNextCycleIsNotExecutionLocked(
                new ScheduledAnno().fixedRate(1_000L), 1_100L, 2_000L);
    }

    @Test
    void fixedDelayKeepsTheExecutionLockAndStartsDelayAtCompletion() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        ScheduledJobInterceptor interceptor = interceptor();
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
        ScheduledJobInterceptor interceptor = interceptor(logs);
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

    @Test
    void fixedDelayCompletionMarkerFailureKeepsLockUntilDelayEnds() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        redis.failBucketSet(2, new IllegalStateException("completion marker unavailable"));
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(logs);
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
            assertTrue(redis.bucketSetFailureObserved.await(2, TimeUnit.SECONDS));
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
            assertEquals(1L, logs.stream().filter(log -> "SKIPPED".equals(log.getStatus())).count());
            assertEquals(1L, logs.stream().filter(log -> "SUCCESS".equals(log.getStatus())).count());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void fixedDelayFallbackWaitIgnoresInterruptAndRestoresIt() throws Exception {
        RedisFixture redis = new RedisFixture(1_000L);
        redis.failBucketSet(2, new IllegalStateException("completion marker unavailable"));
        ScheduledJobInterceptor interceptor = interceptor();
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
        assertTrue(redis.bucketSetFailureObserved.await(2, TimeUnit.SECONDS));
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
        redis.failBucketSet(2, new IllegalStateException("completion marker unavailable"));
        List<SysScheduledJobLog> logs = new CopyOnWriteArrayList<>();
        ScheduledJobInterceptor interceptor = interceptor(logs);
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
        ScheduledJobInterceptor interceptor = interceptor();

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
        ScheduledJobInterceptor interceptor = new ScheduledJobInterceptor(configService, logMapper);
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

    private void assertNextCycleIsNotExecutionLocked(
            ScheduledAnno scheduled, long firstCycleTime, long secondCycleTime) throws Exception {
        RedisFixture redis = new RedisFixture(firstCycleTime);
        ScheduledJobInterceptor interceptor = interceptor();
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

    private static ScheduledJobInterceptor interceptor() {
        return interceptor(null);
    }

    private static ScheduledJobInterceptor interceptor(List<SysScheduledJobLog> logs) {
        ScheduledJobConfigService configService = mock(ScheduledJobConfigService.class);
        SysScheduledJobLogMapper logMapper = mock(SysScheduledJobLogMapper.class);
        when(configService.getOrCreate(anyString(), anyBoolean())).thenAnswer(ignored ->
                new SysScheduledJobConfig()
                        .setEnabled(true)
                        .setMaxRetries(0)
                        .setRetryIntervalMs(0L));
        if (logs != null) {
            doAnswer(invocation -> {
                logs.add(invocation.getArgument(0));
                return 1;
            }).when(logMapper).save(any(SysScheduledJobLog.class));
        }
        return new ScheduledJobInterceptor(configService, logMapper);
    }

    private static Job manualJob(String runId) {
        Context context = new ContextEmpty();
        context.paramMap().put(ScheduledJobInterceptor.MANUAL_TRIGGER, "MANUAL");
        context.paramMap().put(ScheduledJobInterceptor.MANUAL_RUN_ID, runId);
        return job("manualJob", new ScheduledAnno().fixedDelay(1_000L), context);
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

    private static final class RedisFixture {
        private final AtomicLong time;
        private final Map<String, Object> values = new ConcurrentHashMap<>();
        private final Map<String, RLock> locks = new ConcurrentHashMap<>();
        private final AtomicInteger bucketSetCalls = new AtomicInteger();
        private final CountDownLatch bucketSetFailureObserved = new CountDownLatch(1);
        private final RScript script;
        private final RedissonClient client;
        private volatile int failingBucketSetCall = -1;
        private volatile RuntimeException bucketSetFailure;
        private volatile RuntimeException unlockFailure;

        private RedisFixture(long initialTime) {
            time = new AtomicLong(initialTime);
            script = proxy(RScript.class, (method, args) ->
                    "eval".equals(method.getName()) ? time.get() : defaultValue(method.getReturnType()));
            client = proxy(RedissonClient.class, (method, args) -> switch (method.getName()) {
                case "getLock" -> locks.computeIfAbsent(String.valueOf(args[0]), this::lock);
                case "getBucket" -> bucket(String.valueOf(args[0]));
                case "getScript" -> script;
                default -> defaultValue(method.getReturnType());
            });
        }

        private void failBucketSet(int call, RuntimeException failure) {
            failingBucketSetCall = call;
            bucketSetFailure = failure;
        }

        private void clearMarkers() {
            values.clear();
        }

        private void failUnlock(RuntimeException failure) {
            unlockFailure = failure;
        }

        private RLock lock(String name) {
            ReentrantLock lock = new ReentrantLock();
            return proxy(RLock.class, (method, args) -> switch (method.getName()) {
                case "tryLock" -> lock.tryLock();
                case "isHeldByCurrentThread" -> lock.isHeldByCurrentThread();
                case "unlock" -> {
                    if (unlockFailure != null) {
                        throw unlockFailure;
                    }
                    lock.unlock();
                    yield null;
                }
                case "getName" -> name;
                default -> defaultValue(method.getReturnType());
            });
        }

        private RBucket<Object> bucket(String key) {
            return proxy(RBucket.class, (method, args) -> switch (method.getName()) {
                case "get" -> values.get(key);
                case "set" -> {
                    if (bucketSetCalls.incrementAndGet() == failingBucketSetCall) {
                        bucketSetFailureObserved.countDown();
                        throw bucketSetFailure;
                    }
                    values.put(key, args[0]);
                    yield null;
                }
                case "setIfAbsent" -> values.putIfAbsent(key, args[0]) == null;
                case "getName" -> key;
                default -> defaultValue(method.getReturnType());
            });
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
