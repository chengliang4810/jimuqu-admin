package com.jimuqu.common.core.config;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池配置
 * 容器内注册 executorService、scheduledExecutorService
 * @author chengliang
 * @date 2025/12/12
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 异步执行器
     * JDK21自动开启虚拟线程
     * @return 线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService executorService() {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("jimuqu-async-", 0)
                .factory();
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

    /**
     * 执行周期性或定时任务，使用定时线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors() + 1,
                Thread.ofPlatform().daemon(true).name("jimuqu-schedule-", 0).factory());
    }

}
