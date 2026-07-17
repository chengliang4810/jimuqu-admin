package com.jimuqu.common.ratelimit.annotation;

import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    @Test
    void mapsEveryAnnotationSettingWithoutCrossRouteCache() throws Exception {
        RateLimit annotation = Fixture.class.getDeclaredMethod("limited").getAnnotation(RateLimit.class);

        RateLimitConfig config = RateLimitFilter.createRateLimitConfig(annotation);

        assertEquals(RateLimitType.USER, config.getType());
        assertEquals(3.5, config.getPermitsPerSecond());
        assertEquals(7, config.getMaxBurst());
        assertEquals(19, config.getWindow());
        assertEquals(RateLimitAlgorithm.FIXED_WINDOW, config.getAlgorithm());
        assertEquals("稍后重试", config.getErrorMessage());
    }

    private static class Fixture {
        @RateLimit(type = RateLimitType.USER, permitsPerSecond = 3.5, maxBurst = 7,
                window = 19, algorithm = RateLimitAlgorithm.FIXED_WINDOW, message = "稍后重试")
        void limited() {
        }
    }
}
