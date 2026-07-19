package com.jimuqu.test.http;

import cn.hutool.core.lang.TypeReference;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        String fileName = "contract-" + suffix + ".txt";
        HttpApiTestSupport.Response uploaded = uploadTextFile(fileName, content, suffix);
        String ossId = String.valueOf(uploaded.dataObject().get("ossId"));
        assertFalse(ossId.isBlank() || "null".equals(ossId), "上传响应必须返回 ossId");

        String secondContent = "jimuqu-http-contract-second-" + suffix;
        String secondFileName = "contract-second-" + suffix + ".txt";
        String secondOssId = String.valueOf(uploadTextFile(secondFileName, secondContent, "second-" + suffix)
                .dataObject().get("ossId"));
        String missingOssId = "999999999999999999";
        List<Object> orderedFiles = api.get("/resource/oss/listByIds/" + secondOssId + "," + ossId + ","
                        + secondOssId + "," + missingOssId, adminToken)
                .expectSuccess().dataList();
        assertEquals(List.of(secondOssId, ossId, secondOssId), orderedFiles.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .map(item -> String.valueOf(item.get("ossId")))
                        .toList(),
                "listByIds 必须像 6.X 一样保持输入顺序和重复 ID，并过滤不存在项");

        api.get("/resource/oss/list" + HttpApiTestSupport.query(Map.of(
                "originalName", fileName, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        HttpApiTestSupport.Response futureFiles = api.get("/resource/oss/list"
                + HttpApiTestSupport.query(Map.of(
                "originalName", fileName,
                "pageNum", 1,
                "pageSize", 20,
                "params[beginTime]", "2999-01-01 00:00:00",
                "params[endTime]", "2999-12-31 23:59:59")), adminToken).expectPage();
        assertTrue(pageRows(futureFiles).isEmpty(), "Bell OSS 创建时间范围必须实际参与查询");
        List<Object> files = api.get("/resource/oss/listByIds/" + ossId, adminToken)
                .expectSuccess().dataList();
        Map<?, ?> storedFile = files.stream()
                .filter(item -> item instanceof Map<?, ?> map
                        && ossId.equals(String.valueOf(map.get("ossId"))))
                .map(Map.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("上传文件必须可按 ossId 回读"));
        assertEquals("admin", storedFile.get("createByName"));
        Map<String, Object> ext = JsonUtil.toObject(String.valueOf(storedFile.get("ext1")),
                new TypeReference<Map<String, Object>>() {
                });
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, number(ext.get("fileSize")));
        assertEquals("text/plain", ext.get("contentType"));
        assertEquals(suffix, ext.get("traceTag"));
        assertFalse(String.valueOf(ext.get("uploadIp")).isBlank(), "上传元数据必须记录来源 IP");

        HttpApiTestSupport.Response downloaded = api.get("/resource/oss/download/" + ossId, adminToken)
                .expectStatus(200).expectBinary("text/plain");
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), downloaded.bytes());
        assertTrue(downloaded.header("Content-Disposition").contains("attachment"));
        assertFalse(downloaded.header("download-filename").isBlank());
        assertTrue(downloaded.header("Access-Control-Expose-Headers").contains("download-filename"));
        api.delete("/resource/oss/" + ossId + "," + secondOssId, adminToken).expectSuccess();
        assertTrue(api.get("/resource/oss/listByIds/" + ossId + "," + secondOssId, adminToken)
                        .expectSuccess().dataList().isEmpty(),
                "删除 OSS 文件后必须同步清理文件记录");
        HttpApiTestSupport.Response deletedDownload = api.get("/resource/oss/download/" + ossId, adminToken)
                .expectEnvelope();
        assertNotEquals(200, deletedDownload.code(), "删除后的 OSS 文件不得继续下载");

        String boundary = "JimuquBoundaryEmpty" + suffix;
        String emptyFileName = "empty-" + suffix + ".txt";
        String emptyMultipart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + emptyFileName + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "\r\n--" + boundary + "--\r\n";
        HttpApiTestSupport.Response emptyUpload = api.request("POST", "/resource/oss/upload", emptyMultipart,
                "multipart/form-data; boundary=" + boundary, adminToken).expectEnvelope();
        assertNotEquals(200, emptyUpload.code(), "空文件不得写入 OSS");
        assertTrue(pageRows(api.get("/resource/oss/list" + HttpApiTestSupport.query(Map.of(
                "originalName", emptyFileName, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).isEmpty(),
                "空文件上传失败后不得留下文件记录");
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
                Map.of("ossConfigId", configId, "configKey", configKey, "status", "Y"), adminToken)
                .expectSuccess();
        assertEquals("Y", pageRow(api.get("/resource/oss/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", configKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage(),
                "configKey", configKey).get("status"));
        assertEquals("N", pageRow(api.get("/resource/oss/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", "default", "pageNum", 1, "pageSize", 20)), adminToken).expectPage(),
                "configKey", "default").get("status"));
        api.putJson("/resource/oss/config/changeStatus",
                Map.of("ossConfigId", configId, "configKey", configKey, "status", "N"), adminToken)
                .expectSuccess();

        Map<String, Object> duplicatePayload = new LinkedHashMap<>(ossConfigPayload(null, configKey, "N"));
        duplicatePayload.put("accessKey", "duplicate-access-" + suffix);
        HttpApiTestSupport.Response duplicate = api.postJson("/resource/oss/config",
                duplicatePayload, adminToken).expectEnvelope();
        assertNotEquals(200, duplicate.code(), "重复 OSS 配置 key 不得写入");
        assertTrue(String.valueOf(duplicate.json().get("msg")).contains("已存在"));
        assertEquals(1, pageRows(api.get("/resource/oss/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", configKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).size(),
                "重复 OSS 配置失败后不得产生额外记录");
        String invalidConfigKey = "invalid-" + suffix;
        HttpApiTestSupport.Response invalid = api.postJson("/resource/oss/config", Map.of(
                "configKey", invalidConfigKey,
                "accessPolicy", "0"
        ), adminToken).expectEnvelope();
        assertNotEquals(200, invalid.code(), "缺少访问密钥、桶和端点的 OSS 配置不得写入");
        assertTrue(pageRows(api.get("/resource/oss/config/list" + HttpApiTestSupport.query(Map.of(
                "configKey", invalidConfigKey, "pageNum", 1, "pageSize", 20)), adminToken).expectPage()).isEmpty(),
                "非法 OSS 配置失败后不得留下记录");
        api.putJson("/resource/oss/config/changeStatus", Map.of(
                "ossConfigId", 1761900000000000001L,
                "configKey", "default",
                "status", "Y"), adminToken).expectSuccess();
        api.delete("/resource/oss/config/" + configId, adminToken).expectSuccess();
    }

    @Test
    @Order(4)
    void exercisesCacheAndOnlineSessionRoutes() {
        Map<String, Object> cache = api.get("/monitor/cache", adminToken)
                .expectSuccess().dataObject();
        assertTrue(number(cache.get("dbSize")) >= 0);
        assertTrue(cache.get("info") instanceof Map<?, ?>, "Redis info 必须是 JSON 对象");
        assertTrue(cache.get("commandStats") instanceof List<?>);

        assertOnlineSession(api.get("/monitor/online", adminToken).expectPage(), adminToken, "admin");
        assertOnlineSession(api.get("/monitor/online/list?pageNum=1&pageSize=100", adminToken).expectPage(),
                adminToken, "admin");

        String otherToken = api.loginAdmin();
        HttpApiTestSupport.Response otherSessions = api.get("/monitor/online", otherToken).expectPage();
        assertOnlineSession(otherSessions, otherToken, "admin");
        assertTrue(pageRows(api.get("/monitor/online/list" + HttpApiTestSupport.query(Map.of(
                "userName", "missing-online-" + suffix,
                "pageNum", 1,
                "pageSize", 100)), adminToken).expectPage()).isEmpty(),
                "在线用户账号过滤必须实际参与查询");

        String foreignToken = "foreign-" + suffix;
        api.delete("/monitor/online/myself/" + foreignToken, adminToken).expectSuccess();
        api.get("/monitor/online", otherToken).expectPage();

        String selfManagedToken = api.loginAdmin();
        api.delete("/monitor/online/myself/"
                + URLEncoder.encode(selfManagedToken, StandardCharsets.UTF_8), selfManagedToken).expectSuccess();
        api.get("/monitor/online", selfManagedToken).expectStatus(401).expectCode(401);

        String encodedOtherToken = URLEncoder.encode(otherToken, StandardCharsets.UTF_8);
        api.delete("/monitor/online/" + encodedOtherToken, adminToken).expectSuccess();
        api.get("/monitor/online", otherToken).expectStatus(401).expectCode(401);
        api.get("/monitor/cache", adminToken).expectSuccess();
    }

    @Test
    @Order(5)
    void exercisesOperationAndLoginAuditRoutes() throws InterruptedException {
        assertProfileOperationLogMasksSensitiveFields();
        HttpApiTestSupport.Response operPage = api.get(
                "/monitor/operlog/list?pageNum=1&pageSize=100", adminToken).expectPage();
        String auditedPath = pageRows(operPage).stream()
                .map(row -> row.get("operUrl"))
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new AssertionError("写操作必须生成操作日志"));
        assertTrue(auditedPath.startsWith("/") && !auditedPath.contains("://"),
                "操作日志 operUrl 必须只记录请求路径: " + auditedPath);
        assertTrue(pageRows(api.get("/monitor/operlog/list" + HttpApiTestSupport.query(Map.of(
                "pageNum", 1,
                "pageSize", 100,
                "params[beginTime]", "2999-01-01 00:00:00",
                "params[endTime]", "2999-12-31 23:59:59")), adminToken).expectPage()).isEmpty(),
                "Bell 操作日志时间范围必须实际参与查询");
        api.postForm("/monitor/operlog/export", Map.of(), adminToken)
                .expectSpreadsheet();
        deleteFirstAuditRowOrAssertMissingFailure(operPage, "operId", "/monitor/operlog/");
        api.delete("/monitor/operlog/clean", adminToken).expectSuccess();

        HttpApiTestSupport.Response loginPage = api.get(
                "/monitor/loginInfo/list?userName=admin&pageNum=1&pageSize=100", adminToken).expectPage();
        assertTrue(pageRows(loginPage).stream().anyMatch(row -> "pc".equals(row.get("clientKey"))
                        && "pc".equals(row.get("deviceType"))),
                "成功登录日志必须按 ClientID 回填 clientKey 与 deviceType");

        Map<String, Object> failedLogin = api.withCaptcha(Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", "no_permission",
                "password", "wrong-password-" + suffix));
        HttpApiTestSupport.Response failed = api.postEncryptedJsonWithHeaders("/auth/login", failedLogin,
                Map.of("ClientID", "unknown-client-" + suffix)).expectEnvelope();
        assertNotEquals(200, failed.code());
        HttpApiTestSupport.Response failedLogs = api.get("/monitor/loginInfo/list"
                + HttpApiTestSupport.query(Map.of(
                "userName", "no_permission",
                "status", "1",
                "pageNum", 1,
                "pageSize", 100)), adminToken).expectPage();
        Map<String, Object> failedRow = pageRows(failedLogs).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("失败登录必须写入登录日志"));
        assertNull(failedRow.get("clientKey"), "未知 ClientID 不得伪造 clientKey");
        assertNull(failedRow.get("deviceType"), "未知 ClientID 不得伪造 deviceType");
        assertFalse(String.valueOf(failedRow.get("msg")).isBlank(), "失败登录日志必须保留原因");
        assertTrue(pageRows(api.get("/monitor/loginInfo/list" + HttpApiTestSupport.query(Map.of(
                "userName", "admin",
                "pageNum", 1,
                "pageSize", 100,
                "params[beginTime]", "2999-01-01 00:00:00",
                "params[endTime]", "2999-12-31 23:59:59")), adminToken).expectPage()).isEmpty(),
                "Bell 登录日志时间范围必须实际参与查询");
        api.postForm("/monitor/loginInfo/export", Map.of("userName", "admin"), adminToken)
                .expectSpreadsheet();
        api.get("/monitor/loginInfo/unlock/no_permission", adminToken).expectSuccess();
        deleteFirstAuditRowOrAssertMissingFailure(loginPage, "infoId", "/monitor/loginInfo/");
        api.delete("/monitor/loginInfo/clean", adminToken).expectSuccess();
    }

    private void assertProfileOperationLogMasksSensitiveFields() throws InterruptedException {
        Map<String, Object> profile = api.get("/system/user/profile", adminToken)
                .expectSuccess().dataObject();
        @SuppressWarnings("unchecked")
        Map<String, Object> originalUser = new LinkedHashMap<>((Map<String, Object>) profile.get("user"));
        String email = "audit_" + suffix + "@jimuqu.test";
        String phone = "139" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        String marker = "日志脱敏-" + suffix;
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("nickName", marker);
        update.put("email", email);
        update.put("phoneNumber", phone);
        update.put("sex", originalUser.get("sex"));
        api.putJson("/system/user/profile", update, adminToken).expectSuccess();

        Map<String, Object> audited = null;
        for (int attempt = 0; attempt < 40 && audited == null; attempt++) {
            HttpApiTestSupport.Response logs = api.get("/monitor/operlog/list"
                    + HttpApiTestSupport.query(Map.of(
                    "title", "个人信息",
                    "pageNum", 1,
                    "pageSize", 20)), adminToken).expectPage();
            audited = pageRows(logs).stream()
                    .filter(row -> "/system/user/profile".equals(row.get("operUrl")))
                    .filter(row -> "PUT".equals(row.get("requestMethod")))
                    .filter(row -> String.valueOf(row.get("operParam")).contains(marker))
                    .findFirst()
                    .orElse(null);
            if (audited == null) {
                Thread.sleep(50L);
            }
        }
        assertTrue(audited != null, "个人资料更新必须生成操作日志");
        String params = String.valueOf(audited.get("operParam"));
        assertFalse(params.contains(email), "操作日志不得记录完整邮箱: " + params);
        assertFalse(params.contains(phone), "操作日志不得记录完整手机号: " + params);
        assertTrue(params.contains("a***@jimuqu.test"), "操作日志应按上游规则脱敏邮箱: " + params);
        assertTrue(params.contains(phone.substring(0, 3) + "****" + phone.substring(7)),
                "操作日志应按上游规则脱敏手机号: " + params);

        Map<String, Object> restore = new LinkedHashMap<>();
        restore.put("nickName", originalUser.get("nickName"));
        restore.put("email", originalUser.get("email"));
        restore.put("phoneNumber", originalUser.get("phoneNumber"));
        restore.put("sex", originalUser.get("sex"));
        api.putJson("/system/user/profile", restore, adminToken).expectSuccess();
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

    private HttpApiTestSupport.Response uploadTextFile(String fileName, String content, String traceTag) {
        String boundary = "JimuquBoundary" + Long.toUnsignedString(System.nanoTime(), 36);
        String multipart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + content + "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"ossExt\"\r\n"
                + "Content-Type: application/json\r\n\r\n"
                + "{\"traceTag\":\"" + traceTag + "\"}\r\n"
                + "--" + boundary + "--\r\n";
        return api.request("POST", "/resource/oss/upload", multipart,
                "multipart/form-data; boundary=" + boundary, adminToken).expectSuccess();
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
        Map<String, Object> session = pageRows(response).stream()
                .filter(row -> tokenId.equals(row.get("tokenId")) && userName.equals(row.get("userName")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("在线会话分页必须包含当前登录用户及其真实 token"));
        assertFalse(session.containsKey("nickName"), "在线用户响应不得额外暴露 Bell DTO 未声明的 nickName");
        assertTrue(session.get("loginTime") instanceof Number, "在线会话登录时间必须是毫秒时间戳");
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
