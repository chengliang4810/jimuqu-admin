package com.jimuqu.common.web.interceptor;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeConsumingInterceptorTest {

    @Test
    void sanitizesNestedJsonWithoutRejectingArrays() {
        String body = "[{\"name\":\"admin\",\"password\":\"secret\","
                + "\"profile\":{\"Authorization\":\"token\",\"city\":\"Shanghai\"}}]";

        String sanitized = TimeConsumingInterceptor.sanitizeRequestBody("application/json;charset=UTF-8", body);

        assertTrue(sanitized.contains("admin"));
        assertTrue(sanitized.contains("Shanghai"));
        assertFalse(sanitized.contains("secret"));
        assertFalse(sanitized.contains("token"));
    }

    @Test
    void invalidOrNonJsonBodiesCannotBreakRequestLogging() {
        assertEquals("[无法解析的 JSON 请求体]",
                TimeConsumingInterceptor.sanitizeRequestBody("application/json", "not-json"));
        assertEquals("", TimeConsumingInterceptor.sanitizeRequestBody("multipart/form-data", "binary"));
    }

    @Test
    void parameterSanitizingDoesNotMutateTheRequestMap() {
        Map<String, List<String>> source = new LinkedHashMap<>();
        source.put("clientid", List.of("pc"));
        source.put("name", List.of("admin"));

        Map<String, List<String>> sanitized = TimeConsumingInterceptor.sanitizeParams(source);

        assertEquals(List.of("pc"), source.get("clientid"));
        assertEquals(Map.of("name", List.of("admin")), sanitized);
    }
}
