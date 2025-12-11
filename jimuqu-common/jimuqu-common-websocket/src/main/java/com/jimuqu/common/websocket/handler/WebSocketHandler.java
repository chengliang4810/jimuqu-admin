package com.jimuqu.common.websocket.handler;

import cn.hutool.v7.core.util.ObjUtil;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.websocket.holder.WebSocketSessionHolder;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.core.util.MultiMap;
import org.noear.solon.net.websocket.WebSocket;
import org.noear.solon.net.websocket.WebSocketListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.jimuqu.common.satoken.utils.LoginHelper.LOGIN_USER_KEY;

@Slf4j
public class WebSocketHandler implements WebSocketListener {

    @Override
    public void onOpen(WebSocket socket) {
        Map<String, Object> attrMap = socket.attrMap();
        System.out.println("attrMap:" + attrMap);

        MultiMap<String> paramMap = socket.paramMap();
        System.out.println("paramMap:" + paramMap);

        LoginUser loginUser = socket.attr(LOGIN_USER_KEY);
        if (ObjUtil.isNull(loginUser)) {
            socket.close();
            log.info("[connect] invalid token received. sessionId: {}", socket.id());
            return;
        }
        WebSocketSessionHolder.addSession(loginUser.getUserId(), socket);
        log.info("[connect] sessionId: {},userId:{},userType:{}", socket.id(), loginUser.getUserId(), loginUser.getUserType());
    }

    @Override
    public void onMessage(WebSocket socket, String text) throws IOException {
        socket.send("我收到了：" + text);
    }

    @Override
    public void onMessage(WebSocket socket, ByteBuffer binary) throws IOException {
        log.info("二进制 websocket消息");
    }

    @Override
    public void onClose(WebSocket socket) {
        LoginUser loginUser = socket.attr(LOGIN_USER_KEY);
        if (ObjUtil.isNull(loginUser)) {
            log.info("[disconnect] invalid token received. sessionId: {}", socket.id());
            return;
        }
        WebSocketSessionHolder.removeSession(loginUser.getUserId());
        log.info("[disconnect] sessionId: {},userId:{},userType:{}", socket.id(), loginUser.getUserId(), loginUser.getUserType());
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        log.error("[error] sessionId: {} , exception:{}", socket.id(), error.getMessage());
    }

}
