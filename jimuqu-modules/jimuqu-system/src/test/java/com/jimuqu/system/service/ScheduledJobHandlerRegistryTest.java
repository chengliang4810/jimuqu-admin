package com.jimuqu.system.service;

import com.jimuqu.system.task.ScheduledJobHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在线定时任务处理器白名单签名单元测试。
 */
class ScheduledJobHandlerRegistryTest {

    /** 验证同步 void 无参实例方法可以进入白名单。 */
    @Test
    void acceptsPublicVoidHandler() throws Exception {
        Method method = HandlerSamples.class.getMethod("valid");

        assertDoesNotThrow(() -> ScheduledJobHandlerRegistry.validateMethod(
                method, method.getAnnotation(ScheduledJobHandler.class)));
    }

    /** 验证异步返回方法不会被提前记录为执行成功。 */
    @Test
    void rejectsAsynchronousReturnType() throws Exception {
        Method method = HandlerSamples.class.getMethod("asynchronous");

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> ScheduledJobHandlerRegistry.validateMethod(
                        method,
                        method.getAnnotation(ScheduledJobHandler.class)));

        assertTrue(failure.getMessage().contains("返回 void"));
    }

    /**
     * 白名单方法签名样例。
     */
    public static class HandlerSamples {

        /** 合法同步处理器。 */
        @ScheduledJobHandler(
                key = "test.valid",
                description = "合法同步处理器")
        public void valid() {
        }

        /** 不允许提前结束的异步处理器。 */
        @ScheduledJobHandler(
                key = "test.asynchronous",
                description = "非法异步处理器")
        public CompletableFuture<Void> asynchronous() {
            return CompletableFuture.completedFuture(null);
        }
    }
}
