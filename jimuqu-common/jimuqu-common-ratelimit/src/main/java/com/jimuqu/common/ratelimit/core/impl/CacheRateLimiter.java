package com.jimuqu.common.ratelimit.core.impl;

import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.data.cache.CacheService;
import org.redisson.api.RLock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheRateLimiter implements RateLimiter {

    private final CacheService cacheService;

    private final RateLimitConfig globalConfig;

    public CacheRateLimiter(CacheService cacheService, RateLimitConfig globalConfig) {
        this.cacheService = Objects.requireNonNull(cacheService, "cacheService");
        this.globalConfig = Objects.requireNonNull(globalConfig, "globalConfig");
    }

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        return tryAcquire(key, permits, globalConfig);
    }

    @Override
    public boolean tryAcquire(String key, int permits, RateLimitConfig config) {
        if (!config.isEnabled()) {
            return true;
        }

        try {
            validate(permits, config);
            String cacheKey = config.getKeyPrefix() + key;
            if (cacheService instanceof RedissonCacheService redisson) {
                RLock lock = redisson.client().getLock(cacheKey + ":lock");
                lock.lock();
                try {
                    return acquire(cacheKey, permits, config);
                } finally {
                    lock.unlock();
                }
            }
            synchronized (cacheService) {
                return acquire(cacheKey, permits, config);
            }
        } catch (Exception e) {
            log.error("限流器异常 - Key: {}, Permits: {}, 异常: {}", key, permits, e.getMessage(), e);
            throw new RateLimitException("服务器限流异常，请稍候再试", e);
        }
    }

    private boolean acquire(String cacheKey, int permits, RateLimitConfig config) {
        long currentTime = System.currentTimeMillis();
        return switch (config.getAlgorithm()) {
            case SLIDING_WINDOW -> tryAcquireWithSlidingWindow(cacheKey, permits, currentTime, config);
            case FIXED_WINDOW -> tryAcquireWithFixedWindow(cacheKey, permits, currentTime, config);
            default -> tryAcquireWithTokenBucket(cacheKey, permits, currentTime, config);
        };
    }

    private void validate(int permits, RateLimitConfig config) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(config.getAlgorithm(), "algorithm");
        Objects.requireNonNull(config.getKeyPrefix(), "keyPrefix");
        if (permits <= 0 || config.getMaxBurst() <= 0 || config.getWindow() <= 0
                || config.getPermitsPerSecond() <= 0) {
            throw new IllegalArgumentException("限流参数必须大于 0");
        }
    }

    /**
     * 令牌桶算法实现（基于CAS乐观锁）
     */
    private boolean tryAcquireWithTokenBucket(String key, int permits, long currentTime, RateLimitConfig config) {
        TokenBucket bucket = cacheService.get(key, TokenBucket.class);
        if (bucket == null) {
            bucket = new TokenBucket(0, currentTime, config.getMaxBurst());
        }
        long timePassed = Math.max(0, currentTime - bucket.getLastRefillTime());
        double tokens = Math.min(config.getMaxBurst(),
                bucket.getTokens() + timePassed * config.getPermitsPerSecond() / 1000.0);
        boolean acquired = tokens >= permits;
        cacheService.store(key, new TokenBucket(bucket.getVersion() + 1, currentTime,
                acquired ? tokens - permits : tokens), tokenBucketTtl(config));
        return acquired;
    }

    /**
     * 滑动窗口算法实现（基于时间分片）
     */
    private boolean tryAcquireWithSlidingWindow(String key, int permits, long currentTime, RateLimitConfig config) {
        long windowTime = TimeUnit.SECONDS.toMillis(config.getWindow());

        SlidingWindow window = cacheService.get(key, SlidingWindow.class);
        List<Long> requestTimes = window == null || window.getRequestTimes() == null
                ? new ArrayList<>()
                : new ArrayList<>(window.getRequestTimes());
        long cutoff = currentTime - windowTime;
        requestTimes.removeIf(timestamp -> timestamp <= cutoff);
        if (requestTimes.size() + permits > config.getMaxBurst()) {
            cacheService.store(key, new SlidingWindow(requestTimes), windowTtl(config));
            return false;
        }
        for (int index = 0; index < permits; index++) {
            requestTimes.add(currentTime);
        }
        cacheService.store(key, new SlidingWindow(requestTimes), windowTtl(config));
        return true;
    }

    /**
     * 固定窗口算法实现（基于原子计数）
     */
    private boolean tryAcquireWithFixedWindow(String key, int permits, long currentTime, RateLimitConfig config) {
        // 计算当前窗口的key
        long windowStart = currentTime / (config.getWindow() * 1000);
        String windowKey = key + ":" + windowStart;

        WindowCounter counter = cacheService.get(windowKey, WindowCounter.class);
        long count = counter == null ? 0 : counter.getCount();
        if (count + permits > config.getMaxBurst()) {
            return false;
        }
        cacheService.store(windowKey, new WindowCounter(count + permits), windowTtl(config));
        return true;
    }

    @Override
    public double getRemainingPermits(String key) {
        try {
            String cacheKey = globalConfig.getKeyPrefix() + key;

            switch (globalConfig.getAlgorithm()) {
                case TOKEN_BUCKET:
                    TokenBucket bucket = cacheService.get(cacheKey, TokenBucket.class);
                    if (bucket == null) {
                        return globalConfig.getMaxBurst();
                    }
                    long elapsed = Math.max(0, System.currentTimeMillis() - bucket.getLastRefillTime());
                    return Math.min(globalConfig.getMaxBurst(),
                            bucket.getTokens() + elapsed * globalConfig.getPermitsPerSecond() / 1000.0);

                case SLIDING_WINDOW:
                    SlidingWindow window = cacheService.get(cacheKey, SlidingWindow.class);
                    if (window == null || window.getRequestTimes() == null) {
                        return globalConfig.getMaxBurst();
                    }
                    long cutoff = System.currentTimeMillis()
                            - TimeUnit.SECONDS.toMillis(globalConfig.getWindow());
                    long active = window.getRequestTimes().stream().filter(timestamp -> timestamp > cutoff).count();
                    return Math.max(0, globalConfig.getMaxBurst() - active);

                case FIXED_WINDOW:
                    long windowStart = System.currentTimeMillis() / (globalConfig.getWindow() * 1000);
                    WindowCounter counter = cacheService.get(cacheKey + ":" + windowStart, WindowCounter.class);
                    return Math.max(0, globalConfig.getMaxBurst()
                            - (counter == null ? 0 : counter.getCount()));

                default:
                    return globalConfig.getMaxBurst();
            }
        } catch (Exception e) {
            log.error("获取剩余令牌数异常: {}", key, e);
            return 0;
        }
    }

    @Override
    public RateLimitConfig getConfig(String key) {
        return globalConfig;
    }

    private int tokenBucketTtl(RateLimitConfig config) {
        return (int) Math.min(Integer.MAX_VALUE,
                Math.ceil(config.getMaxBurst() / config.getPermitsPerSecond()) + 1);
    }

    private int windowTtl(RateLimitConfig config) {
        return (int) Math.min(Integer.MAX_VALUE, config.getWindow() + 1);
    }

    /**
     * 令牌桶数据结构（带版本号）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenBucket {
        private long version;           // 版本号，用于CAS操作
        private long lastRefillTime;    // 上次补充令牌的时间
        private double tokens;          // 当前令牌数
    }

    /**
     * 滑动窗口数据结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlidingWindow {
        private List<Long> requestTimes;
    }

    /**
     * 固定窗口计数器
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WindowCounter {
        private long count;  // 窗口内计数
    }
}
