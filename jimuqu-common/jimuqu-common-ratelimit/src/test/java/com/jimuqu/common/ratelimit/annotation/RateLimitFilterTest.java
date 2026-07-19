package com.jimuqu.common.ratelimit.annotation;

import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.config.RateLimitProperties;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.route.RouterInterceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void rateLimitFailuresUseTheUnifiedExceptionChainInsideTheGlobalFilter() {
        Component component = RateLimitFilter.class.getAnnotation(Component.class);
        RateLimitException exception = new RateLimitException("访问过于频繁");

        assertEquals(-80, component.index());
        assertTrue(RouterInterceptor.class.isAssignableFrom(RateLimitFilter.class));
        assertEquals(500, exception.getCode());
        assertEquals("访问过于频繁", exception.getMessage());
        assertInstanceOf(com.jimuqu.common.core.exception.base.BaseException.class, exception);
    }

    @Test
    void defaultMessageMatchesUpstreamContract() {
        assertEquals("访问过于频繁，请稍候再试", new RateLimitConfig().getErrorMessage());
        assertEquals("访问过于频繁，请稍候再试", new RateLimitProperties().getErrorMessage());
    }

    private static class Fixture {
        @RateLimit(type = RateLimitType.USER, permitsPerSecond = 3.5, maxBurst = 7,
                window = 19, algorithm = RateLimitAlgorithm.FIXED_WINDOW, message = "稍后重试")
        void limited() {
        }
    }
}
