package com.jimuqu.common.sse.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Bell 前端可直接消费的 SSE 消息负载。
 */
@Data
@Accessors(chain = true)
public class SseMessagePayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String messageId;
    private String message;
    private String type;
    private String source;
    private Long timestamp;
    private String path;
    private Object data;

    public static SseMessagePayload message(String message) {
        return new SseMessagePayload()
                .setMessageId(UUID.randomUUID().toString())
                .setMessage(message)
                .setType("message")
                .setSource("backend")
                .setTimestamp(System.currentTimeMillis());
    }
}
