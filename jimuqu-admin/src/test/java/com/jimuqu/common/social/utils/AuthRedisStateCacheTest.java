package com.jimuqu.common.social.utils;

import org.junit.jupiter.api.Test;
import org.noear.solon.data.cache.CacheService;

import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRedisStateCacheTest {

    @Test
    void roundsMillisecondTimeoutsUpWithoutCreatingZeroSecondEntries() {
        RecordingCacheService cacheService = new RecordingCacheService();
        AuthRedisStateCache cache = new AuthRedisStateCache(cacheService);

        cache.cache("one", "value", 1);
        assertEquals(1, cacheService.seconds);
        cache.cache("subsecond", "value", 999);
        assertEquals(1, cacheService.seconds);
        cache.cache("second", "value", 1000);
        assertEquals(1, cacheService.seconds);
        cache.cache("fraction", "value", 1500);
        assertEquals(2, cacheService.seconds);
        cache.cache("maximum", "value", Long.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, cacheService.seconds);
    }

    @Test
    void rejectsNonPositiveCustomTimeouts() {
        AuthRedisStateCache cache = new AuthRedisStateCache(new RecordingCacheService());

        assertThrows(IllegalArgumentException.class, () -> cache.cache("zero", "value", 0));
        assertThrows(IllegalArgumentException.class, () -> cache.cache("negative", "value", -1));
    }

    private static final class RecordingCacheService implements CacheService {
        private int seconds;

        @Override
        public void store(String key, Object value, int seconds) {
            this.seconds = seconds;
        }

        @Override
        public void remove(String key) {
        }

        @Override
        public <T> T get(String key, Type type) {
            return null;
        }
    }
}
