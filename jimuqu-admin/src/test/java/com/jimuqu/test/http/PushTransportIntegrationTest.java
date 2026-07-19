package com.jimuqu.test.http;

import com.jimuqu.Application;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.system.domain.bo.SysClientBo;
import com.jimuqu.system.domain.vo.SysMessageVo;
import com.jimuqu.system.domain.vo.SysClientVo;
import com.jimuqu.system.service.SysClientService;
import com.jimuqu.system.service.SysMessageService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.noear.solon.Solon;
import org.noear.solon.test.SolonTest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.jimuqu.common.websocket.holder.WebSocketSessionHolder.REPLACED_CLOSE_CODE;
import static com.jimuqu.common.websocket.holder.WebSocketSessionHolder.REPLACED_CLOSE_REASON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Redis、SSE 与 WebSocket 之间的真实传输契约。 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PushTransportIntegrationTest {

    private HttpApiTestSupport api;
    private String adminToken;
    private String deniedToken;
    private SysMessageService messageService;
    private SysClientService clientService;
    private final List<Long> temporaryClientIds = new ArrayList<>();
    private RestrictedToken pathDeniedToken;
    private RestrictedToken ipRestrictedToken;
    private RestrictedToken allowedToken;

    @BeforeAll
    void setUp() {
        api = new HttpApiTestSupport(key -> key.path().startsWith("/resource/message"));
        adminToken = api.loginAdmin();
        deniedToken = api.login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);
        messageService = Solon.context().getBean(SysMessageService.class);
        clientService = Solon.context().getBean(SysClientService.class);
        pathDeniedToken = createRestrictedToken("path", List.of("/system/**"), List.of("127.0.0.1"));
        ipRestrictedToken = createRestrictedToken("ip", List.of("/resource/**"), List.of("203.0.113.10"));
        allowedToken = createRestrictedToken("allowed", List.of("/resource/**"), List.of("127.0.0.1"));
        awaitLoginWelcomePersisted(adminToken);
        awaitLoginWelcomePersisted(deniedToken);
    }

    @AfterAll
    void cleanUpClients() {
        if (!temporaryClientIds.isEmpty()) {
            clientService.deleteByIds(temporaryClientIds);
        }
    }

    @Test
    void routesTargetedAndBroadcastMessagesAcrossRedis() throws Exception {
        api.get("/resource/message").expectStatus(401).expectCode(401);

        try (HttpApiTestSupport.SseSubscription replaced = api.openSse("/resource/message", adminToken)) {
            replaced.expectEvent("connected", "");
            try (HttpApiTestSupport.SseSubscription current = api.openSse("/resource/message", adminToken)) {
                current.expectEvent("connected", "");
                replaced.expectEvent("kicked", "");

                SocketListener denied = new SocketListener();
                WebSocket deniedSocket = openSocket(deniedToken, denied);
                SocketListener oldAdmin = new SocketListener();
                WebSocket replacedSocket = openSocket(adminToken, oldAdmin);
                SocketListener admin = new SocketListener();
                WebSocket adminSocket = openSocket(adminToken, admin);
                CloseFrame replacedClose = oldAdmin.closed.get(10, TimeUnit.SECONDS);
                assertEquals(REPLACED_CLOSE_CODE, replacedClose.code());
                assertEquals(REPLACED_CLOSE_REASON, replacedClose.reason());
                assertNull(oldAdmin.messages.poll(1, TimeUnit.SECONDS),
                        "WebSocket 连接替换控制信号不得进入 Bell 业务消息流");

                SocketListener invalid = new SocketListener();
                WebSocket invalidSocket = openSocket("invalid-token", invalid);
                assertEquals(1007, invalid.closed.get(10, TimeUnit.SECONDS).code());
                invalidSocket.abort();

                SocketListener wrongClient = new SocketListener();
                WebSocket wrongClientSocket = openSocket(adminToken, "wrong-client", wrongClient);
                assertEquals(1007, wrongClient.closed.get(10, TimeUnit.SECONDS).code(),
                        "WebSocket 必须校验 token 绑定的 clientid");
                wrongClientSocket.abort();

                SocketListener missingClient = new SocketListener();
                WebSocket missingClientSocket = openSocket(adminToken, null, missingClient);
                assertEquals(1007, missingClient.closed.get(10, TimeUnit.SECONDS).code(),
                        "WebSocket 缺少 clientid 时必须拒绝握手");
                missingClientSocket.abort();

                SocketListener deniedPath = new SocketListener();
                WebSocket deniedPathSocket = openSocket(pathDeniedToken.token(), pathDeniedToken.clientId(), deniedPath);
                assertEquals(1007, deniedPath.closed.get(10, TimeUnit.SECONDS).code(),
                        "客户端访问路径不包含 /resource/websocket 时必须拒绝握手");
                deniedPathSocket.abort();

                SocketListener deniedIp = new SocketListener();
                WebSocket deniedIpSocket = openSocket(ipRestrictedToken.token(), ipRestrictedToken.clientId(), deniedIp);
                assertEquals(1007, deniedIp.closed.get(10, TimeUnit.SECONDS).code(),
                        "客户端 IP 白名单不匹配直连地址时必须拒绝握手");
                deniedIpSocket.abort();

                SocketListener spoofedIp = new SocketListener();
                WebSocket spoofedIpSocket = openSocket(ipRestrictedToken.token(), ipRestrictedToken.clientId(),
                        Map.of("X-Forwarded-For", "203.0.113.10"), Map.of(), spoofedIp);
                assertEquals(1007, spoofedIp.closed.get(10, TimeUnit.SECONDS).code(),
                        "查询参数不得冒充 X-Forwarded-For 绕过 IP 白名单");
                spoofedIpSocket.abort();

                SocketListener forwardedIp = new SocketListener();
                WebSocket forwardedIpSocket = openSocket(ipRestrictedToken.token(), ipRestrictedToken.clientId(),
                        Map.of(), Map.of("X-Forwarded-For", "unknown, 203.0.113.10"), forwardedIp);
                forwardedIpSocket.sendText("ping", true).get(10, TimeUnit.SECONDS);
                assertEquals("{\"type\":\"pong\"}", forwardedIp.messages.poll(10, TimeUnit.SECONDS),
                        "可信代理头中的首个有效 IP 应按上游规则通过白名单");
                forwardedIpSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
                assertEquals(WebSocket.NORMAL_CLOSURE, forwardedIp.closed.get(10, TimeUnit.SECONDS).code());

                SocketListener allowed = new SocketListener();
                WebSocket allowedSocket = openSocket(allowedToken.token(), allowedToken.clientId(), allowed);
                allowedSocket.sendText("{\"type\":\"ping\"}", true).get(10, TimeUnit.SECONDS);
                assertEquals("{\"type\":\"pong\"}", allowed.messages.poll(10, TimeUnit.SECONDS),
                        "路径、clientid 与直连 IP 均匹配时应成功握手并收发消息");
                allowedSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
                assertEquals(WebSocket.NORMAL_CLOSURE, allowed.closed.get(10, TimeUnit.SECONDS).code());

                String kickedToken = api.loginAdmin();
                api.delete("/monitor/online/" + URLEncoder.encode(kickedToken, StandardCharsets.UTF_8), adminToken)
                        .expectSuccess();
                SocketListener kicked = new SocketListener();
                WebSocket kickedSocket = openSocket(kickedToken, kicked);
                assertEquals(1007, kicked.closed.get(10, TimeUnit.SECONDS).code(),
                        "已踢出的 token 即使 token-session 尚存也不得重新握手");
                kickedSocket.abort();

                SysMessageVo targeted = message("targeted", "target-" + System.nanoTime());
                messageService.publishMessage(List.of(1L), targeted);
                current.expectBellMessageAfterLoginWelcome("message", "backend", targeted.getMessage());
                assertPayloadAfterLoginWelcome(admin, "message", "backend", targeted.getMessage());
                assertMessageNotReceived(denied, targeted.getMessage());
                assertBoxContains(adminToken, targeted.getMessage(), true);
                assertBoxContains(deniedToken, targeted.getMessage(), false);

                SysMessageVo broadcast = message("broadcast", "broadcast-" + System.nanoTime());
                messageService.publishAll(broadcast);
                current.expectBellMessageAfterLoginWelcome("message", "backend", broadcast.getMessage());
                assertPayloadAfterLoginWelcome(admin, "message", "backend", broadcast.getMessage());
                assertPayloadAfterLoginWelcome(denied, "message", "backend", broadcast.getMessage());

                adminSocket.sendText("{\"type\":\"ping\"}", true).get(10, TimeUnit.SECONDS);
                assertEquals("{\"type\":\"pong\"}", admin.messages.poll(10, TimeUnit.SECONDS));
                adminSocket.sendText("client-message", true).get(10, TimeUnit.SECONDS);
                assertPayload(admin.messages.poll(10, TimeUnit.SECONDS), "custom", "client", "client-message");
                current.expectBellMessage("custom", "client", "client-message");

                assertSseHeartbeat(deniedToken);

                api.get("/resource/message/close", adminToken).expectSuccess();
                current.expectEvent("disconnected", "");
                adminSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
                deniedSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
                assertEquals(WebSocket.NORMAL_CLOSURE, admin.closed.get(10, TimeUnit.SECONDS).code());
                assertEquals(WebSocket.NORMAL_CLOSURE, denied.closed.get(10, TimeUnit.SECONDS).code());
                replacedSocket.abort();
            }

            try (HttpApiTestSupport.SseSubscription reconnected = api.openSse("/resource/message", adminToken)) {
                reconnected.expectEvent("connected", "");
                SocketListener reconnectedListener = new SocketListener();
                WebSocket reconnectedSocket = openSocket(adminToken, reconnectedListener);
                reconnectedSocket.sendText("{\"type\":\"ping\"}", true).get(10, TimeUnit.SECONDS);
                assertEquals("{\"type\":\"pong\"}", reconnectedListener.messages.poll(10, TimeUnit.SECONDS));

                SysMessageVo reconnectMessage = message("reconnected", "reconnected-" + System.nanoTime());
                messageService.publishMessage(List.of(1L), reconnectMessage);
                reconnected.expectBellMessage("message", "backend", reconnectMessage.getMessage());
                assertPayload(reconnectedListener.messages.poll(10, TimeUnit.SECONDS),
                        "message", "backend", reconnectMessage.getMessage());

                reconnectedSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
                api.get("/resource/message/close", adminToken).expectSuccess();
                reconnected.expectEvent("disconnected", "");
            }
        }
    }

    private void assertSseHeartbeat(String token) throws Exception {
        int port = Integer.parseInt(System.getenv("JIMU_TEST_SERVER_PORT"));
        URI uri = URI.create("http://127.0.0.1:" + port + "/resource/message?clientid=push-contract");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + token)
                .header("clientid", HttpApiTestSupport.PC_CLIENT_ID)
                .GET().build();
        HttpResponse<InputStream> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, response.statusCode());
        InputStream input = response.body();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        CompletableFuture<Boolean> heartbeat = CompletableFuture.supplyAsync(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equals(":heartbeat")) {
                        return true;
                    }
                }
                return false;
            } catch (Exception exception) {
                return false;
            }
        });
        try {
            assertTrue(heartbeat.get(8, TimeUnit.SECONDS), "SSE 必须定期发送注释型 heartbeat");
        } finally {
            // 直接关闭底层流以解除异步 readLine；关闭 BufferedReader 会等待其内部锁并可能死锁。
            input.close();
            heartbeat.cancel(true);
        }
    }

    private WebSocket openSocket(String token, SocketListener listener) throws Exception {
        return openSocket(token, HttpApiTestSupport.PC_CLIENT_ID, listener);
    }

    private WebSocket openSocket(String token, String clientId, SocketListener listener) throws Exception {
        return openSocket(token, clientId, Map.of(), Map.of(), listener);
    }

    private WebSocket openSocket(String token, String clientId, Map<String, String> queryParameters,
                                 Map<String, String> headers, SocketListener listener) throws Exception {
        int port = Integer.parseInt(System.getenv("JIMU_TEST_SERVER_PORT"));
        List<String> parameters = new ArrayList<>();
        if (clientId != null) {
            parameters.add("clientid=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        }
        queryParameters.forEach((name, value) -> parameters.add(
                URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        parameters.add("Authorization=Bearer%20" + URLEncoder.encode(token, StandardCharsets.UTF_8));
        URI uri = URI.create("ws://127.0.0.1:" + port + "/resource/websocket?" + String.join("&", parameters));
        WebSocket.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                .newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10));
        headers.forEach(builder::header);
        WebSocket socket = builder.buildAsync(uri, listener).get(10, TimeUnit.SECONDS);
        socket.request(1);
        return socket;
    }

    private RestrictedToken createRestrictedToken(String label, List<String> accessPaths, List<String> ipWhitelist) {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36);
        SysClientBo bo = new SysClientBo()
                .setClientKey("ws_" + label + "_" + suffix)
                .setClientSecret("secret_" + suffix)
                .setGrantTypeList(List.of("password"))
                .setDeviceType("pc")
                .setAccessPathList(accessPaths)
                .setIpWhitelistList(ipWhitelist)
                .setActiveTimeout(1800L)
                .setTimeout(3600L)
                .setStatus("0");
        assertTrue(clientService.insertByBo(bo), "WebSocket 契约测试客户端创建失败");
        temporaryClientIds.add(bo.getId());
        SysClientVo client = clientService.queryById(bo.getId());
        String token = api.postEncryptedJson("/auth/login", api.withCaptcha(Map.of(
                        "clientId", client.getClientId(),
                        "grantType", "password",
                        "username", "dept_user",
                        "password", HttpApiTestSupport.DEFAULT_PASSWORD)))
                .expectSuccess().dataString("access_token");
        return new RestrictedToken(client.getClientId(), token);
    }

    private SysMessageVo message(String title, String text) {
        long now = System.currentTimeMillis();
        return new SysMessageVo().setCategory("system").setType("message").setSource("backend")
                .setTitle(title).setMessage(text).setTimestamp(now);
    }

    @SuppressWarnings("unchecked")
    private void assertBoxContains(String token, String message, boolean expected) {
        Object systemList = api.get("/resource/message/box", token).expectSuccess().dataObject().get("systemList");
        assertTrue(systemList instanceof List<?>);
        boolean found = ((List<Object>) systemList).stream().anyMatch(item -> item instanceof Map<?, ?> map
                && message.equals(map.get("message")));
        assertEquals(expected, found);
    }

    @SuppressWarnings("unchecked")
    private void awaitLoginWelcomePersisted(String token) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        do {
            Object systemList = api.get("/resource/message/box", token).expectSuccess().dataObject().get("systemList");
            assertTrue(systemList instanceof List<?>);
            boolean found = ((List<Object>) systemList).stream().anyMatch(item -> item instanceof Map<?, ?> map
                    && String.valueOf(map.get("message")).contains("欢迎登录积木区后台管理系统"));
            if (found) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("等待登录欢迎消息时被中断", exception);
            }
        } while (System.nanoTime() < deadline);
        throw new AssertionError("登录欢迎消息未在 10 秒内持久化");
    }

    private void assertPayload(String text, String type, String source, String message) {
        Map<?, ?> payload = JsonUtil.toObject(text, Map.class);
        assertEquals(type, payload.get("type"));
        assertEquals(source, payload.get("source"));
        assertEquals(message, payload.get("message"));
        assertTrue(payload.get("messageId") instanceof String id && !id.isBlank());
        assertTrue(payload.get("timestamp") instanceof Number);
    }

    private void assertMessageNotReceived(SocketListener listener, String forbiddenMessage) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadline) {
            String text = listener.messages.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            if (text == null) {
                return;
            }
            Map<?, ?> payload = JsonUtil.toObject(text, Map.class);
            assertFalse(forbiddenMessage.equals(payload.get("message")), "目标用户消息不得泄露给其他用户");
        }
    }

    private void assertPayloadAfterLoginWelcome(SocketListener listener, String type, String source, String message)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            String text = listener.messages.poll(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (text == null) {
                break;
            }
            Map<?, ?> payload = JsonUtil.toObject(text, Map.class);
            if (message.equals(payload.get("message"))) {
                assertPayload(text, type, source, message);
                return;
            }
            assertTrue(String.valueOf(payload.get("message")).contains("欢迎登录积木区后台管理系统"),
                    "等待目标消息时收到非登录欢迎消息: " + payload);
        }
        throw new AssertionError("等待 WebSocket 目标消息超时: " + message);
    }

    private static final class SocketListener implements WebSocket.Listener {
        private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final CompletableFuture<CloseFrame> closed = new CompletableFuture<>();
        private final StringBuilder text = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            text.append(data);
            if (last) {
                messages.add(text.toString());
                text.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(new CloseFrame(statusCode, reason));
            return null;
        }
    }

    private record CloseFrame(int code, String reason) {
    }

    private record RestrictedToken(String clientId, String token) {
    }
}
