package com.jimuqu.common.web.filter;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseDisconnectExceptionTest {

    @Test
    void onlySuppressesIoDisconnectsOnTheConfiguredSsePath() {
        RuntimeException disconnect = new RuntimeException(new IOException("broken pipe"));

        assertTrue(GlobalExceptionFilter.isSseDisconnect(
                disconnect, "/resource/message", "/resource/message"));
        assertFalse(GlobalExceptionFilter.isSseDisconnect(
                disconnect, "/resource/oss/download/1", "/resource/message"));
        assertFalse(GlobalExceptionFilter.isSseDisconnect(
                new IllegalStateException("broken pipe"), "/resource/message", "/resource/message"));
    }
}
