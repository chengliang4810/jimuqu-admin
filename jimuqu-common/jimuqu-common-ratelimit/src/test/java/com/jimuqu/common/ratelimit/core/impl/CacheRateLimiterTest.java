package com.jimuqu.common.ratelimit.core.impl;

import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import org.junit.jupiter.api.Test;
import org.noear.solon.data.cache.CacheService;

import java.lang.reflect.Type;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheRateLimiterTest {

    @Test
    void failsClosedWhenTheBackingCacheIsUnavailable() {
        CacheService cache = (CacheService) Proxy.newProxyInstance(
                CacheService.class.getClassLoader(), new Class<?>[]{CacheService.class},
                (proxy, method, args) -> {
                    throw new IllegalStateException("cache unavailable");
                });
        RateLimitConfig config = new RateLimitConfig();
        CacheRateLimiter limiter = new CacheRateLimiter(cache, config);

        RateLimitException exception = assertThrows(RateLimitException.class,
                () -> limiter.tryAcquire("login", 1, config));

        assertEquals("服务器限流异常，请稍候再试", exception.getMessage());
    }

    @Test
    void slidingWindowAllowsExactlyTheConfiguredBurstEvenBelowTen() {
        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        config.setMaxBurst(3);
        CacheRateLimiter limiter = new CacheRateLimiter(new MemoryCache(), config);

        assertEquals(true, limiter.tryAcquire("captcha", 1, config));
        assertEquals(true, limiter.tryAcquire("captcha", 1, config));
        assertEquals(true, limiter.tryAcquire("captcha", 1, config));
        assertEquals(false, limiter.tryAcquire("captcha", 1, config));
    }

    @Test
    void fixedWindowReportsAndEnforcesRemainingPermits() {
        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(RateLimitAlgorithm.FIXED_WINDOW);
        config.setMaxBurst(2);
        CacheRateLimiter limiter = new CacheRateLimiter(new MemoryCache(), config);

        assertEquals(true, limiter.tryAcquire("login", 1, config));
        assertEquals(1.0, limiter.getRemainingPermits("login"), 0.001);
        assertEquals(true, limiter.tryAcquire("login", 1, config));
        assertEquals(false, limiter.tryAcquire("login", 1, config));
    }

    private static final class MemoryCache implements CacheService {
        private final Map<String, Object> values = new ConcurrentHashMap<>();

        @Override
        public void store(String key, Object value, int seconds) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Type type) {
            return (T) values.get(key);
        }
    }
}
