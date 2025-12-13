package com.jimuqu.common.sse.config;

import com.jimuqu.common.sse.controller.SseController;
import com.jimuqu.common.sse.core.SseEmitterManager;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.RunUtil;
import org.noear.solon.web.sse.SseEvent;

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
    public SseEmitterManager sseEmitterManager() {
        return new SseEmitterManager();
    }

    /**
     * 注册 SSE 控制器
     * @param sseProperties SSE 配置项
     */
    @Bean
    public void registerSseController(@Inject SseProperties sseProperties) {
        Solon.app().router().add(sseProperties.getPath(), SseController.class);
    }

    @Bean
    public void registerSseHeader(@Inject SseEmitterManager sseEmitterManager){
        RunUtil.scheduleWithFixedDelay(() -> {
            sseEmitterManager.sendEvent(new SseEvent().name("ping"));
        }, 3000, 10000);
    }

}
