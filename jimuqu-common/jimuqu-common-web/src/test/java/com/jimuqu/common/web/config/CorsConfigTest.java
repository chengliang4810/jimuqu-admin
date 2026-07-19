package com.jimuqu.common.web.config;

import com.jimuqu.common.web.config.properties.CorsProperties;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Bean;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.web.cors.AbstractCross;
import org.noear.solon.web.cors.CrossInterceptor;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void appliesTheConfigurableSixXDefaultsAndOverrides() throws Exception {
        CorsProperties properties = new CorsProperties();
        assertEquals(1800, properties.getMaxAge());

        properties.setAllowedOriginPatterns(List.of("https://admin.example.com"));
        properties.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        properties.setAllowedMethods(List.of("GET", "POST"));
        properties.setAllowCredentials(false);
        properties.setMaxAge(60);

        CrossInterceptor interceptor = new WebConfig().crossInterceptor(properties);

        assertEquals("", field(interceptor, "allowedOrigins"));
        assertEquals("Content-Type,Authorization", field(interceptor, "allowedHeaders"));
        assertEquals("GET,POST", field(interceptor, "allowedMethods"));
        assertEquals(60, field(interceptor, "maxAge"));
        assertFalse((boolean) field(interceptor, "allowCredentials"));
    }

    @Test
    void runsBeforeAuthenticationAndMatchesOriginPatternsExactly() throws Throwable {
        Bean bean = WebConfig.class.getMethod("crossInterceptor", CorsProperties.class)
                .getAnnotation(Bean.class);
        assertEquals(-200, bean.index());

        CorsProperties properties = new CorsProperties();
        properties.setAllowedOriginPatterns(List.of("https://*.example.com:[443,8443]"));
        CrossInterceptor interceptor = new WebConfig().crossInterceptor(properties);

        TestContext allowed = new TestContext("OPTIONS");
        allowed.headerMap().put("Origin", "https://admin.example.com:8443");
        interceptor.doIntercept(allowed, null, (ctx, handler) -> {
            throw new AssertionError("预检请求不应继续进入鉴权链");
        });
        assertTrue(allowed.getHandled());
        assertEquals("https://admin.example.com:8443",
                allowed.headerOfResponse("Access-Control-Allow-Origin"));

        TestContext prefixAttack = new TestContext("GET");
        prefixAttack.headerMap().put("Origin", "https://admin.example.co");
        interceptor.doIntercept(prefixAttack, null, (ctx, handler) -> { });
        assertNull(prefixAttack.headerOfResponse("Access-Control-Allow-Origin"));
    }

    private Object field(CrossInterceptor interceptor, String name) throws Exception {
        Field field = AbstractCross.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(interceptor);
    }

    private static final class TestContext extends ContextEmpty {

        private final String method;

        private TestContext(String method) {
            this.method = method;
        }

        @Override
        public String method() {
            return method;
        }
    }
}
