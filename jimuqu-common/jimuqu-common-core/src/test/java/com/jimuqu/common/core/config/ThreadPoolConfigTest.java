package com.jimuqu.common.core.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线程池配置契约测试。
 */
class ThreadPoolConfigTest {

    /**
     * 验证异步执行器使用具名虚拟线程。
     */
    @Test
    void shouldUseNamedVirtualThreadsForAsyncTasks() throws Exception {
        ThreadPoolConfig config = new ThreadPoolConfig();
        try (ExecutorService executorService = config.executorService()) {
            Thread thread = executorService.submit(Thread::currentThread).get(5, TimeUnit.SECONDS);

            assertTrue(thread.isVirtual());
            assertTrue(thread.getName().startsWith("jimuqu-async-"));
        }
    }

    /**
     * 验证定时执行器继续使用具名平台线程。
     */
    @Test
    void shouldUseNamedPlatformThreadsForScheduledTasks() throws Exception {
        ThreadPoolConfig config = new ThreadPoolConfig();
        try (ScheduledExecutorService executorService = config.scheduledExecutorService()) {
            Thread thread = executorService.schedule(Thread::currentThread, 0, TimeUnit.MILLISECONDS)
                    .get(5, TimeUnit.SECONDS);

            assertFalse(thread.isVirtual());
            assertTrue(thread.getName().startsWith("jimuqu-schedule-"));
        }
    }
}
