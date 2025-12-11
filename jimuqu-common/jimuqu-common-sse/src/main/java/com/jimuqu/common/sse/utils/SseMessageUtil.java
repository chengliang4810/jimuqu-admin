package com.jimuqu.common.sse.utils;

import com.jimuqu.common.sse.core.SseEmitterManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;

/**
 * SSE工具类
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SseMessageUtil {

    private final static Boolean SSE_ENABLE = Solon.cfg().getBool("sse.enabled",false);
    private static SseEmitterManager MANAGER;

    static {
        if (isEnable() && MANAGER == null) {
            Solon.context().getBeanAsync(SseEmitterManager.class, (sseEmitterManager) -> MANAGER = sseEmitterManager);
        }
    }

    /**
     * 向指定的SSE会话发送消息
     *
     * @param userId  要发送消息的用户id
     * @param message 要发送的消息内容
     */
    public static void sendMessage(Long userId, String message) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendMessage(userId, message);
    }

    /**
     * 本机全用户会话发送消息
     *
     * @param message 要发送的消息内容
     */
    public static void sendMessage(String message) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendMessage(message);
    }

//    /**
//     * 发布SSE订阅消息
//     *
//     * @param sseMessageDto 要发布的SSE消息对象
//     */
//    public static void publishMessage(SseMessageDto sseMessageDto) {
//        if (!isEnable()) {
//            return;
//        }
//        MANAGER.publishMessage(sseMessageDto);
//    }
//
//    /**
//     * 向所有的用户发布订阅的消息(群发)
//     *
//     * @param message 要发布的消息内容
//     */
//    public static void publishAll(String message) {
//        if (!isEnable()) {
//            return;
//        }
//        MANAGER.publishAll(message);
//    }

    /**
     * 是否开启
     */
    public static Boolean isEnable() {
        return SSE_ENABLE;
    }

}
