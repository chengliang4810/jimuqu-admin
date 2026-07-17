package com.jimuqu.test.http;

import com.jimuqu.Application;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.noear.solon.test.SolonTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 字典、配置、客户端、通知和消息盒子的真实 HTTP 契约。 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigurationMessagingHttpContractTest {

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
        api.get("/system/config/list?pageNum=1&pageSize=10")
                .expectStatus(401)
                .expectCode(401);
        api.get("/system/dict/type/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.get("/system/client/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.postJson("/system/notice", noticePayload(null, "拒绝写入-" + suffix), deniedToken)
                .expectStatus(403)
                .expectCode(403);
    }

    @Test
    @Order(2)
    void exercisesDictionaryRoutes() {
        String dictKey = "http_dict_" + suffix;
        String dictName = "HTTP字典-" + suffix;
        String dictLabel = "HTTP标签-" + suffix;

        long dictId = scalarLong(api.postJson("/system/dict/type",
                dictTypePayload(null, dictKey, dictName), adminToken).expectSuccess());
        api.get("/system/dict/type/all" + HttpApiTestSupport.query(Map.of("dictKey", dictKey)), deniedToken)
                .expectSuccess();
        api.get("/system/dict/type/list" + HttpApiTestSupport.query(Map.of(
                "dictKey", dictKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        api.get("/system/dict/type/" + dictId, adminToken).expectSuccess();
        api.get("/system/dict/type/optionselect", deniedToken).expectSuccess();

        long dataId = scalarLong(api.postJson("/system/dict/data",
                dictDataPayload(null, dictKey, dictLabel, "v1"), adminToken).expectSuccess());
        api.get("/system/dict/data/list" + HttpApiTestSupport.query(Map.of(
                "dictTypeKey", dictKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        api.get("/system/dict/data/" + dataId, adminToken).expectSuccess();
        List<Object> values = api.get("/system/dict/data/type/" + dictKey, deniedToken)
                .expectSuccess().dataList();
        assertTrue(values.stream().anyMatch(item -> item instanceof Map<?, ?> map
                && dictLabel.equals(map.get("dictLabel"))), "按类型查询必须返回新建字典项");

        api.putJson("/system/dict/type", dictTypePayload(dictId, dictKey, dictName + "-改"), adminToken)
                .expectSuccess();
        api.putJson("/system/dict/data", dictDataPayload(dataId, dictKey, dictLabel + "-改", "v2"), adminToken)
                .expectSuccess();

        api.postForm("/system/dict/type/export", Map.of("dictKey", dictKey), adminToken)
                .expectSpreadsheet();
        api.postForm("/system/dict/data/export", Map.of("dictTypeKey", dictKey), adminToken)
                .expectSpreadsheet();
        api.delete("/system/dict/type/refreshCache", deniedToken).expectSuccess();

        HttpApiTestSupport.Response invalid = api.postJson("/system/dict/type",
                Map.of("dictType", "", "dictKey", "", "dictName", ""), adminToken).expectEnvelope();
        assertNotEquals(200, invalid.code(), "空字典字段不得写入");

        assertEquals(1L, scalarLong(api.delete("/system/dict/data/" + dataId, adminToken).expectSuccess()));
        assertEquals(1L, scalarLong(api.delete("/system/dict/type/" + dictId, adminToken).expectSuccess()));
    }

    @Test
    @Order(3)
    void exercisesConfigurationRoutes() {
        String configKey = "http.config." + suffix;
        String configName = "HTTP配置-" + suffix;
        assertEquals("true", api.get("/system/config/configKey/sys.oss.previewListResource", deniedToken)
                .expectSuccess().json().get("data"));
        long configId = scalarLong(api.postJson("/system/config",
                configPayload(null, configName, configKey, "v1"), adminToken).expectSuccess());

        HttpApiTestSupport.Response configPage = api.get("/system/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", configKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        assertEquals(configId, number(pageRow(configPage, "configKey", configKey).get("configId")));
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
        api.delete("/system/config/refreshCache", adminToken).expectSuccess();

        HttpApiTestSupport.Response invalid = api.postJson("/system/config", Map.of(
                "configName", "非法配置-" + suffix,
                "configKey", "http.invalid." + suffix,
                "configValue", ""
        ), adminToken).expectEnvelope();
        assertNotEquals(200, invalid.code(), "空配置值不得写入");

        assertEquals(1L, scalarLong(api.delete("/system/config/" + configId, adminToken).expectSuccess()));
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
        long clientPk = scalarLong(api.postJson("/system/client",
                clientPayload(null, clientKey, clientSecret, "0"), adminToken).expectSuccess());

        HttpApiTestSupport.Response list = api.get("/system/client/list" + HttpApiTestSupport.query(Map.of(
                "clientKey", clientKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        Map<String, Object> client = pageRow(list, "clientKey", clientKey);
        String clientId = String.valueOf(client.get("clientId"));
        assertTrue(!clientId.isBlank() && !"null".equals(clientId));
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
        assertTrue(String.valueOf(duplicate.json().get("msg")).contains("已存在"));

        assertEquals(1L, scalarLong(api.delete("/system/client/" + clientPk, adminToken).expectSuccess()));
    }

    @Test
    @Order(5)
    void exercisesNoticeAndMessageRoutes() {
        String title = "HTTP通知-" + suffix;
        String directMessage = "HTTP定向消息-" + suffix;
        String broadcastMessage = "HTTP广播消息-" + suffix;
        try (HttpApiTestSupport.SseSubscription stream = api.openSse("/resource/message", adminToken)) {
            stream.expectEvent("connected", "");
            api.get("/resource/message/send" + HttpApiTestSupport.query(Map.of(
                    "userId", 1, "msg", directMessage)), adminToken).expectSuccess();
            stream.expectBellMessage("message", "backend", directMessage);
            api.get("/resource/message/sendAll" + HttpApiTestSupport.query(Map.of(
                    "msg", broadcastMessage)), adminToken).expectSuccess();
            stream.expectBellMessage("message", "backend", broadcastMessage);
            api.postJson("/system/notice", noticePayload(null, title), adminToken).expectSuccess();
            stream.expectBellMessage("notice", "notice", title);
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
                && String.valueOf(map.get("title")).contains(title)), "已发布通知必须进入消息盒子");

        api.putJson("/system/notice", noticePayload(noticeId, title + "-改"), adminToken).expectSuccess();
        api.delete("/system/notice/" + noticeId, adminToken).expectSuccess();
    }

    private Map<String, Object> dictTypePayload(Long id, String key, String name) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("dictId", id);
        }
        payload.put("dictKey", key);
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
        payload.put("parentId", 0);
        payload.put("dictSort", 1);
        payload.put("dictLabel", label);
        payload.put("dictValue", value);
        payload.put("dictTypeKey", key);
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
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("noticeId", id);
        }
        payload.put("noticeTitle", title);
        payload.put("noticeType", "1");
        payload.put("noticeContent", "HTTP contract content");
        payload.put("status", "0");
        return payload;
    }

    private long scalarLong(HttpApiTestSupport.Response response) {
        return number(response.json().get("data"));
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
