package com.jimuqu.common.ratelimit.config;

import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.core.impl.CacheRateLimiter;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.cache.CacheService;

/**
 * 限流自动配置
 */
@Configuration
public class RateLimitAutoConfiguration {

    @Inject
    private RateLimitProperties rateLimitProperties;

    /**
     * 限流配置
     */
    @Bean
    public RateLimitConfig rateLimitConfig() {
        RateLimitConfig config = new RateLimitConfig();
        if (rateLimitProperties != null) {
            config.setType(rateLimitProperties.getType());
            config.setPermitsPerSecond(rateLimitProperties.getPermitsPerSecond());
            config.setMaxBurst(rateLimitProperties.getMaxBurst());
            config.setWindow(rateLimitProperties.getWindow());
            config.setAlgorithm(rateLimitProperties.getAlgorithm());
            config.setEnabled(rateLimitProperties.isEnabled());
            config.setKeyPrefix(rateLimitProperties.getKeyPrefix());
            config.setErrorMessage(rateLimitProperties.getErrorMessage());
        }
        return config;
    }

    /**
     * 统一缓存限流器
     */
    @Bean
    public RateLimiter rateLimiter(CacheService cacheService, RateLimitConfig rateLimitConfig) {
        return new CacheRateLimiter(cacheService, rateLimitConfig);
    }
}
