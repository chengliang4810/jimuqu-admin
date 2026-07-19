package com.jimuqu.test.http;

import com.jimuqu.Application;
import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.noear.solon.Solon;
import org.noear.solon.test.SolonTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CORS、XSS、限流、幂等与方法约束的真实 HTTP 行为测试。 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebSecurityHttpContractTest {

    private HttpApiTestSupport api;
    private String adminToken;
    private String suffix;

    @BeforeAll
    void setUp() {
        api = new HttpApiTestSupport(route -> "/resource/sms/code".equals(route.path()));
        adminToken = api.loginAdmin();
        suffix = Long.toUnsignedString(System.nanoTime(), 36);
    }

    @AfterAll
    void logout() {
        try {
            if (adminToken != null) {
                api.postJson("/auth/logout", Map.of(), adminToken).expectSuccess();
            }
        } finally {
            api.assertCoverageComplete();
        }
    }

    @Test
    @Order(1)
    void corsPreflightRunsBeforeAuthenticationAndAuthenticated405UsesEnvelope() {
        String origin = "http://127.0.0.1:15555";
        HttpApiTestSupport.Response preflight = api.requestWithHeaders(
                "OPTIONS", "/system/user/profile", null, null, null, Map.of(
                        "Origin", origin,
                        "Access-Control-Request-Method", "PUT",
                        "Access-Control-Request-Headers", "Authorization,Content-Type"));

        assertTrue(preflight.statusCode() == 200 || preflight.statusCode() == 204,
                "CORS 预检必须在认证前成功，实际状态: " + preflight.statusCode());
        assertEquals(origin, preflight.header("Access-Control-Allow-Origin"));
        assertTrue(preflight.header("Access-Control-Allow-Methods").contains("PUT"));
        assertTrue(preflight.header("Access-Control-Allow-Credentials").contains("true"));

        api.postJson("/auth/code", Map.of(), adminToken)
                .expectFailure(200, 405, "Method Not Allowed");
    }

    @Test
    @Order(2)
    void xssFilterCleansJsonBeforeAWriteIsPersisted() {
        String key = "security.xss." + suffix;
        String unsafeName = "安全<script>alert(1)</script>配置";
        api.postJson("/system/config", Map.of(
                "configName", unsafeName,
                "configKey", key,
                "configValue", "enabled",
                "configType", "N",
                "remark", "HTTP security contract"), adminToken).expectSuccess();
        try {
            HttpApiTestSupport.Response page = api.get("/system/config/list"
                    + HttpApiTestSupport.query(Map.of(
                    "configKey", key,
                    "pageNum", 1,
                    "pageSize", 10)), adminToken).expectPage();
            List<Map<String, Object>> rows = rows(page);
            assertEquals(1, rows.size());
            String storedName = String.valueOf(rows.get(0).get("configName"));
            assertFalse(storedName.contains("<") || storedName.contains(">"),
                    "XSS 标签不得写入数据库: " + storedName);
            assertTrue(storedName.contains("alert(1)"), "清洗应移除标签而不是静默丢弃文本内容");
        } finally {
            HttpApiTestSupport.Response page = api.get("/system/config/list"
                    + HttpApiTestSupport.query(Map.of(
                    "configKey", key,
                    "pageNum", 1,
                    "pageSize", 10)), adminToken).expectPage();
            for (Map<String, Object> row : rows(page)) {
                api.delete("/system/config/" + row.get("configId"), adminToken).expectSuccess();
            }
        }
    }

    @Test
    @Order(3)
    void duplicateWritesAndCaptchaRateLimitsAreRejectedOverHttp() {
        Map<String, Object> profile = api.get("/system/user/profile", adminToken)
                .expectSuccess().dataObject();
        Map<String, Object> original = new LinkedHashMap<>(object(profile.get("user")));
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("nickName", "幂等验证-" + suffix);
        update.put("email", original.get("email"));
        update.put("phoneNumber", original.get("phoneNumber"));
        update.put("sex", original.get("sex"));
        try {
            api.putJson("/system/user/profile", update, adminToken).expectSuccess();
            api.putJson("/system/user/profile", update, adminToken)
                    .expectFailure(200, 500, "不允许重复提交");
        } finally {
            Map<String, Object> restore = new LinkedHashMap<>();
            restore.put("nickName", original.get("nickName"));
            restore.put("email", original.get("email"));
            restore.put("phoneNumber", original.get("phoneNumber"));
            restore.put("sex", original.get("sex"));
            api.putJson("/system/user/profile", restore, adminToken).expectSuccess();
        }

        RateLimitConfig config = Solon.context().getBean(RateLimitConfig.class);
        boolean enabled = config.isEnabled();
        String phone = "137" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        config.setEnabled(true);
        try {
            api.get("/resource/sms/code?phoneNumber=" + phone).expectSuccess();
            api.get("/resource/sms/code?phoneNumber=" + phone)
                    .expectFailure(200, 500, config.getErrorMessage());
        } finally {
            config.setEnabled(enabled);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        assertTrue(value instanceof Map<?, ?>, "预期 JSON 对象，实际为: " + value);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(HttpApiTestSupport.Response response) {
        return (List<Map<String, Object>>) (List<?>) response.dataObject().get("rows");
    }
}
