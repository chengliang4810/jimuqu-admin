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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OSS 资源与监控接口的真实 HTTP 契约。 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResourceMonitorHttpContractTest {

    private HttpApiTestSupport api;
    private String adminToken;
    private String deniedToken;
    private String suffix;

    @BeforeAll
    void setUp() {
        api = new HttpApiTestSupport(ResourceMonitorHttpContractTest::ownsRoute);
        adminToken = api.loginAdmin();
        deniedToken = api.login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);
        suffix = Long.toUnsignedString(System.nanoTime(), 36);
    }

    static boolean ownsRoute(com.jimuqu.test.coverage.RuntimeRouteCoverage.RouteKey key) {
        return key.path().startsWith("/resource/oss")
                || key.path().startsWith("/monitor/cache")
                || key.path().startsWith("/monitor/online")
                || key.path().startsWith("/monitor/operlog")
                || key.path().startsWith("/monitor/loginInfo");
    }

    @AfterAll
    void assertRouteCoverage() {
        api.assertCoverageComplete();
    }

    @Test
    @Order(1)
    void rejectsUnauthenticatedAndUnprivilegedAccess() {
        api.get("/resource/oss/list?pageNum=1&pageSize=10")
                .expectStatus(401)
                .expectCode(401);
        api.get("/resource/oss/config/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.get("/monitor/cache", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.get("/monitor/online/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
    }

    @Test
    @Order(2)
    void uploadsDownloadsListsAndDeletesARealFile() {
        String content = "jimuqu-http-contract-" + suffix;
        String boundary = "JimuquBoundary" + suffix;
        String fileName = "contract-" + suffix + ".txt";
        String multipart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + content + "\r\n--" + boundary + "--\r\n";

        HttpApiTestSupport.Response uploaded = api.request("POST", "/resource/oss/upload", multipart,
                "multipart/form-data; boundary=" + boundary, adminToken).expectSuccess();
        String ossId = String.valueOf(uploaded.dataObject().get("ossId"));
        assertFalse(ossId.isBlank() || "null".equals(ossId), "上传响应必须返回 ossId");

        api.get("/resource/oss/list" + HttpApiTestSupport.query(Map.of(
                "originalName", fileName, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        List<Object> files = api.get("/resource/oss/listByIds/" + ossId, adminToken)
                .expectSuccess().dataList();
        assertTrue(files.stream().anyMatch(item -> item instanceof Map<?, ?> map
                && ossId.equals(String.valueOf(map.get("ossId")))));

        HttpApiTestSupport.Response downloaded = api.get("/resource/oss/download/" + ossId, adminToken)
                .expectStatus(200).expectBinary("text/plain");
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), downloaded.bytes());
        api.delete("/resource/oss/" + ossId, adminToken).expectSuccess();
    }

    @Test
    @Order(3)
    void exercisesOssConfigurationRoutesAndRejectsDuplicateKeys() {
        String configKey = "http-oss-" + suffix;
        api.postJson("/resource/oss/config", ossConfigPayload(null, configKey, "N"), adminToken)
                .expectSuccess();
        HttpApiTestSupport.Response list = api.get("/resource/oss/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", configKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        Map<String, Object> config = pageRow(list, "configKey", configKey);
        long configId = number(config.get("ossConfigId"));

        api.get("/resource/oss/config/" + configId, adminToken).expectSuccess();
        api.putJson("/resource/oss/config", ossConfigPayload(configId, configKey, "N"), adminToken)
                .expectSuccess();
        api.putJson("/resource/oss/config/changeStatus",
                Map.of("ossConfigId", configId, "status", "N"), adminToken).expectSuccess();

        Map<String, Object> duplicatePayload = new LinkedHashMap<>(ossConfigPayload(null, configKey, "N"));
        duplicatePayload.put("accessKey", "duplicate-access-" + suffix);
        HttpApiTestSupport.Response duplicate = api.postJson("/resource/oss/config",
                duplicatePayload, adminToken).expectEnvelope();
        assertNotEquals(200, duplicate.code(), "重复 OSS 配置 key 不得写入");
        assertTrue(String.valueOf(duplicate.json().get("msg")).contains("已存在"));
        api.delete("/resource/oss/config/" + configId, adminToken).expectSuccess();
    }

    @Test
    @Order(4)
    void exercisesCacheAndOnlineSessionRoutes() {
        Map<String, Object> cache = api.get("/monitor/cache", adminToken)
                .expectSuccess().dataObject();
        assertTrue(number(cache.get("dbSize")) >= 0);
        assertTrue(cache.get("commandStats") instanceof List<?>);

        assertOnlineSession(api.get("/monitor/online", adminToken).expectPage(), adminToken, "admin");
        assertOnlineSession(api.get("/monitor/online/list?pageNum=1&pageSize=100", adminToken).expectPage(),
                adminToken, "admin");

        String otherToken = api.loginAdmin();
        HttpApiTestSupport.Response otherSessions = api.get("/monitor/online", otherToken).expectPage();
        assertOnlineSession(otherSessions, otherToken, "admin");

        String foreignToken = "foreign-" + suffix;
        api.delete("/monitor/online/myself/" + foreignToken, adminToken).expectSuccess();
        api.get("/monitor/online", otherToken).expectPage();

        String encodedOtherToken = URLEncoder.encode(otherToken, StandardCharsets.UTF_8);
        api.delete("/monitor/online/" + encodedOtherToken, adminToken).expectSuccess();
        api.get("/monitor/online", otherToken).expectStatus(401).expectCode(401);
        api.get("/monitor/cache", adminToken).expectSuccess();
    }

    @Test
    @Order(5)
    void exercisesOperationAndLoginAuditRoutes() {
        HttpApiTestSupport.Response operPage = api.get(
                "/monitor/operlog/list?pageNum=1&pageSize=100", adminToken).expectPage();
        api.postForm("/monitor/operlog/export", Map.of(), adminToken)
                .expectSpreadsheet();
        deleteFirstAuditRowOrAssertMissingFailure(operPage, "operId", "/monitor/operlog/");
        api.delete("/monitor/operlog/clean", adminToken).expectSuccess();

        HttpApiTestSupport.Response loginPage = api.get(
                "/monitor/loginInfo/list?userName=admin&pageNum=1&pageSize=100", adminToken).expectPage();
        api.postForm("/monitor/loginInfo/export", Map.of("userName", "admin"), adminToken)
                .expectSpreadsheet();
        api.get("/monitor/loginInfo/unlock/no_permission", adminToken).expectSuccess();
        deleteFirstAuditRowOrAssertMissingFailure(loginPage, "infoId", "/monitor/loginInfo/");
        api.delete("/monitor/loginInfo/clean", adminToken).expectSuccess();
    }

    private Map<String, Object> ossConfigPayload(Long id, String key, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("ossConfigId", id);
        }
        payload.put("configKey", key);
        payload.put("accessKey", "access-" + suffix);
        payload.put("secretKey", "secret-" + suffix);
        payload.put("bucketName", "bucket-" + suffix);
        payload.put("prefix", "http/");
        payload.put("endpoint", "http://127.0.0.1:9000");
        payload.put("domainUrl", "http://127.0.0.1:9000/bucket");
        payload.put("isHttps", "N");
        payload.put("region", "local");
        payload.put("status", status);
        payload.put("accessPolicy", "0");
        return payload;
    }

    private void deleteFirstAuditRowOrAssertMissingFailure(HttpApiTestSupport.Response page,
                                                            String idField, String routePrefix) {
        List<Map<String, Object>> rows = pageRows(page);
        if (!rows.isEmpty() && rows.get(0).get(idField) != null) {
            api.delete(routePrefix + number(rows.get(0).get(idField)), adminToken).expectSuccess();
            return;
        }
        HttpApiTestSupport.Response missing = api.delete(routePrefix + "0", adminToken).expectEnvelope();
        assertNotEquals(200, missing.code(), "删除不存在的审计记录必须失败");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> pageRows(HttpApiTestSupport.Response response) {
        Object rows = response.dataObject().get("rows");
        assertTrue(rows instanceof List<?>);
        return ((List<Object>) rows).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private Map<String, Object> pageRow(HttpApiTestSupport.Response response, String field, Object expected) {
        return pageRows(response).stream()
                .filter(row -> expected.equals(row.get(field)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 " + field + "=" + expected + " 的响应行"));
    }

    private void assertOnlineSession(HttpApiTestSupport.Response response, String tokenId, String userName) {
        assertTrue(pageRows(response).stream().anyMatch(row -> tokenId.equals(row.get("tokenId"))
                        && userName.equals(row.get("userName"))),
                "在线会话分页必须包含当前登录用户及其真实 token");
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
}
