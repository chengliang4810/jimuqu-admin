package com.jimuqu.test.http;

import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.test.coverage.RuntimeRouteCoverage;
import org.noear.solon.Solon;
import org.noear.solon.data.cache.CacheService;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 HTTP 接口测试夹具，并将成功发出的请求记录到运行时路由覆盖率。
 */
public final class HttpApiTestSupport {

    public static final String ADMIN_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin123";
    public static final String PC_CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e";

    private final URI baseUri;
    private final Predicate<RuntimeRouteCoverage.RouteKey> routeSelector;
    private final RuntimeRouteCoverage coverage;

    public HttpApiTestSupport(Predicate<RuntimeRouteCoverage.RouteKey> routeSelector) {
        this.routeSelector = routeSelector;
        int port = Solon.cfg().getInt("server.port", Integer.parseInt(requiredEnvironment("JIMU_TEST_SERVER_PORT")));
        this.baseUri = URI.create("http://127.0.0.1:" + port + "/");

        RuntimeRouteCoverage allRoutes = RuntimeRouteCoverage.snapshotApplicationRoutes(Set.of());
        Set<RuntimeRouteCoverage.RouteKey> exclusions = allRoutes.report().missing().stream()
                .filter(routeSelector.negate())
                .collect(Collectors.toUnmodifiableSet());
        this.coverage = RuntimeRouteCoverage.snapshotApplicationRoutes(exclusions);
        assertFalse(this.coverage.report().missing().isEmpty(), "当前测试分片没有匹配到任何运行时路由");
    }

    public Response get(String path) {
        return request("GET", path, null, null, null);
    }

    public Response get(String path, String token) {
        return request("GET", path, null, null, token);
    }

    /**
     * 建立真实 SSE 连接。调用方必须读取并断言事件，且在结束时关闭连接。
     */
    public SseSubscription openSse(String path, String token) {
        URI uri = baseUri.resolve(path.startsWith("/") ? path.substring(1) : path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "text/event-stream")
                .GET();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            builder.header("clientid", PC_CLIENT_ID);
        }

        try {
            HttpResponse<InputStream> response = newHttpClient().send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            return new SseSubscription(response.statusCode(),
                    response.headers().firstValue("Content-Type").orElse(""), response.body(),
                    coverageRecorder("GET", uri.getPath()));
        } catch (IOException exception) {
            throw new AssertionError("HTTP 流式请求失败: GET " + uri, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP 流式请求被中断: GET " + uri, exception);
        }
    }

    public Response delete(String path) {
        return request("DELETE", path, null, null, null);
    }

    public Response delete(String path, String token) {
        return request("DELETE", path, null, null, token);
    }

    public Response postJson(String path, Object body) {
        return postJson(path, body, null);
    }

    public Response postJson(String path, Object body, String token) {
        return request("POST", path, jsonBody(body), "application/json", token);
    }

    public Response putJson(String path, Object body) {
        return putJson(path, body, null);
    }

    public Response putJson(String path, Object body, String token) {
        return request("PUT", path, jsonBody(body), "application/json", token);
    }

    public Response postEncryptedJson(String path, Object body) {
        return postEncryptedJson(path, body, null);
    }

    public Response postEncryptedJson(String path, Object body, String token) {
        return encryptedJsonRequest("POST", path, body, token);
    }

    public Response putEncryptedJson(String path, Object body, String token) {
        return encryptedJsonRequest("PUT", path, body, token);
    }

    public Response postForm(String path, Map<String, ?> form) {
        return postForm(path, form, null);
    }

    public Response postForm(String path, Map<String, ?> form, String token) {
        return request("POST", path, encodeForm(form), "application/x-www-form-urlencoded", token);
    }

    public Response request(String method, String path, String body, String contentType, String token) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        return sendRequest(method, path, publisher, contentType, token);
    }

    public Response requestBytes(String method, String path, byte[] body, String contentType, String token) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
        return sendRequest(method, path, publisher, contentType, token);
    }

    private Response sendRequest(String method, String path, HttpRequest.BodyPublisher publisher,
                                 String contentType, String token) {
        return sendRequest(method, path, publisher, contentType, token, Map.of());
    }

    private Response sendRequest(String method, String path, HttpRequest.BodyPublisher publisher,
                                 String contentType, String token, Map<String, String> headers) {
        URI uri = baseUri.resolve(path.startsWith("/") ? path.substring(1) : path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            builder.header("clientid", PC_CLIENT_ID);
        }
        headers.forEach(builder::header);

        builder.method(method, publisher);

        try {
            HttpResponse<byte[]> response = newHttpClient().send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            return new Response(response.statusCode(), response.headers().firstValue("Content-Type").orElse(""),
                    response.body(), coverageRecorder(method, uri.getPath()));
        } catch (IOException exception) {
            throw new AssertionError("HTTP 请求失败: " + method + " " + uri, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP 请求被中断: " + method + " " + uri, exception);
        }
    }

    public String loginAdmin() {
        return login(ADMIN_USERNAME, DEFAULT_PASSWORD);
    }

    public String login(String username, String password) {
        Map<String, Object> payload = new LinkedHashMap<>(Map.of(
                "clientId", PC_CLIENT_ID,
                "grantType", "password",
                "username", username,
                "password", password
        ));
        payload.putAll(captchaAnswer());
        Response response = postEncryptedJson("/auth/login", payload).expectSuccess();
        return response.dataString("access_token");
    }

    public Map<String, Object> withCaptcha(Map<String, ?> body) {
        Map<String, Object> payload = new LinkedHashMap<>(body);
        payload.putAll(captchaAnswer());
        return payload;
    }

    private Response encryptedJsonRequest(String method, String path, Object body, String token) {
        String json = jsonBody(body);
        try {
            String aesKey = ApiCryptoUtil.randomAesKey();
            String encryptedKey = ApiCryptoUtil.encryptByRsa(
                    Base64.getEncoder().encodeToString(aesKey.getBytes(StandardCharsets.UTF_8)), requestPublicKey());
            return sendRequest(method, path,
                    HttpRequest.BodyPublishers.ofString(ApiCryptoUtil.encryptByAes(json, aesKey),
                            StandardCharsets.UTF_8),
                    "application/json", token,
                    Map.of(Solon.cfg().get("api-decrypt.headerFlag", "encrypt-key"), encryptedKey));
        } catch (Exception exception) {
            throw new IllegalStateException("无法构造 Bell 加密请求: " + method + " " + path, exception);
        }
    }

    private Map<String, Object> captchaAnswer() {
        CaptchaProperties captchaProperties = Solon.context().getBean(CaptchaProperties.class);
        if (!Boolean.TRUE.equals(captchaProperties.getEnable())) {
            return Map.of();
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String answer = "JIMU";
        CacheService cacheService = Solon.context().getBean(CacheService.class);
        cacheService.store(GlobalConstants.CAPTCHA_CODE_KEY + uuid, answer, Constants.CAPTCHA_EXPIRATION * 60);
        return Map.of("uuid", uuid, "code", answer);
    }

    private static String requestPublicKey() {
        try {
            byte[] encoded = Base64.getDecoder().decode(Solon.cfg().get("api-decrypt.privateKey"));
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
            return Base64.getEncoder().encodeToString(KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()))
                    .getEncoded());
        } catch (Exception exception) {
            throw new IllegalStateException("无法从测试配置推导请求加密公钥", exception);
        }
    }

    public void assertCoverageComplete() {
        coverage.assertComplete();
    }

    public RuntimeRouteCoverage.Report coverageReport() {
        return coverage.report();
    }

    public String baseUrl() {
        return baseUri.toString();
    }

    public static String query(Map<String, ?> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        return parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&", "?", ""));
    }

    private static String jsonBody(Object body) {
        if (body == null) {
            return "{}";
        }
        return body instanceof String text ? text : JsonUtil.toString(body);
    }

    private static String encodeForm(Map<String, ?> form) {
        if (form == null || form.isEmpty()) {
            return "";
        }
        return form.entrySet().stream()
                .flatMap(entry -> {
                    Object value = entry.getValue();
                    if (value instanceof Iterable<?> values) {
                        List<String> encoded = new ArrayList<>();
                        values.forEach(item -> encoded.add(encode(entry.getKey()) + "=" + encode(String.valueOf(item))));
                        return encoded.stream();
                    }
                    return java.util.stream.Stream.of(encode(entry.getKey()) + "=" + encode(String.valueOf(value)));
                })
                .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private Runnable coverageRecorder(String method, String path) {
        RuntimeRouteCoverage.RouteKey requestKey = RuntimeRouteCoverage.RouteKey.of(method, path);
        if (!routeSelector.test(requestKey)) {
            return () -> { };
        }
        return () -> coverage.record(method, path);
    }

    private static String mediaType(String contentType) {
        int separator = contentType.indexOf(';');
        return (separator < 0 ? contentType : contentType.substring(0, separator)).trim().toLowerCase();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少测试环境变量: " + name);
        }
        return value;
    }

    /**
     * HTTP 原始响应及统一契约断言。
     */
    public static final class Response {
        private final int statusCode;
        private final String contentType;
        private final byte[] bytes;
        private final Runnable coverageRecorder;
        private Map<String, Object> json;
        private boolean validated;

        private Response(int statusCode, String contentType, byte[] bytes, Runnable coverageRecorder) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.bytes = bytes == null ? new byte[0] : bytes.clone();
            this.coverageRecorder = coverageRecorder;
        }

        public int statusCode() {
            return statusCode;
        }

        public String contentType() {
            return contentType;
        }

        public byte[] bytes() {
            return bytes.clone();
        }

        public String body() {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> json() {
            if (json == null) {
                Object parsed = JsonUtil.toObject(body(), Map.class);
                assertInstanceOf(Map.class, parsed, "响应不是 JSON 对象: " + body());
                json = new LinkedHashMap<>((Map<String, Object>) parsed);
            }
            return json;
        }

        public Response expectStatus(int expected) {
            assertEquals(expected, statusCode, "HTTP 状态不符合预期，响应: " + body());
            return this;
        }

        public Response expectEnvelope() {
            assertEquals("application/json", mediaType(contentType),
                    "统一响应必须使用 application/json Content-Type，实际为: " + contentType);
            Map<String, Object> payload = json();
            assertEquals(Set.of("code", "msg", "data"), payload.keySet(),
                    "响应必须且只能包含 code、msg、data: " + body());
            assertInstanceOf(Number.class, payload.get("code"), "code 必须为数值");
            assertInstanceOf(String.class, payload.get("msg"), "msg 必须为字符串");
            return this;
        }

        public Response expectSuccess() {
            expectStatus(200);
            return expectCode(200);
        }

        public Response expectCode(int expected) {
            expectEnvelope();
            assertEquals(expected, code(), "业务状态不符合预期，响应: " + body());
            markValidated();
            return this;
        }

        public Response expectFailure(int expectedHttpStatus, int expectedCode, String messagePart) {
            expectStatus(expectedHttpStatus);
            expectEnvelope();
            assertEquals(expectedCode, code(), "业务状态不符合预期，响应: " + body());
            if (messagePart != null && !messagePart.isBlank()) {
                assertTrue(String.valueOf(json().get("msg")).contains(messagePart),
                        "错误消息缺少预期内容 '" + messagePart + "': " + body());
            }
            markValidated();
            return this;
        }

        public int code() {
            Object value = json().get("code");
            assertInstanceOf(Number.class, value, "code 必须为数值");
            return ((Number) value).intValue();
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> dataObject() {
            Object data = json().get("data");
            assertInstanceOf(Map.class, data, "data 必须为 JSON 对象，响应: " + body());
            return (Map<String, Object>) data;
        }

        @SuppressWarnings("unchecked")
        public List<Object> dataList() {
            Object data = json().get("data");
            assertInstanceOf(List.class, data, "data 必须为 JSON 数组，响应: " + body());
            return (List<Object>) data;
        }

        public String dataString(String name) {
            Object value = dataObject().get(name);
            assertInstanceOf(String.class, value, "data." + name + " 必须为字符串");
            assertFalse(((String) value).isBlank(), "data." + name + " 不能为空");
            return (String) value;
        }

        public Response expectPage() {
            expectSuccess();
            Map<String, Object> data = dataObject();
            assertEquals(Set.of("rows", "total"), data.keySet(),
                    "分页 data 必须且只能包含 rows、total: " + body());
            assertInstanceOf(List.class, data.get("rows"), "分页 rows 必须为数组");
            assertInstanceOf(Number.class, data.get("total"), "分页 total 必须为数值");
            List<?> rows = (List<?>) data.get("rows");
            Number total = (Number) data.get("total");
            assertTrue(total.doubleValue() >= 0 && total.doubleValue() == Math.rint(total.doubleValue()),
                    "分页 total 必须为非负整数: " + total);
            assertTrue(total.longValue() >= rows.size(),
                    "分页 total 不得小于当前 rows 数量: " + body());
            return this;
        }

        public Response expectBinary(String expectedMediaType) {
            expectStatus(200);
            assertEquals(expectedMediaType.toLowerCase(), mediaType(contentType),
                    "下载 Content-Type 不符合预期: " + contentType);
            assertFalse("application/json".equals(mediaType(contentType)),
                    "二进制下载不得返回 JSON 错误响应: " + body());
            assertTrue(bytes.length > 0, "下载内容不能为空");
            markValidated();
            return this;
        }

        public Response expectSpreadsheet() {
            expectBinary("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            assertTrue(bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K',
                    "XLSX 下载必须是 ZIP/PK 文件，实际响应: " + body());
            return this;
        }

        private void markValidated() {
            if (!validated) {
                coverageRecorder.run();
                validated = true;
            }
        }
    }

    /** 保持真实 HTTP 连接的 SSE 订阅。 */
    public static final class SseSubscription implements AutoCloseable {
        private final int statusCode;
        private final String contentType;
        private final InputStream input;
        private final BufferedReader reader;
        private final Runnable coverageRecorder;
        private boolean validated;

        private SseSubscription(int statusCode, String contentType, InputStream input, Runnable coverageRecorder) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.input = input;
            this.reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            this.coverageRecorder = coverageRecorder;
        }

        public SseSubscription expectEvent(String expectedName, String expectedData) {
            expectReady();
            SseEvent event = readEvent(Duration.ofSeconds(8));
            assertEquals(expectedName, event.name(), "SSE 事件名不符合预期");
            if (expectedData != null) {
                assertEquals(expectedData, event.data(), "SSE data 不符合预期");
            }
            markValidated();
            return this;
        }

        public SseSubscription expectBellMessage(String expectedType, String expectedSource,
                                                 String expectedMessage) {
            expectReady();
            SseEvent event = readEvent(Duration.ofSeconds(8));
            assertEquals("message", event.name(), "Bell SSE 事件名必须为 message");
            Object parsed = JsonUtil.toObject(event.data(), Map.class);
            assertInstanceOf(Map.class, parsed, "Bell SSE data 必须是 JSON 对象: " + event.data());
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) parsed;
            assertTrue(payload.get("messageId") instanceof String messageId && !messageId.isBlank(),
                    "Bell SSE messageId 必须是非空字符串");
            assertEquals(expectedMessage, payload.get("message"), "Bell SSE message 不符合预期");
            assertEquals(expectedType, payload.get("type"), "Bell SSE type 不符合预期");
            assertEquals(expectedSource, payload.get("source"), "Bell SSE source 不符合预期");
            assertInstanceOf(Number.class, payload.get("timestamp"),
                    "Bell SSE timestamp 必须是 JSON Number");
            markValidated();
            return this;
        }

        private void expectReady() {
            assertEquals(200, statusCode, "SSE HTTP 状态不符合预期");
            assertEquals("text/event-stream", mediaType(contentType),
                    "SSE Content-Type 不符合预期: " + contentType);
        }

        private void markValidated() {
            if (!validated) {
                coverageRecorder.run();
                validated = true;
            }
        }

        private SseEvent readEvent(Duration timeout) {
            CompletableFuture<SseEvent> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return readEventBlocking();
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                close();
                throw new AssertionError("等待 SSE 事件超时", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("等待 SSE 事件被中断", exception);
            } catch (ExecutionException exception) {
                throw new AssertionError("读取 SSE 事件失败", exception.getCause());
            }
        }

        private synchronized SseEvent readEventBlocking() throws IOException {
            String eventName = null;
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (eventName != null || !data.isEmpty()) {
                        return new SseEvent(eventName, data.toString());
                    }
                    continue;
                }
                if (line.startsWith(":")) {
                    continue;
                }
                int separator = line.indexOf(':');
                String field = separator < 0 ? line : line.substring(0, separator);
                String value = separator < 0 ? "" : line.substring(separator + 1);
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                if ("event".equals(field)) {
                    eventName = value;
                } else if ("data".equals(field)) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(value);
                }
            }
            throw new IOException("SSE 连接已关闭，未收到完整事件");
        }

        @Override
        public void close() {
            try {
                input.close();
            } catch (IOException ignored) {
                // 测试清理阶段无需覆盖前序断言错误。
            }
        }
    }

    private record SseEvent(String name, String data) {
    }
}
