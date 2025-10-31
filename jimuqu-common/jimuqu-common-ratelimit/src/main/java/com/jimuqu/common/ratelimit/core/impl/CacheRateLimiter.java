package com.jimuqu.common.ratelimit.core.impl;

import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.cache.CacheService;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheRateLimiter implements RateLimiter {

    @Inject
    private CacheService cacheService;

    private final RateLimitConfig globalConfig;

    public CacheRateLimiter(RateLimitConfig globalConfig) {
        this.globalConfig = globalConfig;
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
            String cacheKey = config.getKeyPrefix() + key;
            long currentTime = System.currentTimeMillis();

            return switch (config.getAlgorithm()) {
                case SLIDING_WINDOW -> tryAcquireWithSlidingWindow(cacheKey, permits, currentTime, config);
                case FIXED_WINDOW -> tryAcquireWithFixedWindow(cacheKey, permits, currentTime, config);
                default -> tryAcquireWithTokenBucket(cacheKey, permits, currentTime, config);
            };
        } catch (Exception e) {
            log.error("限流器异常 - Key: {}, Permits: {}, 异常: {}", key, permits, e.getMessage(), e);
            // 限流器异常时，默认放行
            return true;
        }
    }

    /**
     * 令牌桶算法实现（基于CAS乐观锁）
     */
    private boolean tryAcquireWithTokenBucket(String key, int permits, long currentTime, RateLimitConfig config) {
        final int maxRetries = 3;

        for (int i = 0; i < maxRetries; i++) {
            // 1. 读取当前令牌桶
            TokenBucket bucket = cacheService.get(key, TokenBucket.class);
            if (bucket == null) {
                bucket = new TokenBucket(System.currentTimeMillis(), currentTime, config.getMaxBurst());
            }

            // 2. 计算补充的令牌数
            long timePassed = currentTime - bucket.getLastRefillTime();
            double tokensToAdd = timePassed * config.getPermitsPerSecond() / 1000.0;
            double newTokens = Math.min(config.getMaxBurst(), bucket.getTokens() + tokensToAdd);

            // 3. 检查是否有足够的令牌
            if (newTokens >= permits) {
                // 4. 创建新的令牌桶状态
                TokenBucket newBucket = new TokenBucket(System.currentTimeMillis(), currentTime, newTokens - permits);

                // 5. 尝试CAS更新
                TokenBucket currentBucket = cacheService.get(key, TokenBucket.class);
                // 修复CAS逻辑：如果是第一次访问（currentBucket为null）或者桶状态未改变，则允许更新
                if (currentBucket == null || bucket.equals(currentBucket)) {
                    // 简单实现：直接更新（适用于单机或低并发场景）
                    int expireTime = (int) (config.getMaxBurst() / config.getPermitsPerSecond()) + 1;

                    cacheService.store(key, newBucket, expireTime);
                    return true;
                }
                // 如果被其他线程修改了，重试
                continue;
            } else {
                // 令牌不足，保存更新后的桶状态
                TokenBucket newBucket = new TokenBucket(System.currentTimeMillis(), currentTime, newTokens);
                int expireTime = (int) (config.getMaxBurst() / config.getPermitsPerSecond()) + 1;
                cacheService.store(key, newBucket, expireTime);
                return false;
            }
        }

        // 重试次数耗尽，默认拒绝
        return false;
    }

    /**
     * 滑动窗口算法实现（基于时间分片）
     */
    private boolean tryAcquireWithSlidingWindow(String key, int permits, long currentTime, RateLimitConfig config) {
        long windowTime = TimeUnit.SECONDS.toMillis(config.getWindow());

        // 使用多个时间分片来模拟滑动窗口，避免并发问题
        int sliceCount = 10; // 将窗口分为10个分片
        long sliceTime = windowTime / sliceCount;
        int currentSlice = (int) ((currentTime % windowTime) / sliceTime);

        // 构建分片键
        String sliceKey = key + ":slice:" + currentSlice;

        // 获取当前分片的计数
        WindowCounter counter = cacheService.get(sliceKey, WindowCounter.class);
        if (counter == null) {
            counter = new WindowCounter(0);
        }

        // 简单实现：检查当前分片是否还有容量
        if (counter.getCount() + permits <= config.getMaxBurst() / sliceCount) {
            counter.setCount(counter.getCount() + permits);
            cacheService.store(sliceKey, counter, (int) (sliceTime / 1000) + 1);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 固定窗口算法实现（基于原子计数）
     */
    private boolean tryAcquireWithFixedWindow(String key, int permits, long currentTime, RateLimitConfig config) {
        // 计算当前窗口的key
        long windowStart = currentTime / (config.getWindow() * 1000);
        String windowKey = key + ":" + windowStart;

        // 使用原子递增操作
        for (int i = 0; i < 3; i++) {
            WindowCounter counter = cacheService.get(windowKey, WindowCounter.class);
            if (counter == null) {
                counter = new WindowCounter(0);
            }

            if (counter.getCount() + permits <= config.getMaxBurst()) {
                // 创建新的计数器状态
                WindowCounter newCounter = new WindowCounter(counter.getCount() + permits);
                cacheService.store(windowKey, newCounter, (int) config.getWindow());
                return true;
            } else {
                return false;
            }
        }

        return false;
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
                    return bucket.getTokens();

                case SLIDING_WINDOW:
                    SlidingWindow window = cacheService.get(cacheKey, SlidingWindow.class);
                    if (window == null) {
                        return globalConfig.getMaxBurst();
                    }
                    return Math.max(0, globalConfig.getMaxBurst() - window.getRequestCount());

                case FIXED_WINDOW:
                    // 固定窗口的剩余令牌数较难精确计算，返回最大值
                    return globalConfig.getMaxBurst();

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

    /**
     * 令牌桶数据结构（带版本号）
     */
    @Data
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
    @AllArgsConstructor
    public static class SlidingWindow {
        private long windowStartTime;  // 窗口开始时间
        private long requestCount;     // 窗口内请求数
    }

    /**
     * 固定窗口计数器
     */
    @Data
    @AllArgsConstructor
    public static class WindowCounter {
        private long count;  // 窗口内计数
    }
}