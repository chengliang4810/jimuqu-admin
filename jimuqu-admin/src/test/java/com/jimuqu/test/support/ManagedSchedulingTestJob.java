package com.jimuqu.test.support;

import org.noear.solon.annotation.Component;
import org.noear.solon.scheduling.annotation.Scheduled;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定时任务管理 HTTP 契约的测试任务。
 */
@Component
public class ManagedSchedulingTestJob {

    public static final String JOB_NAME = "httpContractJob";
    public static final String CRON_JOB_NAME = "cronContractJob";
    public static final String RECONCILE_JOB_NAME = "reconcileContractJob";
    public static final long RECONCILE_FIXED_DELAY_MS = 250L;
    private static final AtomicInteger EXECUTIONS = new AtomicInteger();
    private static final AtomicInteger CRON_EXECUTIONS = new AtomicInteger();
    private static final AtomicInteger RECONCILE_EXECUTIONS = new AtomicInteger();
    private static final AtomicInteger MODE_ATTEMPTS = new AtomicInteger();
    private static volatile Mode mode = Mode.SUCCESS;
    private static volatile CountDownLatch entered = new CountDownLatch(0);
    private static volatile CountDownLatch release = new CountDownLatch(0);

    @Scheduled(name = JOB_NAME, fixedDelay = 1000L, initialDelay = 3_600_000L, enable = false)
    public void execute() throws InterruptedException {
        EXECUTIONS.incrementAndGet();
        int attempt = MODE_ATTEMPTS.incrementAndGet();
        if (mode == Mode.FAIL_ONCE && attempt == 1) {
            throw new IllegalStateException("planned first attempt failure");
        }
        if (mode == Mode.ALWAYS_FAIL) {
            throw new IllegalStateException("planned final failure");
        }
        if (mode == Mode.BLOCKING) {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("blocking test task timed out");
            }
        }
    }

    @Scheduled(name = CRON_JOB_NAME, cron = "0 0 0 1 1 ? *", enable = false)
    public void executeCron() {
        CRON_EXECUTIONS.incrementAndGet();
    }

    @Scheduled(name = RECONCILE_JOB_NAME, fixedDelay = RECONCILE_FIXED_DELAY_MS,
            initialDelay = 25L, enable = false)
    public void executeAfterReconciliation() {
        RECONCILE_EXECUTIONS.incrementAndGet();
    }

    public static int executions() {
        return EXECUTIONS.get();
    }

    public static int cronExecutions() {
        return CRON_EXECUTIONS.get();
    }

    public static int reconcileExecutions() {
        return RECONCILE_EXECUTIONS.get();
    }

    public static void mode(Mode nextMode) {
        mode = nextMode;
        MODE_ATTEMPTS.set(0);
        entered = nextMode == Mode.BLOCKING ? new CountDownLatch(1) : new CountDownLatch(0);
        release = nextMode == Mode.BLOCKING ? new CountDownLatch(1) : new CountDownLatch(0);
    }

    public static boolean awaitEntered() throws InterruptedException {
        return entered.await(5, TimeUnit.SECONDS);
    }

    public static void release() {
        release.countDown();
    }

    public enum Mode {
        SUCCESS,
        FAIL_ONCE,
        ALWAYS_FAIL,
        BLOCKING
    }
}
