package com.jimuqu.common.ratelimit.enums;

/**
 * 限流类型枚举（基于限流维度）
 */
public enum RateLimitType {
    /**
     * 基于IP限流
     */
    IP,

    /**
     * 基于用户限流
     */
    USER,

    /**
     * 全局限流（基于方法或自定义key）
     */
    GLOBAL
}