package com.jimuqu.test.http;

import com.jimuqu.Application;
import com.jimuqu.common.core.utils.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.noear.solon.test.SolonTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 字典、配置、客户端、通知和消息盒子的真实 HTTP 契约。 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigurationMessagingHttpContractTest {

    private static final long MISSING_ID = 9_223_372_036_854_775_000L;

    private HttpApiTestSupport api;
    private String adminToken;
    private String deniedToken;
    private String suffix;

    @BeforeAll
    void setUp() {
        api = new HttpApiTestSupport(ConfigurationMessagingHttpContractTest::ownsRoute);
        adminToken = api.loginAdmin();
        deniedToken = api.login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);
        suffix = Long.toUnsignedString(System.nanoTime(), 36);
    }

    static boolean ownsRoute(com.jimuqu.test.coverage.RuntimeRouteCoverage.RouteKey key) {
        return key.path().startsWith("/system/dict/")
                || key.path().startsWith("/system/config")
                || key.path().startsWith("/system/client")
                || key.path().startsWith("/system/notice")
                || key.path().startsWith("/resource/message");
    }

    @AfterAll
    void assertRouteCoverage() {
        api.assertCoverageComplete();
    }

    @Test
    @Order(1)
    void rejectsUnauthenticatedAndUnprivilegedAccess() {
        String deniedTitle = "拒绝写入-" + suffix;
        api.get("/system/config/list?pageNum=1&pageSize=10")
                .expectStatus(401)
                .expectCode(401);
        api.get("/system/dict/type/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.get("/system/client/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.postJson("/system/notice", noticePayload(null, deniedTitle), deniedToken)
                .expectStatus(403)
                .expectCode(403);
        assertTrue(rows(api.get("/system/notice/list" + HttpApiTestSupport.query(Map.of(
                "noticeTitle", deniedTitle, "pageNum", 1, "pageSize", 10)), adminToken).expectPage()).isEmpty(),
                "无权限公告写入不得改变数据库");
    }

    @Test
    @Order(2)
    void exercisesDictionaryRoutes() {
        String dictKey = "http_dict_" + suffix;
        String dictName = "HTTP字典-" + suffix;
        String dictLabel = "HTTP标签-" + suffix;

        HttpApiTestSupport.Response typeAdd = api.postJson("/system/dict/type",
                dictTypePayload(null, dictKey, dictName), adminToken).expectSuccess();
        assertNull(typeAdd.json().get("data"), "Bell 新增字典类型响应 data 必须为 null");
        HttpApiTestSupport.Response typePage = api.get("/system/dict/type/list" + HttpApiTestSupport.query(Map.of(
                "dictType", dictKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        long dictId = number(pageRow(typePage, "dictType", dictKey).get("dictId"));
        api.get("/system/dict/type/" + dictId, adminToken).expectSuccess();
        api.get("/system/dict/type/optionselect", deniedToken).expectSuccess();

        HttpApiTestSupport.Response dataAdd = api.postJson("/system/dict/data",
                dictDataPayload(null, dictKey, dictLabel, "v1"), adminToken).expectSuccess();
        assertNull(dataAdd.json().get("data"), "Bell 新增字典数据响应 data 必须为 null");
        HttpApiTestSupport.Response dataPage = api.get("/system/dict/data/list" + HttpApiTestSupport.query(Map.of(
                "dictType", dictKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        long dataId = number(pageRow(dataPage, "dictValue", "v1").get("dictCode"));
        HttpApiTestSupport.Response duplicateData = api.postJson("/system/dict/data",
                dictDataPayload(null, dictKey, "重复标签-" + suffix, "v1"), adminToken).expectEnvelope();
        assertNotEquals(200, duplicateData.code(), "同一类型下字典键值不得重复");
        assertEquals(1, rows(api.get("/system/dict/data/list" + HttpApiTestSupport.query(Map.of(
                "dictType", dictKey, "dictValue", "v1", "pageNum", 1, "pageSize", 20)),
                adminToken).expectPage()).size(), "重复字典数据失败后不得产生额外记录");

        HttpApiTestSupport.Response assignedType = api.delete("/system/dict/type/" + dictId, adminToken)
                .expectEnvelope();
        assertNotEquals(200, assignedType.code(), "仍有字典数据的类型不得删除");
        api.get("/system/dict/data/list" + HttpApiTestSupport.query(Map.of(
                "dictType", dictKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        api.get("/system/dict/data/" + dataId, adminToken).expectSuccess();
        List<Object> values = api.get("/system/dict/data/type/" + dictKey, deniedToken)
                .expectSuccess().dataList();
        assertTrue(values.stream().anyMatch(item -> item instanceof Map<?, ?> map
                && dictLabel.equals(map.get("dictLabel"))), "按类型查询必须返回新建字典项");

        String renamedKey = dictKey + "_renamed";
        api.putJson("/system/dict/type", dictTypePayload(dictId, renamedKey, dictName + "-改"), adminToken)
                .expectSuccess();
        assertEquals(1, api.get("/system/dict/data/type/" + renamedKey, deniedToken)
                .expectSuccess().dataList().size(), "修改字典类型时必须同步已有字典数据");
        api.putJson("/system/dict/data", dictDataPayload(dataId, renamedKey, dictLabel + "-改", "v2"), adminToken)
                .expectSuccess();
        List<Object> updatedValues = api.get("/system/dict/data/type/" + renamedKey, deniedToken)
                .expectSuccess().dataList();
        assertTrue(updatedValues.stream().anyMatch(item -> item instanceof Map<?, ?> map
                && "v2".equals(map.get("dictValue"))
                && (dictLabel + "-改").equals(map.get("dictLabel"))), "字典修改后必须立即刷新读缓存");

        HttpApiTestSupport.Response duplicate = api.postJson("/system/dict/type",
                dictTypePayload(null, renamedKey, "重复字典-" + suffix), adminToken).expectEnvelope();
        assertNotEquals(200, duplicate.code(), "重复字典类型不得写入");
        assertEquals(1, rows(api.get("/system/dict/type/list" + HttpApiTestSupport.query(Map.of(
                "dictType", renamedKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).size(),
                "重复字典类型失败后不得产生额外记录");

        api.postForm("/system/dict/type/export", Map.of("dictType", dictKey), adminToken)
                .expectSpreadsheet();
        api.postForm("/system/dict/data/export", Map.of("dictType", dictKey), adminToken)
                .expectSpreadsheet();
        api.delete("/system/dict/type/refreshCache", deniedToken).expectFailure(403, 403, null);
        api.delete("/system/dict/type/refreshCache", adminToken).expectSuccess();

        String invalidDictName = "非法字典-" + suffix;
        HttpApiTestSupport.Response invalid = api.postJson("/system/dict/type",
                Map.of("dictType", "", "dictName", invalidDictName), adminToken).expectEnvelope();
        assertNotEquals(200, invalid.code(), "空字典字段不得写入");
        assertTrue(rows(api.get("/system/dict/type/list" + HttpApiTestSupport.query(Map.of(
                "dictName", invalidDictName, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).isEmpty(),
                "非法字典类型失败后不得留下记录");

        assertNull(api.delete("/system/dict/data/" + dataId, adminToken)
                .expectSuccess().json().get("data"), "Bell 删除字典数据响应 data 必须为 null");
        assertNull(api.delete("/system/dict/type/" + dictId, adminToken)
                .expectSuccess().json().get("data"), "Bell 删除字典类型响应 data 必须为 null");
    }

    @Test
    @Order(3)
    void exercisesConfigurationRoutes() {
        String configKey = "http.config." + suffix;
        String configName = "HTTP配置-" + suffix;
        assertEquals("true", api.get("/system/config/configKey/sys.oss.previewListResource", deniedToken)
                .expectSuccess().json().get("data"));
        HttpApiTestSupport.Response configAdd = api.postJson("/system/config",
                configPayload(null, configName, configKey, "v1"), adminToken).expectSuccess();
        assertNull(configAdd.json().get("data"), "Bell 新增参数响应 data 必须为 null");

        HttpApiTestSupport.Response configPage = api.get("/system/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", configKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        long configId = number(pageRow(configPage, "configKey", configKey).get("configId"));
        HttpApiTestSupport.Response futurePage = api.get("/system/config/list"
                + HttpApiTestSupport.query(Map.of(
                "configKey", configKey,
                "pageNum", 1,
                "pageSize", 20,
                "params[beginTime]", "2999-01-01 00:00:00",
                "params[endTime]", "2999-12-31 23:59:59")), adminToken).expectPage();
        assertTrue(rows(futurePage).isEmpty(), "Bell 配置创建时间范围必须实际参与查询");
        HttpApiTestSupport.Response configDetail = api.get("/system/config/" + configId, adminToken).expectSuccess();
        assertEquals(configId, number(configDetail.dataObject().get("configId")));
        assertEquals("v1", api.get("/system/config/configKey/" + configKey, deniedToken)
                .expectSuccess().json().get("data"));

        api.putJson("/system/config",
                configPayload(configId, configName + "-改", configKey, "v2"), adminToken).expectSuccess();
        assertEquals("v2", api.get("/system/config/configKey/" + configKey, adminToken)
                .expectSuccess().json().get("data"));
        api.putJson("/system/config/updateByKey",
                configPayload(null, configName + "-按键改", configKey, "v3"), adminToken).expectSuccess();
        assertEquals("v3", api.get("/system/config/configKey/" + configKey, adminToken)
                .expectSuccess().json().get("data"));

        HttpApiTestSupport.Response duplicate = api.postJson("/system/config",
                configPayload(null, "重复配置-" + suffix, configKey, "duplicate"), adminToken).expectEnvelope();
        assertNotEquals(200, duplicate.code(), "重复参数键不得写入");
        assertEquals(1, rows(api.get("/system/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", configKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).size());
        api.postForm("/system/config/export", Map.of("configKey", configKey), adminToken)
                .expectSpreadsheet();
        api.delete("/system/config/refreshCache", deniedToken).expectFailure(403, 403, null);
        api.delete("/system/config/refreshCache", adminToken).expectSuccess();

        String invalidConfigKey = "http.invalid." + suffix;
        HttpApiTestSupport.Response invalid = api.postJson("/system/config", Map.of(
                "configName", "非法配置-" + suffix,
                "configKey", invalidConfigKey,
                "configValue", ""
        ), adminToken).expectEnvelope();
        assertNotEquals(200, invalid.code(), "空配置值不得写入");
        assertTrue(rows(api.get("/system/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", invalidConfigKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).isEmpty(),
                "非法参数失败后不得留下记录");

        HttpApiTestSupport.Response builtInDelete = api.delete("/system/config/1", adminToken).expectEnvelope();
        assertNotEquals(200, builtInDelete.code(), "内置参数不得删除");
        api.get("/system/config/1", adminToken).expectSuccess();
        assertNull(api.delete("/system/config/" + configId, adminToken)
                .expectSuccess().json().get("data"), "Bell 删除参数响应 data 必须为 null");
        assertEquals("", api.get("/system/config/configKey/" + configKey, deniedToken)
                .expectSuccess().json().get("data"), "不存在的参数必须按上游契约返回空字符串");
    }

    @SuppressWarnings("unchecked")
    private List<Object> rows(HttpApiTestSupport.Response response) {
        return (List<Object>) response.dataObject().get("rows");
    }

    @Test
    @Order(4)
    void exercisesClientRoutesAndRejectsDuplicateKeys() {
        String clientKey = "http_client_" + suffix;
        String clientSecret = "secret_" + suffix;
        Map<String, Object> createPayload = clientPayload(null, clientKey, clientSecret, "0");
        createPayload.remove("activeTimeout");
        createPayload.remove("timeout");
        createPayload.put("grantTypeList", List.of("password", "password"));
        HttpApiTestSupport.Response clientAdd = api.postJson("/system/client",
                createPayload, adminToken).expectSuccess();
        assertNull(clientAdd.json().get("data"), "Bell 新增客户端响应 data 必须为 null");

        HttpApiTestSupport.Response list = api.get("/system/client/list" + HttpApiTestSupport.query(Map.of(
                "clientKey", clientKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        Map<String, Object> client = pageRow(list, "clientKey", clientKey);
        long clientPk = number(client.get("id"));
        String clientId = String.valueOf(client.get("clientId"));
        assertTrue(!clientId.isBlank() && !"null".equals(clientId));
        assertEquals(1800L, number(client.get("activeTimeout")), "客户端缺省活跃超时必须与 Bell 6.X 一致");
        assertEquals(604800L, number(client.get("timeout")), "客户端缺省固定超时必须与 Bell 6.X 一致");
        assertEquals(List.of("password", "password"), client.get("grantTypeList"),
                "客户端规则列表必须保持原始顺序与重复项");
        api.get("/system/client/" + clientPk, adminToken).expectSuccess();

        api.putJson("/system/client",
                clientPayload(clientPk, clientKey, clientSecret + "-edit", "0"), adminToken).expectSuccess();
        api.putJson("/system/client/changeStatus", Map.of("clientId", clientId, "status", "1"), adminToken)
                .expectSuccess();
        api.postForm("/system/client/export", Map.of("clientKey", clientKey), adminToken)
                .expectSpreadsheet();

        HttpApiTestSupport.Response duplicate = api.postJson("/system/client",
                clientPayload(null, clientKey, "duplicate_" + suffix, "0"), adminToken).expectEnvelope();
        assertNotEquals(200, duplicate.code(), "重复客户端 key 不得写入");
        assertTrue(String.valueOf(duplicate.json().get("msg")).contains("已存在"),
                "重复客户端响应应保留上游业务消息: " + duplicate.json());
        assertEquals(1, rows(api.get("/system/client/list" + HttpApiTestSupport.query(Map.of(
                "clientKey", clientKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).size(),
                "重复客户端失败后不得产生额外记录");

        HttpApiTestSupport.Response mixedDelete = api.delete(
                "/system/client/" + clientPk + "," + MISSING_ID, adminToken).expectEnvelope();
        assertNotEquals(200, mixedDelete.code(), "混入不存在 ID 时客户端批删不得部分成功");
        api.get("/system/client/" + clientPk, adminToken).expectSuccess();

        assertNull(api.delete("/system/client/" + clientPk, adminToken)
                .expectSuccess().json().get("data"), "Bell 删除客户端响应 data 必须为 null");
    }

    @Test
    @Order(5)
    void exercisesNoticeAndMessageRoutes() {
        String disabledTitle = "HTTP关闭通知-" + suffix;
        api.postJson("/system/notice", noticePayload(null, disabledTitle, "1"), adminToken).expectSuccess();
        Object disabledBox = api.get("/resource/message/box", adminToken)
                .expectSuccess().dataObject().get("noticeList");
        assertTrue(disabledBox instanceof List<?> disabledMessages
                        && disabledMessages.stream().anyMatch(item -> item instanceof Map<?, ?> map
                        && "通知公告消息".equals(map.get("title"))
                        && ("[通知] " + disabledTitle).equals(map.get("message"))
                        && map.get("data") instanceof Map<?, ?> data
                        && "1".equals(String.valueOf(data.get("status")))),
                "6.X 新增公告必须无条件发布，并保留公告状态");

        String title = "HTTP通知-" + suffix;
        try (HttpApiTestSupport.SseSubscription stream = api.openSse("/resource/message", adminToken)) {
            stream.expectEvent("connected", "");
            api.postJson("/system/notice", noticePayload(null, title), adminToken).expectSuccess();
            stream.expectBellMessage("notice", "notice", "[通知] " + title);
            api.get("/resource/message/close", adminToken).expectSuccess();
            stream.expectEvent("disconnected", "");
        }

        HttpApiTestSupport.Response page = api.get("/system/notice/list" + HttpApiTestSupport.query(Map.of(
                "noticeTitle", title, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        long noticeId = number(pageRow(page, "noticeTitle", title).get("noticeId"));

        api.get("/system/notice/" + noticeId, adminToken).expectSuccess();
        Object noticeList = api.get("/resource/message/box", adminToken)
                .expectSuccess().dataObject().get("noticeList");
        assertTrue(noticeList instanceof List<?>, "消息盒子 data.noticeList 必须为数组");
        List<Object> messages = (List<Object>) noticeList;
        assertTrue(messages.stream().anyMatch(item -> item instanceof Map<?, ?> map
                && "通知公告消息".equals(map.get("title"))
                && ("[通知] " + title).equals(map.get("message"))
                && map.get("data") instanceof Map<?, ?> data
                && "0".equals(String.valueOf(data.get("status")))), "已发布通知必须进入消息盒子");

        api.putJson("/system/notice", noticePayload(noticeId, title + "-改"), adminToken).expectSuccess();
        api.delete("/system/notice/" + noticeId, adminToken).expectSuccess();
    }

    @Test
    @Order(6)
    void exchangesWebSocketMessages() throws Exception {
        int port = Integer.parseInt(System.getenv("JIMU_TEST_SERVER_PORT"));
        URI uri = URI.create("ws://127.0.0.1:" + port + "/resource/websocket?clientid="
                + HttpApiTestSupport.PC_CLIENT_ID + "&Authorization="
                + "Bearer%20" + adminToken);
        LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
        CompletableFuture<Void> closed = new CompletableFuture<>();
        WebSocket.Listener listener = new WebSocket.Listener() {
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
                closed.complete(null);
                return null;
            }
        };

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        WebSocket socket = client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .buildAsync(uri, listener).get(10, TimeUnit.SECONDS);
        socket.request(1);
        socket.sendText("{\"type\":\"ping\"}", true).get(10, TimeUnit.SECONDS);
        assertEquals("{\"type\":\"pong\"}", messages.poll(10, TimeUnit.SECONDS));
        socket.sendText("http-contract", true).get(10, TimeUnit.SECONDS);
        Map<?, ?> custom = JsonUtil.toObject(messages.poll(10, TimeUnit.SECONDS), Map.class);
        assertEquals("custom", custom.get("type"));
        assertEquals("client", custom.get("source"));
        assertEquals("http-contract", custom.get("message"));

        socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
        closed.get(10, TimeUnit.SECONDS);
    }

    private Map<String, Object> dictTypePayload(Long id, String key, String name) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("dictId", id);
        }
        payload.put("dictType", key);
        payload.put("dictName", name);
        payload.put("isBuiltIn", "N");
        payload.put("remark", "HTTP contract");
        return payload;
    }

    private Map<String, Object> dictDataPayload(Long id, String key, String label, String value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("dictCode", id);
        }
        payload.put("dictSort", 1);
        payload.put("dictLabel", label);
        payload.put("dictValue", value);
        payload.put("dictType", key);
        payload.put("listClass", "default");
        payload.put("isDefault", "N");
        return payload;
    }

    private Map<String, Object> configPayload(Long id, String name, String key, String value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("configId", id);
        }
        payload.put("configName", name);
        payload.put("configKey", key);
        payload.put("configValue", value);
        payload.put("configType", "N");
        payload.put("remark", "HTTP contract");
        return payload;
    }

    private Map<String, Object> clientPayload(Long id, String key, String secret, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("id", id);
        }
        payload.put("clientKey", key);
        payload.put("clientSecret", secret);
        payload.put("grantTypeList", List.of("password"));
        payload.put("deviceType", "pc");
        payload.put("accessPathList", List.of("/system/**"));
        payload.put("ipWhitelistList", List.of("127.0.0.1"));
        payload.put("activeTimeout", 1800);
        payload.put("timeout", 3600);
        payload.put("status", status);
        return payload;
    }

    private Map<String, Object> noticePayload(Long id, String title) {
        return noticePayload(id, title, "0");
    }

    private Map<String, Object> noticePayload(Long id, String title, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("noticeId", id);
        }
        payload.put("noticeTitle", title);
        payload.put("noticeType", "1");
        payload.put("noticeContent", "HTTP contract content");
        payload.put("status", status);
        return payload;
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new AssertionError("预期数值，实际为: " + value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pageRow(HttpApiTestSupport.Response response, String field, Object expected) {
        Object rows = response.dataObject().get("rows");
        assertTrue(rows instanceof List<?>, "分页 rows 必须为数组");
        return ((List<Object>) rows).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(row -> expected.equals(row.get(field)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 " + field + "=" + expected + " 的响应行"));
    }
}
