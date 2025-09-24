package com.jimuqu.common.ratelimit.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * 限流异常
 *
 * @author chengliang
 * @date 2025/09/24
 */
@Setter
@Getter
public class RateLimitException extends RuntimeException {

    private Integer code;

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    public RateLimitException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public RateLimitException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}