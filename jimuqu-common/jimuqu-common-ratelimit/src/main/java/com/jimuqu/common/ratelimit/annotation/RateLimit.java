package com.jimuqu.common.ratelimit.annotation;

import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 是否启用限流
     */
    boolean enabled() default true;

    /**
     * 限流键
     * 当type为GLOBAL时使用，默认使用方法全名
     */
    String key() default "";

    /**
     * 限流类型：IP-基于IP限流，USER-基于用户限流，GLOBAL-全局限流
     */
    RateLimitType type() default RateLimitType.IP;

    /**
     * 每秒生成令牌数
     */
    double permitsPerSecond() default 10.0;

    /**
     * 最大突发请求数
     */
    int maxBurst() default 100;

    /**
     * 限流时间窗口（秒）
     */
    long window() default 60;

    /**
     * 限流算法
     */
    RateLimitAlgorithm algorithm() default RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * 限流失败时的错误消息
     */
    String message() default "";
}