package com.jimuqu.ai.domain;

import com.jimuqu.common.core.utils.JsonUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.ai.chat.message.AssistantMessage;

import java.io.Serializable;

/**
 * 聊天响应
 *
 * @author chengliang
 * @date 2025/12/28
 */
@Data
@NoArgsConstructor
public class ChatResponseVo implements Serializable {

    /**
     * 是否完成
     */
    private boolean isFinished;

    /**
     * 内容
     */
    private String content;

    /**
     * 角色
     */
    private String role;

    /**
     * 思考
     */
    private Boolean isThinking;


    public ChatResponseVo(ChatResponse chatResponse) {
        AssistantMessage message = chatResponse.getMessage();
        this.isFinished = chatResponse.isFinished();
        this.role = message.getRole().name();
        this.content = message.getContent();
        this.isThinking = message.isThinking();
    }

    @Override
    public String toString() {
        return JsonUtil.toString( this);
    }

}
