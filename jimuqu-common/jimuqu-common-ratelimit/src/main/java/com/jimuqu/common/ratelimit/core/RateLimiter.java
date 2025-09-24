package com.jimuqu.common.ratelimit.core;

/**
 * 限流器接口
 */
public interface RateLimiter {

    /**
     * 尝试获取令牌
     * @param key 限流键
     * @return 是否获取成功
     */
    boolean tryAcquire(String key);

    /**
     * 尝试获取令牌
     * @param key 限流键
     * @param permits 需要的令牌数
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, int permits);

    /**
     * 尝试获取令牌（使用指定配置）
     * @param key 限流键
     * @param permits 需要的令牌数
     * @param config 限流配置
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, int permits, RateLimitConfig config);

    /**
     * 获取剩余的令牌数
     * @param key 限流键
     * @return 剩余令牌数
     */
    double getRemainingPermits(String key);

    /**
     * 获取限流配置
     * @param key 限流键
     * @return 限流配置
     */
    RateLimitConfig getConfig(String key);
}