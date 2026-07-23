package com.jimuqu.support;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Condition;
import org.noear.solon.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 仅在完整全栈验收中启用的定时任务状态探针。
 */
@Component
@Condition(onExpression = "${jimuqu.test.scheduledJobProbeEnabled:false} == true")
public class FullStackScheduledJobProbe {

    public static final String RETRY_SUCCESS_JOB = "fullStackRetrySuccessJob";
    public static final String ALWAYS_FAIL_JOB = "fullStackAlwaysFailJob";
    public static final String SLOW_JOB = "fullStackSlowJob";
    private static final String NEVER_AUTOMATICALLY_RUN = "0 0 0 1 1 ? *";
    private static final AtomicInteger RETRY_SUCCESS_ATTEMPTS = new AtomicInteger();

    @Scheduled(name = RETRY_SUCCESS_JOB, cron = NEVER_AUTOMATICALLY_RUN, enable = false)
    public void failOnceThenSucceed() {
        if ((RETRY_SUCCESS_ATTEMPTS.incrementAndGet() & 1) == 1) {
            throw new IllegalStateException("full-stack planned first attempt failure");
        }
    }

    @Scheduled(name = ALWAYS_FAIL_JOB, cron = NEVER_AUTOMATICALLY_RUN, enable = false)
    public void alwaysFail() {
        throw new IllegalStateException("full-stack planned final failure");
    }

    @Scheduled(name = SLOW_JOB, cron = NEVER_AUTOMATICALLY_RUN, enable = false)
    public void runSlowly() throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
    }
}
