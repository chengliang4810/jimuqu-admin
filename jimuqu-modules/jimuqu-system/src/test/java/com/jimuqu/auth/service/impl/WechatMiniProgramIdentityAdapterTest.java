package com.jimuqu.auth.service.impl;

import com.jimuqu.auth.config.properties.MiniProgramProperties;
import com.jimuqu.auth.service.MiniProgramIdentityAdapter;
import com.jimuqu.common.core.exception.ServiceException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatMiniProgramIdentityAdapterTest {

    @Test
    void exchangesCodeThroughTheConfiguredWechatContract() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = server(query);
        try {
            MiniProgramProperties properties = properties(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/sns/jscode2session");
            WechatMiniProgramIdentityAdapter adapter = new WechatMiniProgramIdentityAdapter(properties);

            MiniProgramIdentityAdapter.MiniProgramIdentity identity =
                    adapter.authenticate("wx-app", "temporary code");

            assertEquals("openid-1", identity.openId());
            assertEquals("union-1", identity.unionId());
            assertTrue(query.get().contains("appid=wx-app"));
            assertTrue(query.get().contains("secret=app-secret"));
            assertTrue(query.get().contains("js_code=temporary+code"));
            assertTrue(query.get().contains("grant_type=authorization_code"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void propagatesProviderErrorsWithoutLeakingCredentials() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = server(query);
        try {
            WechatMiniProgramIdentityAdapter adapter = new WechatMiniProgramIdentityAdapter(
                    properties("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/sns/jscode2session?failure=true"));

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> adapter.authenticate("wx-app", "bad-code"));

            assertTrue(exception.getMessage().contains("invalid code"));
            assertFalse(exception.getMessage().contains("app-secret"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void remainsUnavailableUntilEnabledCredentialsExist() {
        MiniProgramProperties properties = new MiniProgramProperties();
        WechatMiniProgramIdentityAdapter adapter = new WechatMiniProgramIdentityAdapter(properties);

        assertFalse(adapter.isAvailable());
        assertThrows(ServiceException.class, () -> adapter.authenticate(null, "code"));

        properties.setEnabled(true);
        assertFalse(adapter.isAvailable());
    }

    private HttpServer server(AtomicReference<String> query) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sns/jscode2session", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            String body = query.get().contains("failure=true")
                    ? "{\"errcode\":40029,\"errmsg\":\"invalid code\"}"
                    : "{\"openid\":\"openid-1\",\"unionid\":\"union-1\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private MiniProgramProperties properties(String endpoint) {
        MiniProgramProperties.App app = new MiniProgramProperties.App();
        app.setAppid("wx-app");
        app.setSecret("app-secret");
        MiniProgramProperties properties = new MiniProgramProperties();
        properties.setEnabled(true);
        properties.setEndpoint(endpoint);
        properties.setApps(new LinkedHashMap<>());
        properties.getApps().put("primary", app);
        return properties;
    }
}
