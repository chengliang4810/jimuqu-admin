package com.jimuqu.common.websocket.holder;

import org.junit.jupiter.api.Test;
import org.noear.solon.net.websocket.WebSocket;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketSessionHolderTest {

    @Test
    void monitorRemovesInvalidSessionsAndKeepsValidSessions() {
        long invalidUserId = 900000000000000001L;
        long validUserId = 900000000000000002L;
        AtomicInteger invalidCloseCount = new AtomicInteger();
        AtomicInteger validCloseCount = new AtomicInteger();
        WebSocket invalid = socket(false, invalidCloseCount);
        WebSocket valid = socket(true, validCloseCount);

        try {
            WebSocketSessionHolder.addSession(invalidUserId, "invalid-token", invalid);
            WebSocketSessionHolder.addSession(validUserId, "valid-token", valid);

            WebSocketSessionHolder.sessionMonitor();

            assertFalse(WebSocketSessionHolder.existSession(invalidUserId));
            assertTrue(WebSocketSessionHolder.existSession(validUserId));
            assertEquals(1, invalidCloseCount.get());
            assertEquals(0, validCloseCount.get());
        } finally {
            WebSocketSessionHolder.removeSession(invalidUserId, "invalid-token");
            WebSocketSessionHolder.removeSession(validUserId, "valid-token");
        }
    }

    private WebSocket socket(boolean valid, AtomicInteger closeCount) {
        return (WebSocket) Proxy.newProxyInstance(WebSocket.class.getClassLoader(),
                new Class<?>[]{WebSocket.class}, (proxy, method, args) -> {
                    if ("isValid".equals(method.getName())) {
                        return valid;
                    }
                    if ("close".equals(method.getName())) {
                        closeCount.incrementAndGet();
                        return null;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == long.class) {
                        return 0L;
                    }
                    return null;
                });
    }
}
