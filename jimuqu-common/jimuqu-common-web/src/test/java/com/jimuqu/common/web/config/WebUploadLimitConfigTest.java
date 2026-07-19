package com.jimuqu.common.web.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebUploadLimitConfigTest {

    @Test
    void keepsBellUploadLimits() throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream("config/common-web.yml")) {
            String config = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(config.contains("server.request.maxBodySize: 20mb"));
            assertTrue(config.contains("server.request.maxFileSize: 10mb"));
        }
    }
}
