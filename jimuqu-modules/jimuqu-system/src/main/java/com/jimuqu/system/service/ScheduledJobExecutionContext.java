package com.jimuqu.system.service;

import java.util.Optional;

/**
 * 当前定时任务业务执行的只读上下文。
 *
 * <p>上下文只在任务处理器实际执行期间可用。一次触发及其重试共享同一个
 * {@code executionId}，业务可以把它作为幂等请求号或审计关联标识。</p>
 */
public final class ScheduledJobExecutionContext {

    /**
     * 当前线程绑定的任务执行信息。
     */
    private static final ThreadLocal<Execution> CURRENT = new ThreadLocal<>();

    /**
     * 工具类不允许实例化。
     */
    private ScheduledJobExecutionContext() {
    }

    /**
     * 获取当前任务执行信息。
     *
     * @return 处于任务处理器内时返回执行信息，否则返回空
     */
    public static Optional<Execution> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * 在当前线程绑定一次任务执行信息。
     *
     * @param jobName 任务名称
     * @param executionId 执行链标识
     * @param triggerType 触发类型
     * @param attempt 当前尝试次数
     * @return 用于恢复原线程上下文的作用域
     */
    static Scope open(
            String jobName, String executionId, String triggerType,
            int attempt) {
        Execution previous = CURRENT.get();
        CURRENT.set(new Execution(
                jobName, executionId, triggerType, attempt));
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    /**
     * 单次任务处理器调用的不可变执行信息。
     *
     * @param jobName 任务名称
     * @param executionId 一次触发及其重试共享的执行链标识
     * @param triggerType 触发类型：SCHEDULED、MANUAL 或 RECOVERY
     * @param attempt 当前尝试次数，从 1 开始
     */
    public record Execution(
            String jobName, String executionId, String triggerType,
            int attempt) {
    }

    /**
     * 线程上下文绑定作用域。
     */
    @FunctionalInterface
    interface Scope extends AutoCloseable {

        /**
         * 恢复进入作用域前的线程上下文。
         */
        @Override
        void close();
    }
}
