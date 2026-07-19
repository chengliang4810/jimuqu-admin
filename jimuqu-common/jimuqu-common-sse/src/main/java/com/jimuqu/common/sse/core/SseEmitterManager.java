package com.jimuqu.common.sse.core;

import cn.hutool.v7.core.map.MapUtil;
import org.noear.solon.web.sse.SseEmitter;
import org.noear.solon.web.sse.SseEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 管理 Server-Sent Events (SSE) 连接
 *
 * @author Lion Li
 */
public class SseEmitterManager {

    private static final String KICKED = "kicked";
    private final static Map<Long, Map<String, SseEmitter>> USER_TOKEN_EMITTERS = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutorService;

    public SseEmitterManager(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * 建立与指定用户的 SSE 连接
     *
     * @param userId 用户的唯一标识符，用于区分不同用户的连接
     * @param token  用户的唯一令牌，用于识别具体的连接
     * @return 返回一个 SseEmitter 实例，客户端可以通过该实例接收 SSE 事件
     */
    public SseEmitter connect(Long userId, String token) {
        // 从 USER_TOKEN_EMITTERS 中获取或创建当前用户的 SseEmitter 映射表（ConcurrentHashMap）
        // 每个用户可以有多个 SSE 连接，通过 token 进行区分
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

        // 创建一个新的 SseEmitter 实例，超时时间设置为一天 避免连接之后直接关闭浏览器导致连接停滞
        SseEmitter emitter = new SseEmitter(86400000L);

        SseEmitter oldEmitter = emitters.put(token, emitter);
        if (oldEmitter != null) {
            try {
                // 控制信号使用独立事件名，避免 Bell 将纯文本当作 JSON 消息解析。
                oldEmitter.send(new SseEvent().name(KICKED));
            } catch (Exception ignored) {
            }
            scheduledExecutorService.schedule(oldEmitter::complete, 100L, TimeUnit.MILLISECONDS);
        }

        // 当 emitter 完成、超时或发生错误时，从映射表中移除对应的 token
        emitter.onCompletion(() -> {
            remove(userId, token, emitter);
        });
        emitter.onTimeout(() -> {
            remove(userId, token, emitter);
        });
        emitter.onError((e) -> {
            remove(userId, token, emitter);
        });

        try {
            // 向客户端发送一条连接成功的事件
            emitter.send(new SseEvent().name("connected"));
        } catch (IOException e) {
            // 如果发送消息失败，则从映射表中移除 emitter
            remove(userId, token, emitter);
        }
        return emitter;
    }

    /**
     * 断开指定用户的 SSE 连接
     *
     * @param userId 用户的唯一标识符，用于区分不同用户的连接
     * @param token  用户的唯一令牌，用于识别具体的连接
     */
    public void disconnect(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            SseEmitter sseEmitter = emitters.remove(token);
            try {
                if (sseEmitter == null) {
                    return;
                }
                sseEmitter.send(new SseEvent().name("disconnected"));
                sseEmitter.complete();
            } catch (Exception ignore) {
            }
            if (emitters.isEmpty()) {
                USER_TOKEN_EMITTERS.remove(userId, emitters);
            }
        } else {
            USER_TOKEN_EMITTERS.remove(userId);
        }
    }

    /**
     * 向指定的用户会话发送消息
     *
     * @param userId  要发送消息的用户id
     * @param message 要发送的消息内容
     */
    public void sendMessage(Long userId, String message) {
        this.sendEvent(userId, new SseEvent().name("message").data(message));
    }

    /**
     * 本机全用户会话发送消息
     *
     * @param message 要发送的消息内容
     */
    public void sendMessage(String message) {
        for (Long userId : USER_TOKEN_EMITTERS.keySet()) {
            sendMessage(userId, message);
        }
    }

    /**
     * 向指定的用户会话发送消息
     *
     * @param userId  要发送消息的用户id
     * @param sseEvent 要发送的消息内容
     */
    public void sendEvent(Long userId, SseEvent sseEvent) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    entry.getValue().send(sseEvent);
                } catch (Exception e) {
                    SseEmitter remove = emitters.remove(entry.getKey(), entry.getValue()) ? entry.getValue() : null;
                    if (remove != null) {
                        remove.complete();
                    }
                }
            }
            if (emitters.isEmpty()) {
                USER_TOKEN_EMITTERS.remove(userId, emitters);
            }
        } else {
            USER_TOKEN_EMITTERS.remove(userId);
        }
    }

    /**
     * 本机全用户会话发送 SseEvent
     * @param sseEvent SseEvent
     */
    public void sendEvent(SseEvent sseEvent) {
        for (Long userId : USER_TOKEN_EMITTERS.keySet()) {
            sendEvent(userId, sseEvent);
        }
    }

    private void remove(Long userId, String token, SseEmitter emitter) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (emitters != null && emitters.remove(token, emitter) && emitters.isEmpty()) {
            USER_TOKEN_EMITTERS.remove(userId, emitters);
        }
    }

}
