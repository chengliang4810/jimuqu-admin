package com.jimuqu.ai.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 聊天请求
 *
 * @author chengliang
 * @date 2025/12/28
 */
@Data
@NoArgsConstructor
public class ChatRequestVo implements Serializable {

    /**
     * 会话ID
     */
    private String chatId;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息
     */
    private MessageVo message;

}
