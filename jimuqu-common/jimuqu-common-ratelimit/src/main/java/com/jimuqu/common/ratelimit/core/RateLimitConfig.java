package com.jimuqu.common.ratelimit.core;

import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import lombok.Data;

/**
 * 限流配置
 */
@Data
public class RateLimitConfig {

    /**
     * 是否启用限流
     */
    private boolean enabled = true;

    /**
     * 限流类型：IP-基于IP限流，USER-基于用户限流，GLOBAL-全局限流
     */
    private RateLimitType type = RateLimitType.IP;

    /**
     * 每秒生成令牌数
     */
    private double permitsPerSecond = 10.0;

    /**
     * 最大突发请求数
     */
    private int maxBurst = 100;

    /**
     * 限流时间窗口（秒）
     */
    private long window = 60;

    /**
     * 限流算法
     */
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * 限流键前缀
     */
    private String keyPrefix = "rate_limit:";

    /**
     * 限流失败时的错误消息
     */
    private String errorMessage = "访问过于频繁，请稍候再试";


}
