package com.jimuqu.common.websocket.config;

import com.jimuqu.common.websocket.handler.WebSocketHandler;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.net.websocket.WebSocketRouter;

@Configuration
@Condition(onExpression = "${websocket.enabled:false} == true")
public class WebSocketConfig {

    @Bean
    public void webSocketHandler() {
        Solon.app().enableWebSocket(true);
        String websocketPath = Solon.cfg().getProperty("websocket.path", "/websocket");
        WebSocketRouter.getInstance().of(websocketPath, new WebSocketHandler());
    }

}
