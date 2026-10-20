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

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 使用真实 FeatHTTP 服务验证插件装配、JSON 请求绑定与响应渲染。 */
class ForyJsonHttpTest {
    private static int port;
    private static RsaKeyPair keyPair;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        Solon.start(ForyJsonHttpTest.class, new String[]{"--server.port=" + port}, app -> {
            app.enableScanning(false);
            app.router().add("/json-test", TestController.class);
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
        Solon.stopBlock(false, 0);
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
        assertInstanceOf(Number.class, data.get("createdAt"));
        assertTrue(data.containsKey("absent"));
        assertNull(data.get("absent"));
        assertFalse(data.containsKey("password"));
        assertFalse(data.containsKey("id"));
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
