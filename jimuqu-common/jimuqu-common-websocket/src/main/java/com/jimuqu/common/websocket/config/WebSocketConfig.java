package com.jimuqu.common.websocket.config;

import com.jimuqu.common.websocket.handler.WebSocketHandler;
import com.jimuqu.common.websocket.holder.WebSocketSessionHolder;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.net.websocket.WebSocketRouter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@Condition(onExpression = "${websocket.enabled:false} == true")
public class WebSocketConfig {

    @Bean
    public void webSocketHandler(@Inject ScheduledExecutorService scheduledExecutorService) {
        Solon.app().enableWebSocket(true);
        String websocketPath = Solon.cfg().getProperty("websocket.path", "/websocket");
        WebSocketRouter.getInstance().of(websocketPath, new WebSocketHandler());
        long heartbeatInterval = Math.max(1000L,
                Solon.cfg().getLong("websocket.heartbeatInterval", 60000L));
        scheduledExecutorService.scheduleWithFixedDelay(WebSocketSessionHolder::sessionMonitor,
                heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

}
