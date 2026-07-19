package com.jimuqu.common.websocket.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.util.ObjUtil;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.common.satoken.utils.ClientAccessValidator;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.sse.domain.SseMessagePayload;
import com.jimuqu.common.sse.dto.SseMessageDto;
import com.jimuqu.common.websocket.holder.WebSocketSessionHolder;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.net.websocket.WebSocket;
import org.noear.solon.net.websocket.WebSocketListener;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;

import static com.jimuqu.common.satoken.utils.LoginHelper.LOGIN_USER_KEY;

@Slf4j
public class WebSocketHandler implements WebSocketListener {

    private static final String LOGIN_TOKEN_KEY = "loginToken";
    private static final String MESSAGE_TOPIC = "global:message";
    private static final int BAD_DATA_CLOSE_CODE = 1007;
    private static final int SERVER_ERROR_CLOSE_CODE = 1011;
    private static final List<String> CLIENT_IP_HEADERS = List.of(
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR");

    @Override
    public void onOpen(WebSocket socket) {

        String token = socket.paramOrDefault("Authorization", "").replace("Bearer ", "");
        // 获取登录用户信息
        LoginUser loginUser;
        try {
            if (!StpUtil.stpLogic.isValidToken(token)) {
                socket.close(BAD_DATA_CLOSE_CODE, "invalid token");
                log.info("[connect] invalid token received. sessionId: {}", socket.id());
                return;
            }
            ClientAccessValidator.validateToken(token, socket.param(LoginHelper.CLIENT_KEY),
                    socket.path(), realIp(socket));
            loginUser = LoginHelper.getLoginUser(token);
        } catch (Exception e) {
            socket.close(BAD_DATA_CLOSE_CODE, "invalid token");
            log.warn("WebSocket 认证失败,无法访问系统资源. sessionId: {}, exception: {}",
                    socket.id(), e.getClass().getSimpleName());
            return;
        }

        if (ObjUtil.isNull(loginUser)) {
            socket.close(BAD_DATA_CLOSE_CODE, "invalid token");
            log.info("[connect] invalid token received. sessionId: {}", socket.id());
            return;
        }

        socket.attr(LOGIN_USER_KEY, loginUser);
        socket.attr(LOGIN_TOKEN_KEY, token);
        WebSocketSessionHolder.addSession(loginUser.getUserId(), token, socket);
        log.info("[connect] sessionId: {},userId:{},userType:{}", socket.id(), loginUser.getUserId(), loginUser.getUserType());
    }

    @Override
    public void onMessage(WebSocket socket, String text) throws IOException {
        // 心跳
        if ("ping".equals(text) || isJsonPing(text)) {
            socket.send("{\"type\":\"pong\"}");
            return;
        }

        Long userId = ((LoginUser) socket.attr(LOGIN_USER_KEY)).getUserId();
        SseMessagePayload payload = SseMessagePayload.message(text)
                .setType("custom")
                .setSource("client");
        SseMessageDto message = new SseMessageDto();
        message.setUserIds(List.of(userId));
        message.setMessage(JsonUtil.toString(payload));
        RedisUtils.publish(MESSAGE_TOPIC, message);
    }

    private boolean isJsonPing(String text) {
        try {
            return "ping".equals(JsonUtil.toMap(text).getStr("type"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String realIp(WebSocket socket) {
        for (String headerName : CLIENT_IP_HEADERS) {
            String forwarded = handshakeHeader(socket, headerName);
            if (!isUnknown(forwarded)) {
                return stripIpv6Brackets(firstKnownProxyIp(forwarded));
            }
        }
        String remoteIp = socket.remoteAddress() == null || socket.remoteAddress().getAddress() == null
                ? null : socket.remoteAddress().getAddress().getHostAddress();
        return stripIpv6Brackets(firstKnownProxyIp(remoteIp));
    }

    /** Solon 将查询参数与握手 Header 合并保存，Header 位于同名查询参数之后。 */
    private String handshakeHeader(WebSocket socket, String name) {
        List<String> values = socket.paramMap().getAll(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        int queryValueCount = queryValueCount(socket.url(), name);
        return values.size() > queryValueCount ? values.get(queryValueCount) : null;
    }

    private int queryValueCount(String url, String name) {
        String query = URI.create(url).getQuery();
        if (query == null || query.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String item : query.split("&")) {
            int separator = item.indexOf('=');
            String key = separator < 0 ? item : item.substring(0, separator);
            if (name.equalsIgnoreCase(key)) {
                count++;
            }
        }
        return count;
    }

    private String firstKnownProxyIp(String value) {
        if (value == null || !value.contains(",")) {
            return value == null ? null : value.trim();
        }
        for (String candidate : value.split(",")) {
            if (!isUnknown(candidate)) {
                return candidate.trim();
            }
        }
        return value.trim();
    }

    private boolean isUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value.trim());
    }

    private String stripIpv6Brackets(String value) {
        if (value != null && value.length() > 1 && value.startsWith("[") && value.endsWith("]")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    @Override
    public void onMessage(WebSocket socket, ByteBuffer binary) throws IOException {
        log.info("二进制 websocket消息 根据业务自行实现");
    }

    @Override
    public void onClose(WebSocket socket) {
        LoginUser loginUser = socket.attr(LOGIN_USER_KEY);
        if (ObjUtil.isNull(loginUser)) {
            log.info("[disconnect] invalid token received. sessionId: {}", socket.id());
            return;
        }
        WebSocketSessionHolder.removeSession(loginUser.getUserId(), socket.attr(LOGIN_TOKEN_KEY), socket);
        log.info("[disconnect] sessionId: {},userId:{},userType:{}", socket.id(), loginUser.getUserId(), loginUser.getUserType());
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        log.error("[error] sessionId: {} , exception:{}", socket.id(), error.getMessage());
        LoginUser loginUser = socket.attr(LOGIN_USER_KEY);
        if (loginUser != null) {
            WebSocketSessionHolder.removeSession(loginUser.getUserId(), socket.attr(LOGIN_TOKEN_KEY), socket);
        }
        socket.close(SERVER_ERROR_CLOSE_CODE, "server error");
    }

}
