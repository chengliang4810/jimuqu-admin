package com.jimuqu.common.core.exception.base;

import com.jimuqu.common.core.utils.MessageUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 基础异常
 *
 * @author chengliang
 * @date 2024/08/07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属模块
     */
    private String module;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 消息内容对应的参数
     */
    private Object[] args;

    public BaseException(String module, Integer code, String message) {
        this.module = module;
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return MessageUtils.message(message, args);
    }

}
