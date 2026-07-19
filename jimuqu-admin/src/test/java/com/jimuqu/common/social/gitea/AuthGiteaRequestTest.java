package com.jimuqu.common.social.gitea;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthGiteaRequestTest {

    @Test
    void buildsEndpointsFromConfiguredServer() {
        var source = AuthGiteaRequest.source("https://git.example.test/");
        assertEquals("https://git.example.test/login/oauth/authorize", source.authorize());
        assertEquals("https://git.example.test/login/oauth/access_token", source.accessToken());
        assertEquals("https://git.example.test/login/oauth/userinfo", source.userInfo());
        assertEquals("GITEA", source.getName());
    }

    @Test
    void exchangesAuthorizationCodeAndMapsUserInfo() throws Exception {
        AtomicReference<String> tokenForm = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/login/oauth/access_token", exchange -> {
            tokenForm.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "{\"access_token\":\"access-token\",\"refresh_token\":\"refresh-token\",\"token_type\":\"bearer\"}");
        });
        server.createContext("/login/oauth/userinfo", exchange -> {
            assertEquals("Bearer access-token", exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, "{\"sub\":\"42\",\"name\":\"alice\",\"preferred_username\":\"Alice\",\"picture\":\"https://git.example/avatar.png\",\"email\":\"alice@example.test\"}");
        });
        server.start();
        try {
            String serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            AuthConfig config = AuthConfig.builder()
                    .clientId("client id")
                    .clientSecret("secret/value")
                    .redirectUri("https://admin.example.test/callback?source=gitea")
                    .build();
            AuthGiteaRequest request = new AuthGiteaRequest(config, null, serverUrl);
            AuthCallback callback = new AuthCallback();
            callback.setCode("code value");

            var token = request.getAccessToken(callback);
            var user = request.getUserInfo(token);

            assertEquals("access-token", token.getAccessToken());
            assertTrue(tokenForm.get().contains("client_id=client+id"));
            assertTrue(tokenForm.get().contains("client_secret=secret%2Fvalue"));
            assertTrue(tokenForm.get().contains("code=code+value"));
            assertEquals("42", user.getUuid());
            assertEquals("alice", user.getUsername());
            assertEquals("Alice", user.getNickname());
            assertEquals("alice@example.test", user.getEmail());
            assertEquals("GITEA", user.getSource());
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
