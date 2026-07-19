package com.jimuqu.common.sse.config;

import com.jimuqu.common.sse.controller.SseController;
import com.jimuqu.common.sse.core.SseEmitterManager;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.web.sse.SseEvent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE 自动装配
 *
 * @author chengliang
 * @date 2025/12/11
 */
@Configuration
@Condition(onExpression = "${sse.enabled:false} == true")
public class SseAutoConfig {

    /**
     * 创建 SSE emitters 管理器
     * @return SSE emitters 管理器
     */
    @Bean
    public SseEmitterManager sseEmitterManager(@Inject ScheduledExecutorService scheduledExecutorService) {
        return new SseEmitterManager(scheduledExecutorService);
    }

    /**
     * 注册 SSE 控制器
     * @param sseProperties SSE 配置项
     */
    @Bean
    public void registerSseController(@Inject SseProperties sseProperties) {
        Solon.app().router().add(sseProperties.getPath(), SseController.class);
    }

    /**
     * 注册 sse 心跳
     * @param sseEmitterManager SSE emitters 管理器
     */
    @Bean
    @Condition(onExpression = "${sse.heartbeat:true} == true")
    public void registerSseHeartbeat(@Inject SseEmitterManager sseEmitterManager,
                                     @Inject ScheduledExecutorService scheduledExecutorService) {
        long heartbeatInterval = Solon.cfg().getLong("sse.heartbeatInterval", 60000L);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            sseEmitterManager.sendEvent(new SseEvent().comment("heartbeat"));
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

}
