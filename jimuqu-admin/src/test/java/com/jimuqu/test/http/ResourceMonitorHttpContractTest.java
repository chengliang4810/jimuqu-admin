package com.jimuqu.test.http;

import cn.hutool.core.lang.TypeReference;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.Application;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.mapper.SysScheduledJobConfigMapper;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import com.jimuqu.test.support.ManagedSchedulingTestJob;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.noear.solon.Solon;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;
import org.noear.solon.test.SolonTest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
                || key.path().startsWith("/monitor/loginInfo")
                || key.path().startsWith("/monitor/job");
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
        Map<String, Object> failedRow = awaitFailedLoginLog();
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

    @Test
    @Order(6)
    void managesSolonRuntimeJobs() throws Exception {
        assertJobRoutePermissionFailures();
        Map<String, Object> initial = jobRow();
        assertEquals("FIXED_DELAY", initial.get("scheduleType"));
        assertEquals("1000", initial.get("scheduleExpression"));
        assertEquals(false, initial.get("enabled"));
        assertEquals(0, number(initial.get("maxRetries")));
        assertEquals(1000, number(initial.get("retryIntervalMs")));

        api.putJson("/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME + "/config",
                        Map.of("maxRetries", 1, "retryIntervalMs", 0), adminToken)
                .expectSuccess();
        assertEquals(1, number(jobRow().get("maxRetries")));
        assertEquals("FORBID", jobRow().get("concurrentPolicy"),
                "启用失败重试后必须自动禁止同一任务并发执行");
        HttpApiTestSupport.Response invalidConfig = api.putJson(
                "/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME + "/config",
                Map.of("maxRetries", 11, "retryIntervalMs", 0), adminToken).expectEnvelope();
        assertNotEquals(200, invalidConfig.code());
        assertEquals(1, number(jobRow().get("maxRetries")), "非法重试配置不得写入");

        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.SUCCESS);
        int before = ManagedSchedulingTestJob.executions();
        HttpApiTestSupport.Response submitted = api.postJson(
                        "/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME + "/run",
                        Map.of(), adminToken)
                .expectSuccess();
        assertEquals("定时任务已提交执行", submitted.json().get("msg"));
        awaitExecutions(before + 1);
        assertEquals(before + 1, ManagedSchedulingTestJob.executions(), "立即执行必须调用原始 Solon 任务处理器");
        assertEquals(false, jobRow().get("enabled"), "停止状态不得阻止手动执行");

        int logCount = jobLogs().size();
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.FAIL_ONCE);
        api.postJson("/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME + "/run", Map.of(), adminToken)
                .expectSuccess();
        List<Map<String, Object>> retrySuccess = awaitJobLogs(logCount + 2).subList(0, 2);
        assertTrue(retrySuccess.stream().anyMatch(row -> "RETRY".equals(row.get("status"))
                && number(row.get("attempt")) == 1));
        assertTrue(retrySuccess.stream().anyMatch(row -> "SUCCESS".equals(row.get("status"))
                && number(row.get("attempt")) == 2));

        logCount += 2;
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.ALWAYS_FAIL);
        api.postJson("/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME + "/run", Map.of(), adminToken)
                .expectSuccess();
        List<Map<String, Object>> finalFailure = awaitJobLogs(logCount + 2).subList(0, 2);
        assertTrue(finalFailure.stream().anyMatch(row -> "RETRY".equals(row.get("status"))));
        assertTrue(finalFailure.stream().anyMatch(row -> "FAILED".equals(row.get("status"))
                && String.valueOf(row.get("errorSummary")).contains("planned final failure")));

        api.putJson("/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME + "/start", Map.of(), adminToken)
                .expectSuccess();
        assertEquals(true, jobRow().get("enabled"));
        assertSingleScheduledExecutionAcrossConcurrentTriggers();

        api.putJson("/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME + "/stop", Map.of(), adminToken)
                .expectSuccess();
        assertEquals(false, jobRow().get("enabled"));

        HttpApiTestSupport.Response missing = api.postJson(
                "/monitor/job/missing-" + suffix + "/run", Map.of(), adminToken).expectEnvelope();
        assertNotEquals(200, missing.code(), "不存在的运行时任务不得执行");

        List<Map<String, Object>> logs = jobLogs();
        assertTrue(logs.stream().allMatch(row -> row.get("instanceId") != null
                && row.get("startTime") != null && row.get("endTime") != null
                && row.get("durationMs") != null));
        assertJobLogNameFilterIsExact();
        long firstLogId = number(logs.get(0).get("logId"));
        api.delete("/monitor/job/log/" + firstLogId, adminToken).expectSuccess();
        assertFalse(jobLogs().stream().anyMatch(row -> number(row.get("logId")) == firstLogId));
        api.delete("/monitor/job/log/clean", adminToken).expectSuccess();
        assertTrue(jobLogs().isEmpty());
    }

    /** 验证所有受保护的定时任务操作同时拒绝未登录和无权限用户。 */
    private void assertJobRoutePermissionFailures() {
        String jobPath = "/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME;
        api.get("/monitor/job/list").expectStatus(401).expectCode(401);
        api.get("/monitor/job/list", deniedToken).expectStatus(403).expectCode(403);
        api.putJson(jobPath + "/config", Map.of("maxRetries", 1, "retryIntervalMs", 0))
                .expectStatus(401).expectCode(401);
        api.putJson(jobPath + "/config", Map.of("maxRetries", 1, "retryIntervalMs", 0),
                        deniedToken)
                .expectStatus(403).expectCode(403);
        api.putJson(jobPath + "/start", Map.of()).expectStatus(401).expectCode(401);
        api.putJson(jobPath + "/start", Map.of(), deniedToken)
                .expectStatus(403).expectCode(403);
        api.putJson(jobPath + "/stop", Map.of()).expectStatus(401).expectCode(401);
        api.putJson(jobPath + "/stop", Map.of(), deniedToken)
                .expectStatus(403).expectCode(403);
        api.postJson(jobPath + "/run", Map.of()).expectStatus(401).expectCode(401);
        api.postJson(jobPath + "/run", Map.of(), deniedToken)
                .expectStatus(403).expectCode(403);
        api.get("/monitor/job/log/list?pageNum=1&pageSize=10")
                .expectStatus(401).expectCode(401);
        api.get("/monitor/job/log/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403).expectCode(403);
        api.delete("/monitor/job/log/999999999999999999")
                .expectStatus(401).expectCode(401);
        api.delete("/monitor/job/log/999999999999999999", deniedToken)
                .expectStatus(403).expectCode(403);
        api.delete("/monitor/job/log/clean").expectStatus(401).expectCode(401);
        api.delete("/monitor/job/log/clean", deniedToken)
                .expectStatus(403).expectCode(403);
    }

    /** 验证动态任务白名单、CRUD、执行策略与运行时生命周期。 */
    @Test
    @Order(7)
    void managesWhitelistedDynamicJobs() throws Exception {
        String guardedName = "guardedDynamicJob." + suffix;
        Map<String, Object> guardedPayload = dynamicJobPayload(guardedName);
        api.get("/monitor/job/handlers").expectStatus(401).expectCode(401);
        api.get("/monitor/job/handlers", deniedToken).expectStatus(403).expectCode(403);
        api.postJson("/monitor/job", guardedPayload).expectStatus(401).expectCode(401);
        api.postJson("/monitor/job", guardedPayload, deniedToken)
                .expectStatus(403).expectCode(403);
        api.putJson("/monitor/job/" + guardedName, guardedPayload)
                .expectStatus(401).expectCode(401);
        api.putJson("/monitor/job/" + guardedName, guardedPayload, deniedToken)
                .expectStatus(403).expectCode(403);
        api.delete("/monitor/job/" + guardedName).expectStatus(401).expectCode(401);
        api.delete("/monitor/job/" + guardedName, deniedToken)
                .expectStatus(403).expectCode(403);
        assertEquals(0, jobConfigCount(guardedName),
                "认证或授权失败不得写入动态任务配置");

        Map<String, Object> handler = api.get("/monitor/job/handlers", adminToken)
                .expectSuccess().dataList().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(row -> (Map<String, Object>) row)
                .filter(row -> ManagedSchedulingTestJob.HANDLER_KEY.equals(row.get("handlerKey")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("测试任务方法必须出现在动态任务白名单中"));
        assertEquals(ManagedSchedulingTestJob.class.getName(), handler.get("className"));
        assertEquals("execute", handler.get("methodName"));

        String statusBypassName = "statusBypassJob." + suffix;
        Map<String, Object> statusBypass = dynamicJobPayload(statusBypassName);
        statusBypass.put("enabled", true);
        api.postJson("/monitor/job", statusBypass, adminToken).expectSuccess();
        assertEquals(false, dynamicJobRow(statusBypassName).get("enabled"),
                "新增或编辑定义不得绕过独立启停权限");
        api.delete("/monitor/job/" + statusBypassName, adminToken)
                .expectSuccess();

        String rejectedName = "rejectedDynamicJob." + suffix;
        Map<String, Object> rejectedHandler = dynamicJobPayload(rejectedName);
        rejectedHandler.put("handlerKey", "java.lang.Runtime.exec");
        HttpApiTestSupport.Response rejected = api.postJson(
                "/monitor/job", rejectedHandler, adminToken).expectEnvelope();
        assertNotEquals(200, rejected.code(), "未显式注解的方法不得成为在线任务");
        assertEquals(0, jobConfigCount(rejectedName), "非法处理器不得写入数据库");

        String invalidCronName = "invalidCronJob." + suffix;
        Map<String, Object> invalidCron = dynamicJobPayload(invalidCronName);
        invalidCron.put("scheduleExpression", "not-a-cron");
        HttpApiTestSupport.Response invalid = api.postJson(
                "/monitor/job", invalidCron, adminToken).expectEnvelope();
        assertNotEquals(200, invalid.code(), "非法 Cron 表达式不得创建在线任务");
        assertEquals(0, jobConfigCount(invalidCronName), "非法 Cron 不得写入数据库");

        String unsafeRetryName = "unsafeRetryJob." + suffix;
        Map<String, Object> unsafeRetry = dynamicJobPayload(unsafeRetryName);
        unsafeRetry.put("concurrentPolicy", "ALLOW");
        HttpApiTestSupport.Response unsafeRetryFailure = api.postJson(
                "/monitor/job", unsafeRetry, adminToken).expectEnvelope();
        assertNotEquals(200, unsafeRetryFailure.code(),
                "启用失败重试的在线任务不得允许并发");
        assertEquals(0, jobConfigCount(unsafeRetryName),
                "不安全的重试并发组合不得写入数据库");

        String missingName = "missingDynamicJob." + suffix;
        Map<String, Object> missingPayload = dynamicJobPayload(missingName);
        assertNotEquals(200, api.putJson(
                "/monitor/job/" + missingName, missingPayload, adminToken)
                .expectEnvelope().code(), "不存在的动态任务不得更新");
        assertNotEquals(200, api.delete(
                "/monitor/job/" + missingName, adminToken)
                .expectEnvelope().code(), "不存在的动态任务不得删除");
        assertEquals(0, jobConfigCount(missingName),
                "不存在任务的更新或删除失败后不得写入数据库");

        String jobName = "dynamicContractJob." + suffix;
        Map<String, Object> createdPayload = dynamicJobPayload(jobName);
        createdPayload.put("scheduleType", "FIXED_RATE");
        createdPayload.put("scheduleExpression", "100");
        createdPayload.put("zone", "");
        int disabledExecutions = ManagedSchedulingTestJob.executions();
        api.postJson("/monitor/job", createdPayload, adminToken).expectSuccess();
        assertEquals(1, jobConfigCount(jobName));
        Map<String, Object> created = dynamicJobRow(jobName);
        assertEquals("DYNAMIC", created.get("jobSource"));
        assertEquals(ManagedSchedulingTestJob.HANDLER_KEY, created.get("handlerKey"));
        assertEquals("FIXED_RATE", created.get("scheduleType"));
        assertEquals("100", created.get("scheduleExpression"));
        assertEquals("", created.get("zone"));
        assertEquals("FORBID", created.get("concurrentPolicy"));
        assertEquals("FIRE_ONCE", created.get("misfirePolicy"));
        assertNull(Solon.context().getBean(IJobManager.class).jobGet(jobName),
                "禁用的零延迟在线任务不得注册到 Solon IJobManager");
        Thread.sleep(350L);
        assertEquals(disabledExecutions, ManagedSchedulingTestJob.executions(),
                "禁用的零延迟 fixedRate 任务在新增后不得抢跑");

        HttpApiTestSupport.Response duplicate = api.postJson(
                "/monitor/job", createdPayload, adminToken).expectEnvelope();
        assertNotEquals(200, duplicate.code(), "在线任务名称不得重复");
        assertEquals(1, jobConfigCount(jobName), "重复新增不得覆盖已有任务");

        Map<String, Object> renamed = dynamicJobPayload("renamedDynamicJob." + suffix);
        HttpApiTestSupport.Response renameFailure = api.putJson(
                "/monitor/job/" + jobName, renamed, adminToken).expectEnvelope();
        assertNotEquals(200, renameFailure.code(), "更新时不得改变任务唯一名称");
        assertEquals(1, jobConfigCount(jobName), "非法改名不得删除原任务");
        assertEquals(0, jobConfigCount(String.valueOf(renamed.get("jobName"))),
                "非法改名不得写入新任务");

        Map<String, Object> normalizedCron = dynamicJobPayload(jobName);
        normalizedCron.put("scheduleExpression", " 0 0/5 * * * ? * ");
        api.putJson("/monitor/job/" + jobName, normalizedCron, adminToken).expectSuccess();
        assertEquals("0 0/5 * * * ? *",
                dynamicJobRow(jobName).get("scheduleExpression"),
                "Cron 校验与运行时注册必须使用同一个规范化表达式");

        Map<String, Object> updatedPayload = dynamicJobPayload(jobName);
        updatedPayload.put("description", "已更新的动态契约任务");
        updatedPayload.put("scheduleType", "FIXED_DELAY");
        updatedPayload.put("scheduleExpression", "600000");
        updatedPayload.put("zone", "");
        updatedPayload.put("initialDelayMs", 3_600_000);
        api.putJson("/monitor/job/" + jobName, updatedPayload, adminToken).expectSuccess();
        Map<String, Object> updated = dynamicJobRow(jobName);
        assertEquals("已更新的动态契约任务", updated.get("description"));
        assertEquals("FIXED_DELAY", updated.get("scheduleType"));
        assertEquals("600000", updated.get("scheduleExpression"));

        int retryExecutions = ManagedSchedulingTestJob.executions();
        int retryLogs = jobLogs(jobName).size();
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.FAIL_ONCE);
        api.postJson("/monitor/job/" + jobName + "/run", Map.of(), adminToken)
                .expectSuccess();
        awaitExecutions(retryExecutions + 2);
        List<Map<String, Object>> retried = awaitJobLogs(jobName, retryLogs + 2).subList(0, 2);
        assertTrue(retried.stream().anyMatch(row -> "RETRY".equals(row.get("status"))
                && number(row.get("attempt")) == 1));
        assertTrue(retried.stream().anyMatch(row -> "SUCCESS".equals(row.get("status"))
                && number(row.get("attempt")) == 2));
        Object retryExecutionId = retried.get(0).get("executionId");
        assertTrue(retryExecutionId instanceof String
                        && !((String) retryExecutionId).isBlank(),
                "重试执行链必须返回非空 executionId");
        assertTrue(retried.stream().allMatch(
                        row -> retryExecutionId.equals(row.get("executionId"))),
                "同一次手工执行的 RETRY 与 SUCCESS 必须共用 executionId");

        int forbiddenExecutions = ManagedSchedulingTestJob.executions();
        int forbiddenLogs = jobLogs(jobName).size();
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.BLOCKING);
        api.postJson("/monitor/job/" + jobName + "/run", Map.of(), adminToken)
                .expectSuccess();
        assertTrue(ManagedSchedulingTestJob.awaitEntered(), "第一个动态任务必须进入处理器");
        try {
            api.postJson("/monitor/job/" + jobName + "/run", Map.of(), adminToken)
                    .expectSuccess();
            List<Map<String, Object>> competing = awaitJobLogs(
                    jobName, forbiddenLogs + 1).subList(0, 1);
            assertTrue(competing.stream().anyMatch(row -> "SKIPPED".equals(row.get("status"))),
                    "FORBID 必须跳过同一任务的并发执行");
            assertEquals(forbiddenExecutions + 1, ManagedSchedulingTestJob.executions());
        } finally {
            ManagedSchedulingTestJob.release();
        }
        awaitJobLogs(jobName, forbiddenLogs + 2);

        Map<String, Object> concurrentPayload = new LinkedHashMap<>(updatedPayload);
        concurrentPayload.put("concurrentPolicy", "ALLOW");
        concurrentPayload.put("maxRetries", 0);
        api.putJson("/monitor/job/" + jobName, concurrentPayload, adminToken).expectSuccess();
        int allowedExecutions = ManagedSchedulingTestJob.executions();
        int allowedLogs = jobLogs(jobName).size();
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.BLOCKING);
        api.postJson("/monitor/job/" + jobName + "/run", Map.of(), adminToken)
                .expectSuccess();
        assertTrue(ManagedSchedulingTestJob.awaitEntered(), "并发测试首个任务必须进入处理器");
        try {
            api.postJson("/monitor/job/" + jobName + "/run", Map.of(), adminToken)
                    .expectSuccess();
            awaitExecutions(allowedExecutions + 2);
            assertEquals(allowedExecutions + 2, ManagedSchedulingTestJob.executions(),
                    "ALLOW 必须允许同一任务并发执行");
        } finally {
            ManagedSchedulingTestJob.release();
        }
        List<Map<String, Object>> allowed = awaitJobLogs(jobName, allowedLogs + 2).subList(0, 2);
        assertTrue(allowed.stream().allMatch(row -> "SUCCESS".equals(row.get("status"))));
        Object firstAllowedExecutionId = allowed.get(0).get("executionId");
        Object secondAllowedExecutionId = allowed.get(1).get("executionId");
        assertTrue(firstAllowedExecutionId instanceof String
                        && !((String) firstAllowedExecutionId).isBlank()
                        && secondAllowedExecutionId instanceof String
                        && !((String) secondAllowedExecutionId).isBlank(),
                "并发手工执行日志必须返回非空 executionId");
        assertNotEquals(firstAllowedExecutionId, secondAllowedExecutionId,
                "两次 ALLOW 并发手工执行必须使用不同 executionId");
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.SUCCESS);

        api.putJson("/monitor/job/" + jobName + "/start", Map.of(), adminToken)
                .expectSuccess();
        assertEquals(true, dynamicJobRow(jobName).get("enabled"));
        assertTrue(Solon.context().getBean(IJobManager.class).jobGet(jobName) != null,
                "启用在线任务后必须立即注册到 Solon IJobManager");
        api.putJson("/monitor/job/" + jobName + "/stop", Map.of(), adminToken)
                .expectSuccess();
        assertEquals(false, dynamicJobRow(jobName).get("enabled"));
        assertNull(Solon.context().getBean(IJobManager.class).jobGet(jobName),
                "停用在线任务后必须立即从 Solon IJobManager 注销");

        HttpApiTestSupport.Response systemDelete = api.delete(
                "/monitor/job/" + ManagedSchedulingTestJob.JOB_NAME, adminToken).expectEnvelope();
        assertNotEquals(200, systemDelete.code(), "系统内置任务不得通过在线任务接口删除");
        assertTrue(Solon.context().getBean(IJobManager.class)
                .jobGet(ManagedSchedulingTestJob.JOB_NAME) != null);

        api.delete("/monitor/job/" + jobName, adminToken).expectSuccess();
        assertEquals(0, jobConfigCount(jobName));
        assertTrue(Solon.context().getBean(IJobManager.class).jobGet(jobName) == null,
                "删除在线任务后必须同步从 Solon IJobManager 注销");
    }

    @Test
    @Order(8)
    void preservesCronCadenceAndReconcilesDatabaseState() throws Exception {
        assertCronExecutionIsClaimedUntilItsNextFireTime();
        assertDatabaseStateIsPeriodicallyReconciled();
    }

    /** 查询指定在线任务的列表行。 */
    private Map<String, Object> dynamicJobRow(String jobName) {
        return api.get("/monitor/job/list", adminToken).expectSuccess().dataList().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(row -> (Map<String, Object>) row)
                .filter(row -> jobName.equals(row.get("jobName")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到在线任务: " + jobName));
    }

    /** 统计指定任务名的持久化配置。 */
    private long jobConfigCount(String jobName) {
        return QueryChain.of(Solon.context().getBean(SysScheduledJobConfigMapper.class))
                .eq(SysScheduledJobConfig::getJobName, jobName)
                .count();
    }

    /** 构造合法的动态任务请求。 */
    private static Map<String, Object> dynamicJobPayload(String jobName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobName", jobName);
        payload.put("description", "动态 HTTP 契约任务");
        payload.put("handlerKey", ManagedSchedulingTestJob.HANDLER_KEY);
        payload.put("scheduleType", "CRON");
        payload.put("scheduleExpression", "0 0 0 1 1 ? *");
        payload.put("zone", "Asia/Shanghai");
        payload.put("initialDelayMs", 0);
        payload.put("concurrentPolicy", "FORBID");
        payload.put("misfirePolicy", "FIRE_ONCE");
        payload.put("maxRetries", 1);
        payload.put("retryIntervalMs", 0);
        return payload;
    }

    private Map<String, Object> jobRow() {
        return api.get("/monitor/job/list", adminToken).expectSuccess().dataList().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(row -> (Map<String, Object>) row)
                .filter(row -> ManagedSchedulingTestJob.JOB_NAME.equals(row.get("jobName")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("测试定时任务必须注册到 Solon IJobManager"));
    }

    private void assertSingleScheduledExecutionAcrossConcurrentTriggers() throws Exception {
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.BLOCKING);
        int executions = ManagedSchedulingTestJob.executions();
        int logs = jobLogs().size();
        JobHolder job = Solon.context().getBean(IJobManager.class)
                .jobGet(ManagedSchedulingTestJob.JOB_NAME);
        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> invokeJob(job));
        assertTrue(ManagedSchedulingTestJob.awaitEntered(), "第一个实例必须进入任务处理器");
        long firstBucket = System.currentTimeMillis() / 1000L;
        while (System.currentTimeMillis() / 1000L == firstBucket) {
            Thread.sleep(2L);
        }
        CompletableFuture<Void> second = CompletableFuture.runAsync(() -> invokeJob(job));
        second.get();
        ManagedSchedulingTestJob.release();
        first.get();
        invokeJob(job);
        assertEquals(executions + 1, ManagedSchedulingTestJob.executions(),
                "fixedDelay 必须从上次执行完成后开始计时");
        Map<String, Object> newLog = awaitJobLogs(logs + 1).get(0);
        assertEquals("SCHEDULED", newLog.get("triggerType"));
        assertEquals("SUCCESS", newLog.get("status"));
    }

    private void assertCronExecutionIsClaimedUntilItsNextFireTime() throws Exception {
        String jobName = ManagedSchedulingTestJob.CRON_JOB_NAME;
        api.putJson("/monitor/job/" + jobName + "/start", Map.of(), adminToken).expectSuccess();
        try {
            JobHolder job = Solon.context().getBean(IJobManager.class).jobGet(jobName);
            int executions = ManagedSchedulingTestJob.cronExecutions();
            int logs = jobLogs(jobName).size();
            invokeJob(job);
            assertEquals(executions + 1, ManagedSchedulingTestJob.cronExecutions());
            Thread.sleep(1_100L);
            invokeJob(job);
            assertEquals(executions + 1, ManagedSchedulingTestJob.cronExecutions(),
                    "Cron 去重必须持续到表达式的下一次合法触发，而不是固定一秒时间桶");
            Map<String, Object> newLog = awaitJobLogs(jobName, logs + 1).get(0);
            assertEquals("SUCCESS", newLog.get("status"));
        } finally {
            api.putJson("/monitor/job/" + jobName + "/stop", Map.of(), adminToken).expectSuccess();
        }
    }

    private void assertDatabaseStateIsPeriodicallyReconciled() throws Exception {
        String jobName = ManagedSchedulingTestJob.RECONCILE_JOB_NAME;
        SysScheduledJobConfigMapper mapper = Solon.context().getBean(SysScheduledJobConfigMapper.class);
        SysScheduledJobConfig config = QueryChain.of(mapper)
                .eq(SysScheduledJobConfig::getJobName, jobName)
                .get();

        SysScheduledJobConfig enable = new SysScheduledJobConfig()
                .setConfigId(config.getConfigId())
                .setEnabled(true);
        enable.setUpdateBy(0L);
        int before = ManagedSchedulingTestJob.reconcileExecutions();
        assertEquals(1, mapper.update(enable));
        awaitReconcileExecutions(before + 1);

        SysScheduledJobConfig disable = new SysScheduledJobConfig()
                .setConfigId(config.getConfigId())
                .setEnabled(false);
        disable.setUpdateBy(0L);
        assertEquals(1, mapper.update(disable));
        Thread.sleep(ManagedSchedulingTestJob.RECONCILE_FIXED_DELAY_MS + 200L);
        int stoppedAt = ManagedSchedulingTestJob.reconcileExecutions();
        Thread.sleep(ManagedSchedulingTestJob.RECONCILE_FIXED_DELAY_MS * 2L + 100L);
        assertEquals(stoppedAt, ManagedSchedulingTestJob.reconcileExecutions(),
                "数据库直接停用后，至少两个 fixedDelay 窗口内不得再执行任务");
    }

    private void assertJobLogNameFilterIsExact() {
        SysScheduledJobLogMapper mapper = Solon.context().getBean(SysScheduledJobLogMapper.class);
        Date now = new Date();
        SysScheduledJobLog similarName = new SysScheduledJobLog()
                .setJobName(ManagedSchedulingTestJob.JOB_NAME + "Extended")
                .setStatus("SUCCESS")
                .setTriggerType("SCHEDULED")
                .setAttempt(1)
                .setInstanceId("filter-contract")
                .setStartTime(now)
                .setEndTime(now)
                .setDurationMs(0L);
        similarName.setCreateDept(0L);
        similarName.setCreateBy(0L);
        similarName.setUpdateBy(0L);
        mapper.save(similarName);
        try {
            assertTrue(jobLogs().stream().allMatch(
                            row -> ManagedSchedulingTestJob.JOB_NAME.equals(row.get("jobName"))),
                    "任务日志抽屉必须按任务名精确隔离，不能混入相似名称任务");
        } finally {
            mapper.delete(where -> where.eq(SysScheduledJobLog::getLogId, similarName.getLogId()));
        }
    }

    private List<Map<String, Object>> awaitJobLogs(int expected) throws InterruptedException {
        return awaitJobLogs(ManagedSchedulingTestJob.JOB_NAME, expected);
    }

    private List<Map<String, Object>> awaitJobLogs(String jobName, int expected) throws InterruptedException {
        List<Map<String, Object>> logs = jobLogs(jobName);
        for (int attempt = 0; attempt < 80 && logs.size() < expected; attempt++) {
            Thread.sleep(25L);
            logs = jobLogs(jobName);
        }
        assertTrue(logs.size() >= expected, "定时任务执行日志未按时写入");
        return logs;
    }

    private List<Map<String, Object>> jobLogs() {
        return jobLogs(ManagedSchedulingTestJob.JOB_NAME);
    }

    private List<Map<String, Object>> jobLogs(String jobName) {
        return pageRows(api.get("/monitor/job/log/list" + HttpApiTestSupport.query(Map.of(
                "jobName", jobName,
                "pageNum", 1,
                "pageSize", 100)), adminToken).expectPage());
    }

    private Map<String, Object> awaitFailedLoginLog() throws InterruptedException {
        Map<String, Object> query = Map.of(
                "userName", "no_permission",
                "status", "1",
                "pageNum", 1,
                "pageSize", 100);
        for (int attempt = 0; attempt < 80; attempt++) {
            List<Map<String, Object>> rows = pageRows(api.get("/monitor/loginInfo/list"
                    + HttpApiTestSupport.query(query), adminToken).expectPage());
            if (!rows.isEmpty()) {
                return rows.get(0);
            }
            Thread.sleep(25L);
        }
        throw new AssertionError("失败登录必须写入登录日志");
    }

    private void awaitExecutions(int expected) throws InterruptedException {
        for (int attempt = 0; attempt < 80 && ManagedSchedulingTestJob.executions() < expected; attempt++) {
            Thread.sleep(25L);
        }
    }

    private static void awaitReconcileExecutions(int expected) throws InterruptedException {
        for (int attempt = 0;
             attempt < 80 && ManagedSchedulingTestJob.reconcileExecutions() < expected;
             attempt++) {
            Thread.sleep(25L);
        }
        assertTrue(ManagedSchedulingTestJob.reconcileExecutions() >= expected,
                "数据库直接启用后，周期对账必须真正启动任务");
    }

    private static void invokeJob(JobHolder job) {
        try {
            job.handle(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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
