package com.jimuqu.support;

import com.jimuqu.system.task.ScheduledJobHandler;
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

    /**
     * 首次失败后重试成功的测试任务标识。
     */
    public static final String RETRY_SUCCESS_JOB = "fullStackRetrySuccessJob";

    /**
     * 始终失败的测试任务标识。
     */
    public static final String ALWAYS_FAIL_JOB = "fullStackAlwaysFailJob";

    /**
     * 慢任务标识。
     */
    public static final String SLOW_JOB = "fullStackSlowJob";

    /**
     * 慢任务白名单处理器标识。
     */
    public static final String SLOW_HANDLER_KEY = "test.fullStack.slow";

    /**
     * 避免测试任务被调度器自动触发的远期 Cron 表达式。
     */
    private static final String NEVER_AUTOMATICALLY_RUN = "0 0 0 1 1 ? *";

    /**
     * 重试成功任务的累计执行次数。
     */
    private static final AtomicInteger RETRY_SUCCESS_ATTEMPTS = new AtomicInteger();

    /**
     * 首次执行失败，下一次重试成功。
     */
    @Scheduled(name = RETRY_SUCCESS_JOB, cron = NEVER_AUTOMATICALLY_RUN, enable = false)
    public void failOnceThenSucceed() {
        if ((RETRY_SUCCESS_ATTEMPTS.incrementAndGet() & 1) == 1) {
            throw new IllegalStateException("full-stack planned first attempt failure");
        }
    }

    /**
     * 每次执行都返回计划内失败。
     */
    @Scheduled(name = ALWAYS_FAIL_JOB, cron = NEVER_AUTOMATICALLY_RUN, enable = false)
    public void alwaysFail() {
        throw new IllegalStateException("full-stack planned final failure");
    }

    /**
     * 执行五秒，用于验证禁止并发时的跳过记录。
     *
     * @throws InterruptedException 当前线程被中断
     */
    @ScheduledJobHandler(key = SLOW_HANDLER_KEY, description = "完整验收慢任务")
    @Scheduled(name = SLOW_JOB, cron = NEVER_AUTOMATICALLY_RUN, enable = false)
    public void runSlowly() throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
    }
}
