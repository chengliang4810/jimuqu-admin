package com.jimuqu.common.sse.config;

import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;

/**
 * SSE 自动装配
 *
 */
@Configuration
@Condition(onExpression="${sse.enabled:false} == true")
public class SseAutoConfig {

//    @Bean
//    public SseEmitterManager sseEmitterManager() {
//        return new SseEmitterManager();
//    }
//
//    @Bean
//    public SseTopicListener sseTopicListener() {
//        return new SseTopicListener();
//    }
//
//    @Bean
//    public SseController sseController(SseEmitterManager sseEmitterManager) {
//        return new SseController(sseEmitterManager);
//    }

}
