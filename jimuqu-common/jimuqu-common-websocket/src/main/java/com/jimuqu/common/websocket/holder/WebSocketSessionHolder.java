package com.jimuqu.common.websocket.holder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.noear.solon.net.websocket.WebSocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 用于保存当前所有在线的会话信息
 *
 * @author chengliang
 * @date 2025/12/11
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebSocketSessionHolder {

    private static final Map<Long, Map<String, WebSocket>> USER_SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 将WebSocket会话添加到用户会话Map中
     *
     * @param sessionKey 会话键，用于检索会话
     * @param session    要添加的WebSocket会话
     */
    public static void addSession(Long userId, String token, WebSocket session) {
        USER_SESSION_MAP.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(token, session);
    }

    /**
     * 移除用户指定 token 对应的 WebSocket 会话
     *
     * @param userId 用户 ID
     * @param token 登录 token
     */
    public static void removeSession(Long userId, String token) {
        Map<String, WebSocket> sessions = USER_SESSION_MAP.get(userId);
        if (sessions != null) {
            sessions.remove(token);
            if (sessions.isEmpty()) {
                USER_SESSION_MAP.remove(userId);
            }
        }
    }

    /**
     * 获取用户的全部 WebSocket 会话
     *
     * @param userId 用户 ID
     * @return token 与会话的映射
     */
    public static Map<String, WebSocket> getSessions(Long userId) {
        return USER_SESSION_MAP.getOrDefault(userId, Map.of());
    }

    /**
     * 获取存储在用户会话Map中所有WebSocket会话的会话键集合
     *
     * @return 所有WebSocket会话的会话键集合
     */
    public static Set<Long> getSessionsAll() {
        return USER_SESSION_MAP.keySet();
    }

    /**
     * 检查给定的会话键是否存在于用户会话Map中
     *
     * @param sessionKey 要检查的会话键
     * @return 如果存在对应的会话键，则返回true；否则返回false
     */
    public static Boolean existSession(Long sessionKey) {
        return USER_SESSION_MAP.containsKey(sessionKey);
    }

    public static void sendAll(String message) {
        USER_SESSION_MAP.values().stream()
                .flatMap(sessions -> sessions.values().stream())
                .forEach(session -> {
                    try {
                        session.send(message);
                    } catch (Exception ignored) {
                    }
                });
    }
}
