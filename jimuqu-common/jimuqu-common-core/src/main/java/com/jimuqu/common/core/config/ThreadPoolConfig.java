package com.jimuqu.common.core.config;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.core.util.RunUtil;

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
    @Bean
    public ExecutorService executorService() {
        return RunUtil.io();
    }

    /**
     * 执行周期性或定时任务，使用定时线程池
     */
    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return RunUtil.timer();
    }

}
