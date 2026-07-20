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

    public static final int REPLACED_CLOSE_CODE = 4001;
    public static final String REPLACED_CLOSE_REASON = "kicked";

    private static final Map<Long, Map<String, WebSocket>> USER_SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 将WebSocket会话添加到用户会话Map中
     *
     * @param sessionKey 会话键，用于检索会话
     * @param session    要添加的WebSocket会话
     */
    public static void addSession(Long userId, String token, WebSocket session) {
        WebSocket oldSession = USER_SESSION_MAP.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .put(token, session);
        closeReplaced(oldSession);
    }

    /**
     * 移除用户指定 token 对应的 WebSocket 会话
     *
     * @param userId 用户 ID
     * @param token 登录 token
     */
    public static void removeSession(Long userId, String token) {
        removeSession(userId, token, null);
    }

    public static void removeSession(Long userId, String token, WebSocket session) {
        Map<String, WebSocket> sessions = USER_SESSION_MAP.get(userId);
        if (sessions != null) {
            if (session == null) {
                close(sessions.remove(token));
            } else {
                sessions.remove(token, session);
            }
            if (sessions.isEmpty()) {
                USER_SESSION_MAP.remove(userId, sessions);
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
        for (Long userId : Set.copyOf(USER_SESSION_MAP.keySet())) {
            sendMessage(userId, message);
        }
    }

    public static void sendMessage(Long userId, String message) {
        Map<String, WebSocket> sessions = USER_SESSION_MAP.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.entrySet().removeIf(entry -> {
            if (!isValid(entry.getValue())) {
                close(entry.getValue());
                return true;
            }
            try {
                entry.getValue().send(message);
                return false;
            } catch (Exception ignored) {
                close(entry.getValue());
                return true;
            }
        });
        if (sessions.isEmpty()) {
            USER_SESSION_MAP.remove(userId, sessions);
        }
    }

    /**
     * 周期清理已失效的会话，避免静默断网后会话长期滞留。
     */
    public static void sessionMonitor() {
        USER_SESSION_MAP.forEach((userId, sessions) -> {
            sessions.entrySet().removeIf(entry -> {
                WebSocket session = entry.getValue();
                if (isValid(session)) {
                    return false;
                }
                close(session);
                return true;
            });
            if (sessions.isEmpty()) {
                USER_SESSION_MAP.remove(userId, sessions);
            }
        });
    }

    private static boolean isValid(WebSocket session) {
        if (session == null) {
            return false;
        }
        try {
            return session.isValid();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void close(WebSocket session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (Exception ignored) {
        }
    }

    private static void closeReplaced(WebSocket session) {
        if (session == null) {
            return;
        }
        try {
            session.close(REPLACED_CLOSE_CODE, REPLACED_CLOSE_REASON);
        } catch (Exception ignored) {
        }
    }

}
