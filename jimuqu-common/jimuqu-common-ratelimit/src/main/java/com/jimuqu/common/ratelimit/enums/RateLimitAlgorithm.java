package com.jimuqu.common.ratelimit.enums;

/**
 * 限流算法枚举
 */
public enum RateLimitAlgorithm {
    /**
     * 令牌桶算法
     */
    TOKEN_BUCKET,

    /**
     * 滑动窗口算法
     */
    SLIDING_WINDOW,

    /**
     * 固定窗口算法
     */
    FIXED_WINDOW
}