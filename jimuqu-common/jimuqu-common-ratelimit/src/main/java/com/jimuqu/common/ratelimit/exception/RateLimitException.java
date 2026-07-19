package com.jimuqu.common.ratelimit.exception;

import com.jimuqu.common.core.exception.base.BaseException;

/**
 * 限流异常
 *
 * @author chengliang
 * @date 2025/09/24
 */
public class RateLimitException extends BaseException {

    private static final String MODULE = "ratelimit";
    private static final int DEFAULT_CODE = 500;

    public RateLimitException(String message) {
        super(MODULE, DEFAULT_CODE, message);
    }

    public RateLimitException(String message, Throwable cause) {
        this(message);
        initCause(cause);
    }

    public RateLimitException(Integer code, String message) {
        super(MODULE, code == null ? DEFAULT_CODE : code, message);
    }

    public RateLimitException(Integer code, String message, Throwable cause) {
        this(code, message);
        initCause(cause);
    }
}
