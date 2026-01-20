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
     * 状态码 常见错误码：
     *
     * 错误码	说明
     * 0	成功
     * 1	参数错误
     * 2	认证失败
     * 3	服务限流
     * 4	服务异常
     * 5	上下文过长
     * 错误响应示例：
     *
     * {
     *   "code": 1,
     *   "message": "参数错误",
     *   "error": "query参数不能为空"
     * }
     * {
     *   "code": 4,
     *   "message": "服务异常",
     *   "error": "AI服务暂时不可用，请稍后重试"
     * }
     */
    private Integer code = 0;

    /**
     * 响应id
     */
    private String responseId;

    /**
     * 是否完成
     */
    private boolean isEnd;

    /**
     * 内容（思考或回答内容）
     */
    private String content;

    /**
     * 角色
     */
    private String role;

    /**
     * 是否思考中
     */
    private boolean isThinking;


    public ChatResponseVo(ChatResponse chatResponse) {
        AssistantMessage message = chatResponse.getMessage();
        this.isEnd = chatResponse.isFinished();
        this.role = message.getRole().name();
        this.isThinking = message.isThinking();
        this.content = message.getContent();
    }

    @Override
    public String toString() {
        return JsonUtil.toString( this);
    }

}
