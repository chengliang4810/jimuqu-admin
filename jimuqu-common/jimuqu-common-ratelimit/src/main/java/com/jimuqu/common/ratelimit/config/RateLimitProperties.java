package com.jimuqu.common.ratelimit.config;

import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import lombok.Data;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 限流配置属性
 */
@Data
@Component
@Inject(value = "${jimuqu.ratelimit}")
public class RateLimitProperties {

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
     * 是否启用限流
     */
    private boolean enabled = true;

    /**
     * 限流键前缀
     */
    private String keyPrefix = "rate_limit:";

    /**
     * 限流失败时的错误消息
     */
    private String errorMessage = "请求过于频繁，请稍后再试";

}