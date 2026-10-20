package com.jimuqu.common.web.serialization;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.domain.RsaKeyPair;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.web.config.WebConfig;
import com.jimuqu.common.web.filter.GlobalExceptionFilter;
import com.jimuqu.common.web.filter.FeatHttpEmptyBodyFilter;
import org.apache.fory.json.annotation.JsonIgnore;
import org.apache.fory.json.annotation.JsonProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.event.EventListener;
import tech.smartboot.feat.core.server.ServerOptions;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 使用真实 FeatHTTP 服务验证插件装配、JSON 请求绑定与响应渲染。 */
class ForyJsonHttpTest {
    private static int port;
    private static RsaKeyPair keyPair;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static AsynchronousChannelGroup channelGroup;
    private static final EventListener<ServerOptions> CHANNEL_OBSERVER = options -> channelGroup = options.group();

    @BeforeAll
    static void startServer() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        EventBus.subscribe(ServerOptions.class, 100, CHANNEL_OBSERVER);
        Solon.start(ForyJsonHttpTest.class, new String[]{"--server.port=" + port,
                "--server.http.useJdkAio=true",
                "--solon.config.load=config/common-web.yml"}, app -> {
            app.enableScanning(false);
            app.router().add("/json-test", TestController.class);
            app.router().filter(new FeatHttpEmptyBodyFilter());
            app.router().filter(new GlobalExceptionFilter());
        });
        ForyJsonStringSerializer serializer = Solon.context().getBean(ForyJsonStringSerializer.class);
        assertNotNull(serializer);
        assertEquals(serializer, Solon.app().serializers().get("@json"));
        new WebConfig().sensitiveJsonRender(serializer, (roles, permissions) -> true);
        keyPair = ApiCryptoUtil.generateRsaKeyPair();
        Solon.cfg().setProperty("api-decrypt.enabled", "true");
        Solon.cfg().setProperty("api-decrypt.publicKey", keyPair.getPublicKey());
    }

    @AfterAll
    static void stopServer() {
        CLIENT.close();
        Solon.stopBlock(false, 0);
        EventBus.unsubscribe(CHANNEL_OBSERVER);
    }

    @Test
    void acceptsExplicitZeroLengthBodiesWithoutWaitingForTheConnectionToClose() throws Exception {
        for (String method : List.of("GET", "POST", "PUT", "DELETE", "GET_WITHOUT_BODY_PUBLISHER")) {
            var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/json-test/read-empty-body")).timeout(Duration.ofSeconds(5));
            var request = "GET_WITHOUT_BODY_PUBLISHER".equals(method)
                    ? builder.GET().build() : builder.method(method, HttpRequest.BodyPublishers.noBody()).build();
            var response = CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            Map<?, ?> data = (Map<?, ?>) JsonUtil.toMap(response.body()).get("data");
            assertEquals("", data.get("body"));
        }
        var missing = CLIENT.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/missing-empty-body")).timeout(Duration.ofSeconds(5))
                        .method("GET", HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, JsonUtil.toMap(missing.body()).get("code"));
    }

    @Test
    void parsesEmptyAndNonemptyFormsWhileKeepingQueryParameters() throws Exception {
        for (String body : List.of("", "filter=body+value")) {
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                            + "/json-test/form?query=kept")).timeout(Duration.ofSeconds(5))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            Map<?, ?> data = (Map<?, ?>) JsonUtil.toMap(response.body()).get("data");
            assertEquals("kept", data.get("query"));
            assertEquals(body.isEmpty() ? "" : "body value", data.get("filter"));
        }
    }

    @Test
    void retainsChunkedJsonBodiesWhenNoContentLengthIsDeclared() throws Exception {
        var publisher = HttpRequest.BodyPublishers.fromPublisher(
                HttpRequest.BodyPublishers.ofString("{\"userId\":8}"));
        var response = CLIENT.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/json-test/echo")).timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json").POST(publisher).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Map<?, ?> data = (Map<?, ?>) JsonUtil.toMap(response.body()).get("data");
        assertEquals(8, data.get("userId"));
    }

    @Test
    void usesJdkAioAndKeepsQueryPriorityPostConnectionsReusable() throws Exception {
        assertNotNull(channelGroup);
        assertSame(AsynchronousChannelProvider.provider(), channelGroup.provider());
        for (int i = 0; i < 100; i++) {
            var response = post("/param?id=4", "{\"id\":5}", "application/json");
            assertEquals(200, response.statusCode());
            assertEquals(4, JsonUtil.toMap(response.body()).get("id"));
        }
    }

    @Test
    void bindsAliasesDatesAndExactDecimalThenMasksAndEncodesNumbers() throws Exception {
        var response = post("/echo", """
                {"userId":"350000000000000001","phoneNumber":"13812345678",
                 "password":"secret","amount":12345678901234567890.123400,"createdAt":"2026-10-20 12:30:00"}
                """, "application/json");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        Map<?, ?> body = JsonUtil.toObject(response.body(), Map.class);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertEquals("350000000000000001", data.get("userId"));
        assertEquals("138****5678", data.get("phoneNumber"));
        assertEquals("12345678901234567890.123400", data.get("amount"));
        assertEquals("2026-10-20 12:30:00", data.get("createdAt"));
        assertTrue(data.containsKey("absent"));
        assertNull(data.get("absent"));
        assertFalse(data.containsKey("password"));
        assertFalse(data.containsKey("id"));
    }

    @Test
    void formatsNestedDatesThroughTheConfiguredHttpSerializer() throws Exception {
        var response = CLIENT.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/json-test/dates")).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Map<?, ?> body = JsonUtil.toObject(response.body(), Map.class);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertEquals("1970-01-01 08:00:00", data.get("date"));
        assertEquals(List.of("1970-01-01 08:00:00"), data.get("nested"));
    }

    @Test
    void bindsGenericListsAndNamedParametersWithoutLosingPrecision() throws Exception {
        String value = "12345678901234567890.123400";
        var list = post("/list", "[{\"userId\":7,\"amount\":" + value + "}]", "application/json");
        assertTrue(list.body().contains("\"amount\":\"" + value + "\""), list.body());
        var wrapped = post("/wrapped", "{\"rows\":[{\"userId\":8,\"amount\":" + value + "}]}", "application/json");
        assertTrue(wrapped.body().contains("\"userId\":8"), wrapped.body());
        assertTrue(wrapped.body().contains(value), wrapped.body());
    }

    @Test
    void keepsQueryPrecedenceAndSupportsJsonMediaTypeSuffix() throws Exception {
        var response = post("/param?id=4", "{\"id\":5}", "application/problem+json");
        Map<?, ?> body = JsonUtil.toObject(response.body(), Map.class);
        assertEquals(4, body.get("id"));
    }

    @Test
    void malformedAndTrailingInputUseExistingBadRequestEnvelope() throws Exception {
        for (String text : List.of("{", "{\"userId\":7} trailing", "{\"userId\":7,}")) {
            var response = post("/echo", text, "application/json");
            assertEquals(200, response.statusCode());
            Map<?, ?> body = JsonUtil.toObject(response.body(), Map.class);
            assertEquals(400, body.get("code"), response.body());
            assertEquals("请求数据格式错误", body.get("msg"));
        }
    }

    @Test
    void encryptsTheMaskedJsonResponseWithTheExistingHeaderProtocol() throws Exception {
        var response = post("/encrypted", "{\"phoneNumber\":\"13812345678\"}", "application/json");
        String encryptedKey = response.headers().firstValue("encrypt-key").orElseThrow();
        String json = ApiCryptoUtil.decryptRequest(response.body(), encryptedKey, keyPair.getPrivateKey());
        Map<?, ?> body = JsonUtil.toObject(json, Map.class);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertEquals("138****5678", data.get("phoneNumber"));
        assertTrue(response.headers().firstValue("Access-Control-Expose-Headers").orElse("")
                .contains("encrypt-key"));
    }

    private static HttpResponse<String> post(String path, String body, String mime) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/json-test" + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", mime)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Controller
    public static class TestController {
        @Mapping("/form")
        public R<Map<String, String>> form(org.noear.solon.core.handle.Context ctx) throws Exception {
            String query = ctx.param("query");
            String filter = ctx.paramOrDefault("filter", "");
            ctx.bodyNew();
            return R.ok(Map.of("query", query, "filter", filter));
        }

        @Mapping("/read-empty-body")
        public R<Map<String, String>> readEmptyBody(org.noear.solon.core.handle.Context ctx) throws Exception {
            return R.ok(Map.of("body", ctx.bodyNew()));
        }

        @Mapping("/dates")
        public R<Map<String, Object>> dates() {
            return R.ok(Map.of("date", new Date(0), "nested", List.of(new Date(0))));
        }

        @Mapping(value = "/echo", method = MethodType.POST)
        public R<Payload> echo(@Body Payload value) {
            return R.ok(value);
        }

        @ApiEncrypt(response = true)
        @Mapping(value = "/encrypted", method = MethodType.POST)
        public R<Payload> encrypted(@Body Payload value) {
            return R.ok(value);
        }

        @Mapping(value = "/list", method = MethodType.POST)
        public R<List<Payload>> list(@Body List<Payload> rows) {
            return R.ok(rows);
        }

        @Mapping(value = "/wrapped", method = MethodType.POST)
        public R<List<Payload>> wrapped(List<Payload> rows) {
            return R.ok(rows);
        }

        @Mapping(value = "/param", method = MethodType.POST)
        public Map<String, Object> param(Long id) {
            return Map.of("id", id);
        }
    }

    public static class Payload {
        @JsonProperty("userId")
        public Long id;
        @JsonProperty("phoneNumber")
        @Sensitive(type = SensitiveType.MOBILE)
        public String phone;
        @JsonIgnore
        public String password;
        public BigDecimal amount;
        public Date createdAt;
        public String absent;
    }
}
