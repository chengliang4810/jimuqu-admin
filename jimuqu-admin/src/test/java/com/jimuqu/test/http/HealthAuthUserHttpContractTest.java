package com.jimuqu.test.http;

import com.jimuqu.Application;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.test.coverage.RuntimeRouteCoverage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.noear.solon.Solon;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.test.SolonTest;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Health、认证、用户、个人资料和社会化账号的真实 HTTP 契约测试。
 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HealthAuthUserHttpContractTest {

    private static final long MISSING_ID = 9_223_372_036_854_775_000L;

    private HttpApiTestSupport http;
    private String adminToken;
    private String restrictedToken;
    private final String suffix = Long.toUnsignedString(System.nanoTime(), 36);
    private final String registeredUsername = "reg_" + suffix;
    private final String createdUsername = "http_" + suffix;
    private boolean registeredUserCreated;
    private boolean createdUserCreated;
    private boolean importedUserCreated;
    private Long registeredUserId;
    private Long createdUserId;
    private Long importedUserId;
    private final String importedUsername = "excel_" + suffix;
    private final String securityClientKey = "auth_security_" + suffix;
    private final String securityClientSecret = "secret_" + suffix;
    private Long securityClientPk;
    private String securityClientId;
    private boolean securityClientCreated;

    @BeforeAll
    void setUp() {
        http = new HttpApiTestSupport(HealthAuthUserHttpContractTest::ownsRoute);
    }

    @AfterAll
    void cleanUpAndVerifyRouteCoverage() {
        try {
            cleanUpTemporaryUsers();
        } finally {
            http.assertCoverageComplete();
        }
    }

    @Test
    @Order(1)
    void anonymousHealthAndAuthenticationContracts() {
        HttpApiTestSupport.Response health = http.get("/");
        HttpApiTestSupport.Response captcha = http.get("/auth/code");
        HttpApiTestSupport.Response invalidEmail = http.get("/resource/email/code?email=invalid");
        HttpApiTestSupport.Response invalidPhone = http.get("/resource/sms/code?phoneNumber=invalid");
        HttpApiTestSupport.Response unsupportedBinding = http.get("/auth/binding/not-configured");
        HttpApiTestSupport.Response invalidClient = http.postEncryptedJson("/auth/login", http.withCaptcha(Map.of(
                "clientId", "invalid-client",
                "grantType", "password",
                "username", HttpApiTestSupport.ADMIN_USERNAME,
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        )));
        HttpApiTestSupport.Response englishInvalidClient = http.postEncryptedJsonWithHeaders(
                "/auth/login", http.withCaptcha(Map.of(
                        "clientId", "invalid-client",
                        "grantType", "password",
                        "username", HttpApiTestSupport.ADMIN_USERNAME,
                        "password", HttpApiTestSupport.DEFAULT_PASSWORD
                )), Map.of("Content-Language", "en-US"));
        HttpApiTestSupport.Response disabledUser = http.postEncryptedJson("/auth/login", http.withCaptcha(Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", "disabled_user",
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        )));
        HttpApiTestSupport.Response invalidUsernameLogin = http.postEncryptedJson("/auth/login", http.withCaptcha(Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", "x",
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        )));
        HttpApiTestSupport.Response unauthenticatedCodes = http.get("/auth/codes");
        HttpApiTestSupport.Response unauthenticatedCallback = http.postJson("/auth/social/callback", Map.of(
                "source", "not-configured",
                "socialCode", "invalid",
                "socialState", "invalid"
        ));
        HttpApiTestSupport.Response unauthenticatedUnlock = http.delete("/auth/unlock/999999");
        HttpApiTestSupport.Response disabledRegister = http.postEncryptedJson("/auth/register", http.withCaptcha(Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", registeredUsername,
                "password", "Register123!",
                "userType", "sys_user"
        )));
        HttpApiTestSupport.Response plainLogin = http.postJson("/auth/login", Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", HttpApiTestSupport.ADMIN_USERNAME,
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        ));
        HttpApiTestSupport.Response plainRegister = http.postJson("/auth/register", Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", registeredUsername,
                "password", "Register123!",
                "userType", "sys_user"
        ));
        HttpApiTestSupport.Response malformedEncryptedJson = http.postEncryptedJson("/auth/login", "{invalid-json");
        HttpApiTestSupport.Response unauthenticatedMissingRoute = http.get("/__missing_http_contract__");
        HttpApiTestSupport.Response methodNotAllowed = http.postJson("/auth/code", Map.of());
        String missingUsername = "ghost_" + suffix;
        HttpApiTestSupport.Response englishMissingUser = http.postEncryptedJsonWithHeaders(
                "/auth/login", http.withCaptcha(Map.of(
                        "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                        "grantType", "password",
                        "username", missingUsername,
                        "password", HttpApiTestSupport.DEFAULT_PASSWORD
                )), Map.of(
                        "Content-Language", "en-US",
                        "Accept-Language", "zh-CN"
                ));
        HttpApiTestSupport.Response englishPasswordRetry = http.postEncryptedJsonWithHeaders(
                "/auth/login", http.withCaptcha(Map.of(
                        "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                        "grantType", "password",
                        "username", HttpApiTestSupport.ADMIN_USERNAME,
                        "password", "incorrect-password"
                )), Map.of("Content-Language", "en-US"));
        HttpApiTestSupport.Response chinesePasswordRetry = http.postEncryptedJsonWithHeaders(
                "/auth/login", http.withCaptcha(Map.of(
                        "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                        "grantType", "password",
                        "username", "self_user",
                        "password", "incorrect-password"
                )), Map.of(
                        "Content-Language", "zh-CN",
                        "Accept-Language", "en-US"
                ));

        health.expectSuccess();
        assertEquals("欢迎使用jimuqu-admin后台管理框架，请通过前端地址访问。",
                health.json().get("data"));
        captcha.expectStatus(200).expectSuccess();
        assertInstanceOf(Boolean.class, captcha.dataObject().get("captchaEnabled"));
        invalidEmail.expectStatus(200).expectCode(500);
        invalidPhone.expectStatus(200).expectCode(500);
        unsupportedBinding.expectStatus(200).expectCode(500);
        invalidClient.expectStatus(200).expectCode(500);
        englishInvalidClient.expectStatus(200).expectCode(500);
        assertEquals("Auth grant type error", englishInvalidClient.json().get("msg"));
        disabledUser.expectStatus(200).expectCode(500);
        invalidUsernameLogin.expectFailure(200, 500, "账户长度必须在2到30个字符之间");
        assertEquals("账户长度必须在2到30个字符之间", invalidUsernameLogin.json().get("msg"));
        unauthenticatedCodes.expectStatus(401).expectCode(401);
        unauthenticatedCallback.expectStatus(401).expectCode(401);
        unauthenticatedUnlock.expectStatus(401).expectCode(401);
        disabledRegister.expectStatus(200).expectCode(500);
        assertEquals("当前系统没有开启注册功能！", disabledRegister.json().get("msg"));
        plainLogin.expectFailure(200, 403, "没有访问权限，请联系管理员授权");
        plainRegister.expectFailure(200, 403, "没有访问权限，请联系管理员授权");
        malformedEncryptedJson.expectFailure(200, 400, "请求数据格式错误");
        unauthenticatedMissingRoute.expectStatus(401).expectCode(401);
        methodNotAllowed.expectStatus(401).expectCode(401);
        englishMissingUser.expectStatus(200).expectCode(500);
        assertEquals("Sorry, your account: " + missingUsername + " does not exist",
                englishMissingUser.json().get("msg"),
                "Content-Language 必须优先于 Accept-Language");
        englishPasswordRetry.expectStatus(200).expectCode(500);
        assertEquals("Password input error 1 times", englishPasswordRetry.json().get("msg"));
        assertFalse(String.valueOf(englishPasswordRetry.json().get("msg"))
                .contains("user.password.retry.limit.count"), "认证错误不得泄露 i18n 消息键");
        chinesePasswordRetry.expectStatus(200).expectCode(500);
        assertEquals("密码输入错误1次", chinesePasswordRetry.json().get("msg"));
    }

    @Test
    @Order(2)
    void loginRegisterAndAuthenticatedAuthContracts() {
        HttpApiTestSupport.Response login = http.postEncryptedJson("/auth/login", http.withCaptcha(Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", HttpApiTestSupport.ADMIN_USERNAME,
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        )));
        login.expectStatus(200).expectSuccess();
        Object expireIn = login.dataObject().get("expire_in");
        assertInstanceOf(Number.class, expireIn, "expire_in 必须是 JSON Number");
        assertTrue(((Number) expireIn).intValue() != 0, "expire_in 必须表示有效的令牌 TTL");
        adminToken = login.dataString("access_token");
        http.get("/__missing_http_contract__", adminToken)
                .expectFailure(200, 404, "请求地址不存在");

        try (HttpApiTestSupport.SseSubscription stream = http.openSse("/resource/message", adminToken)) {
            stream.expectEvent("connected", "");
            stream.expectBellMessage("message", "backend",
                    DateUtil.getTodayHour(new Date(System.currentTimeMillis() + 5_000L))
                            + "好，欢迎登录积木区后台管理系统");
        }

        http.get("/resource/sms/code?phoneNumber=13800000002").expectSuccess();
        HttpApiTestSupport.Response smsLogin = http.postEncryptedJson("/auth/login", Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "sms",
                "phoneNumber", "13800000002",
                "smsCode", "1234"
        )).expectSuccess();
        assertEquals(HttpApiTestSupport.PC_CLIENT_ID, smsLogin.dataString("client_id"));
        String smsToken = smsLogin.dataString("access_token");
        http.get("/auth/codes", smsToken).expectSuccess();
        http.postJson("/auth/logout", Map.of(), smsToken).expectSuccess();

        http.get("/resource/email/code?email=dept_user@jimuqu.local").expectSuccess();
        HttpApiTestSupport.Response emailLogin = http.postEncryptedJson("/auth/login", Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "email",
                "email", "dept_user@jimuqu.local",
                "emailCode", "1234"
        )).expectSuccess();
        assertEquals(HttpApiTestSupport.PC_CLIENT_ID, emailLogin.dataString("client_id"));
        String emailToken = emailLogin.dataString("access_token");
        http.get("/auth/codes", emailToken).expectSuccess();
        http.postJson("/auth/logout", Map.of(), emailToken).expectSuccess();

        HttpApiTestSupport.Response codes = http.get("/auth/codes", adminToken);
        HttpApiTestSupport.Response callback = http.postJson("/auth/social/callback", Map.of(
                "source", "not-configured",
                "socialCode", "invalid",
                "socialState", "invalid"
        ), adminToken);
        HttpApiTestSupport.Response unlockMissing = http.delete("/auth/unlock/999999", adminToken);
        http.putJson("/system/config/updateByKey", Map.of(
                "configKey", "sys.account.registerUser",
                "configValue", "true"
        ), adminToken).expectSuccess();
        HttpApiTestSupport.Response register;
        try {
            HttpApiTestSupport.Response invalidRegister = http.postEncryptedJson("/auth/register", http.withCaptcha(Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "password",
                    "username", "x",
                    "password", "Register123!",
                    "userType", "sys_user"
            )));
            invalidRegister.expectFailure(200, 500, "账户长度必须在2到30个字符之间");
            assertEquals("账户长度必须在2到30个字符之间", invalidRegister.json().get("msg"));
            assertTrue(rows(http.get("/system/user/list" + HttpApiTestSupport.query(Map.of(
                    "pageNum", 1, "pageSize", 10, "userName", "x")), adminToken).expectPage()).isEmpty(),
                    "非法注册失败后不得创建用户");
            register = http.postEncryptedJson("/auth/register", http.withCaptcha(Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "password",
                    "username", registeredUsername,
                    "password", "Register123!",
                    "userType", "sys_user"
            )));
        } finally {
            http.putJson("/system/config/updateByKey", Map.of(
                    "configKey", "sys.account.registerUser",
                    "configValue", "false"
            ), adminToken).expectSuccess();
        }

        codes.expectStatus(200).expectSuccess();
        assertTrue(codes.dataList().contains("*:*:*"));
        callback.expectStatus(200).expectCode(500);
        unlockMissing.expectStatus(200).expectCode(500);
        register.expectStatus(200).expectSuccess();
        registeredUserCreated = true;
        registeredUserId = findUserId(http.get(
                "/system/user/list" + HttpApiTestSupport.query(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "userName", registeredUsername
                )), adminToken).expectPage(), registeredUsername);

        exerciseSocialAuthenticationContract();
    }

    @Test
    @Order(3)
    void userReadProfileAndSocialContracts() {
        assertNotNull(adminToken, "管理员登录必须先成功");

        HttpApiTestSupport.Response list = http.get("/system/user/list?pageNum=1&pageSize=10", adminToken);
        HttpApiTestSupport.Response byDept = http.get("/system/user/list/dept/103", adminToken);
        HttpApiTestSupport.Response detail = http.get("/system/user/2", adminToken);
        HttpApiTestSupport.Response health = http.get("/", adminToken);
        HttpApiTestSupport.Response createInfo = http.get("/system/user/", adminToken);
        HttpApiTestSupport.Response currentInfo = http.get("/system/user/getInfo", adminToken);
        HttpApiTestSupport.Response authRole = http.get("/system/user/authRole/2", adminToken);
        HttpApiTestSupport.Response unlock = http.get("/system/user/unlock/2", adminToken);
        HttpApiTestSupport.Response deptTree = http.get("/system/user/deptTree", adminToken);
        HttpApiTestSupport.Response optionselect = http.get("/system/user/optionselect", adminToken);
        HttpApiTestSupport.Response profile = http.get("/system/user/profile", adminToken);
        HttpApiTestSupport.Response social = http.get("/system/social/list", adminToken);

        list.expectStatus(200).expectPage();
        assertFalse(rows(list).isEmpty(), "种子用户列表不能为空");
        HttpApiTestSupport.Response futureCreated = http.get(
                "/system/user/list" + HttpApiTestSupport.query(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "params[beginTime]", "2999-01-01 00:00:00",
                        "params[endTime]", "2999-12-31 23:59:59"
                )), adminToken).expectPage();
        assertTrue(rows(futureCreated).isEmpty(), "Bell 创建时间范围必须实际参与用户查询");
        assertEquals(0L, ((Number) futureCreated.dataObject().get("total")).longValue());
        HttpApiTestSupport.Response explicitParams = http.get(
                "/system/user/list" + HttpApiTestSupport.query(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "params", "{\"beginTime\":\"2999-01-01 00:00:00\","
                                + "\"endTime\":\"2999-12-31 23:59:59\"}",
                        "params[beginTime]", "2000-01-01 00:00:00",
                        "params[endTime]", "2100-12-31 23:59:59"
                )), adminToken).expectPage();
        assertTrue(rows(explicitParams).isEmpty(), "显式 params JSON 不得被括号参数覆盖");
        byDept.expectStatus(200).expectSuccess();
        assertFalse(byDept.dataList().isEmpty(), "研发部门种子用户不能为空");
        detail.expectStatus(200).expectSuccess();
        assertEquals("custom_user", object(detail.dataObject().get("user")).get("userName"));
        health.expectStatus(200).expectSuccess();
        assertEquals("欢迎使用jimuqu-admin后台管理框架，请通过前端地址访问。",
                health.json().get("data"));
        createInfo.expectStatus(200).expectSuccess();
        assertTrue(createInfo.dataObject().containsKey("roles"));
        currentInfo.expectStatus(200).expectSuccess();
        Map<String, Object> currentUser = object(currentInfo.dataObject().get("user"));
        assertEquals("admin", currentUser.get("userName"));
        assertEquals("admin@jimuqu.local", currentUser.get("email"), "超级管理员必须可查看邮箱原文");
        assertEquals("15888888888", currentUser.get("phoneNumber"), "超级管理员必须可查看手机号原文");
        assertEquals(List.of("superadmin"), currentInfo.dataObject().get("roles"));
        authRole.expectStatus(200).expectSuccess();
        assertEquals("custom_user", object(authRole.dataObject().get("user")).get("userName"));
        unlock.expectStatus(200).expectSuccess();
        deptTree.expectStatus(200).expectSuccess();
        assertFalse(deptTree.dataList().isEmpty(), "部门树不能为空");
        optionselect.expectStatus(200).expectSuccess();
        assertTrue(optionselect.dataList().stream().anyMatch(item -> item instanceof Map<?, ?> map
                        && "admin".equals(String.valueOf(map.get("userName")))),
                "用户候选列表必须包含管理员");
        assertFalse(optionselect.dataList().stream().anyMatch(item -> item instanceof Map<?, ?> map
                        && "disabled_user".equals(String.valueOf(map.get("userName")))),
                "用户候选列表不得包含停用账号");
        profile.expectStatus(200).expectSuccess();
        assertEquals("admin", object(profile.dataObject().get("user")).get("userName"));
        social.expectStatus(200).expectSuccess();
    }

    @Test
    @Order(7)
    void unauthorizedAndInvalidUserWritesAreRejected() {
        restrictedToken = http.login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);

        HttpApiTestSupport.Response unauthenticatedProfile = http.get("/system/user/profile");
        HttpApiTestSupport.Response forbiddenList = http.get("/system/user/list?pageNum=1&pageSize=10", restrictedToken);
        HttpApiTestSupport.Response restrictedInfo = http.get("/system/user/getInfo", restrictedToken);
        String invalidUsername = "invalid_" + suffix;
        HttpApiTestSupport.Response invalidAdd = http.postJson("/system/user",
                Map.of("userName", invalidUsername), adminToken);
        HttpApiTestSupport.Response invalidSelfDelete = http.delete("/system/user/1", adminToken);

        unauthenticatedProfile.expectStatus(401).expectCode(401);
        forbiddenList.expectStatus(403).expectCode(403);
        restrictedInfo.expectStatus(200).expectSuccess();
        Map<String, Object> restrictedUser = object(restrictedInfo.dataObject().get("user"));
        assertEquals("n***@jimuqu.local", restrictedUser.get("email"));
        assertEquals("138****0007", restrictedUser.get("phoneNumber"));
        invalidAdd.expectStatus(200).expectCode(500);
        assertTrue(rows(http.get("/system/user/list" + HttpApiTestSupport.query(Map.of(
                "pageNum", 1, "pageSize", 10, "userName", invalidUsername)), adminToken).expectPage()).isEmpty(),
                "非法用户新增失败后不得留下记录");
        invalidSelfDelete.expectStatus(200).expectCode(500);
        http.get("/system/user/1", adminToken).expectSuccess();
    }

    @Test
    @Order(6)
    void userWriteProfileAndExportContracts() throws Exception {
        Map<String, Object> newUser = Map.of(
                "deptId", 103,
                "userName", createdUsername,
                "nickName", "HTTP契约用户",
                "password", "Created123!",
                "status", "0",
                "roleIds", List.of(5),
                "postIds", List.of()
        );
        HttpApiTestSupport.Response add = http.postJson("/system/user", newUser, adminToken);
        add.expectStatus(200).expectSuccess();
        createdUserCreated = true;

        HttpApiTestSupport.Response createdPage = http.get(
                "/system/user/list" + HttpApiTestSupport.query(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "userName", createdUsername
                )), adminToken).expectPage();
        createdUserId = findUserId(createdPage, createdUsername);

        HttpApiTestSupport.Response edit = http.putJson("/system/user", Map.of(
                "userId", createdUserId,
                "deptId", 103,
                "userName", createdUsername,
                "nickName", "HTTP契约用户已更新",
                "email", createdUsername + "@jimuqu.local",
                "phoneNumber", "13912345678",
                "status", "0",
                "roleIds", List.of(5),
                "postIds", List.of()
        ), adminToken);
        HttpApiTestSupport.Response plainResetPassword = http.putJson("/system/user/resetPwd", Map.of(
                "userId", createdUserId,
                "password", "MustNotApply123!"
        ), adminToken);
        HttpApiTestSupport.Response resetPassword = http.putEncryptedJson("/system/user/resetPwd", Map.of(
                "userId", createdUserId,
                "password", "Reset123!"
        ), adminToken);
        HttpApiTestSupport.Response changeStatus = http.putJson("/system/user/changeStatus", Map.of(
                "userId", createdUserId,
                "status", "0"
        ), adminToken);
        HttpApiTestSupport.Response grantRole = http.putJson("/system/user/authRole", Map.of(
                "userId", createdUserId,
                "roleIds", List.of(5)
        ), adminToken);
        grantRole.expectStatus(200).expectSuccess();
        Map<String, Object> associationsBeforeInvalidWrites = http.get(
                "/system/user/" + createdUserId, adminToken).expectSuccess().dataObject();
        assertEquals(Set.of(5L), numericIds(associationsBeforeInvalidWrites.get("roleIds")));
        assertTrue(numericIds(associationsBeforeInvalidWrites.get("postIds")).isEmpty());

        HttpApiTestSupport.Response invalidRole = http.putJson("/system/user/authRole", Map.of(
                "userId", createdUserId,
                "roleIds", List.of(MISSING_ID)
        ), adminToken).expectEnvelope();
        assertTrue(invalidRole.code() != 200, "不存在的角色不得覆盖用户原角色");
        assertEquals(Set.of(5L), numericIds(http.get("/system/user/" + createdUserId, adminToken)
                .expectSuccess().dataObject().get("roleIds")));

        HttpApiTestSupport.Response invalidPost = http.putJson("/system/user", Map.of(
                "userId", createdUserId,
                "deptId", 103,
                "userName", createdUsername,
                "nickName", "不应写入",
                "email", createdUsername + "@jimuqu.local",
                "phoneNumber", "13912345678",
                "status", "0",
                "roleIds", List.of(2),
                "postIds", List.of(MISSING_ID)
        ), adminToken).expectEnvelope();
        assertTrue(invalidPost.code() != 200, "不存在的岗位不得覆盖用户原关联");
        Map<String, Object> associationsAfterInvalidPost = http.get(
                "/system/user/" + createdUserId, adminToken).expectSuccess().dataObject();
        assertEquals(Set.of(5L), numericIds(associationsAfterInvalidPost.get("roleIds")),
                "岗位校验失败后角色替换必须回滚");
        assertTrue(numericIds(associationsAfterInvalidPost.get("postIds")).isEmpty(),
                "岗位校验失败后岗位关联必须保持不变");
        assertEquals("HTTP契约用户已更新", object(associationsAfterInvalidPost.get("user")).get("nickName"),
                "岗位校验失败后用户字段必须保持不变");
        HttpApiTestSupport.Response updateProfile = http.putJson("/system/user/profile", Map.of(
                "nickName", "系统管理员",
                "email", "admin@jimuqu.local",
                "phoneNumber", "15888888888",
                "sex", "0"
        ), adminToken);
        HttpApiTestSupport.Response plainUpdatePassword = http.putJson("/system/user/profile/updatePwd", Map.of(
                "oldPassword", HttpApiTestSupport.DEFAULT_PASSWORD,
                "newPassword", "MustNotApply123!"
        ), adminToken);
        HttpApiTestSupport.Response updatePassword = http.putEncryptedJson("/system/user/profile/updatePwd", Map.of(
                "oldPassword", HttpApiTestSupport.DEFAULT_PASSWORD,
                "newPassword", "Temporary123!"
        ), adminToken);
        HttpApiTestSupport.Response restorePassword = http.putEncryptedJson("/system/user/profile/updatePwd", Map.of(
                "oldPassword", "Temporary123!",
                "newPassword", HttpApiTestSupport.DEFAULT_PASSWORD
        ), adminToken);
        HttpApiTestSupport.Response export = http.postForm("/system/user/export", Map.of(), adminToken);
        HttpApiTestSupport.Response delete = http.delete("/system/user/" + createdUserId, adminToken);

        edit.expectStatus(200).expectSuccess();
        plainResetPassword.expectFailure(200, 403, "没有访问权限，请联系管理员授权");
        resetPassword.expectStatus(200).expectSuccess();
        changeStatus.expectStatus(200).expectSuccess();
        updateProfile.expectStatus(200).expectSuccess();
        plainUpdatePassword.expectFailure(200, 403, "没有访问权限，请联系管理员授权");
        updatePassword.expectStatus(200).expectSuccess();
        restorePassword.expectStatus(200).expectSuccess();
        export.expectSpreadsheet();
        assertUserExportContent(export.bytes());
        delete.expectStatus(200).expectSuccess();
        createdUserCreated = false;
        createdUserId = null;
    }

    private void assertUserExportContent(byte[] workbookBytes) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            Sheet sheet = workbook.getSheet("用户数据");
            assertNotNull(sheet, "用户导出缺少用户数据工作表");
            org.apache.poi.ss.usermodel.Row header = sheet.getRow(0);
            org.apache.poi.ss.usermodel.Row admin = null;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
                if (row != null && "admin".equals(cellByHeader(header, row, "用户账号"))) {
                    admin = row;
                    break;
                }
            }
            assertNotNull(admin, "用户导出缺少 admin 行");
            assertEquals("积木区科技/总部/研发部", cellByHeader(header, admin, "部门名称"));
            assertEquals("admin", cellByHeader(header, admin, "部门负责人"));
        }
    }

    private static String cellByHeader(org.apache.poi.ss.usermodel.Row header,
                                       org.apache.poi.ss.usermodel.Row row, String name) {
        for (org.apache.poi.ss.usermodel.Cell cell : header) {
            if (name.equals(cell.getStringCellValue())) {
                org.apache.poi.ss.usermodel.Cell value = row.getCell(cell.getColumnIndex());
                return value == null ? "" : value.getStringCellValue();
            }
        }
        throw new AssertionError("工作簿缺少列：" + name);
    }

    @Test
    @Order(5)
    void userImportTemplateAndUploadContracts() throws Exception {
        HttpApiTestSupport.Response template = http.request(
                "POST", "/system/user/importTemplate", null, null, adminToken).expectSpreadsheet();
        assertDepartmentDropDown(template.bytes());

        HttpApiTestSupport.Response imported = uploadUserWorkbook(
                template.bytes(), importedUsername, "Excel导入用户", false);
        importedUserCreated = true;
        imported.expectSuccess();
        assertTrue(String.valueOf(imported.json().get("msg")).contains("1"));

        HttpApiTestSupport.Response createdPage = http.get(
                "/system/user/list" + HttpApiTestSupport.query(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "userName", importedUsername
                )), adminToken).expectPage();
        importedUserId = findUserId(createdPage, importedUsername);
        assertEquals("Excel导入用户", object(rows(createdPage).get(0)).get("nickName"));

        uploadUserWorkbook(template.bytes(), importedUsername, "Excel覆盖用户", true).expectSuccess();
        HttpApiTestSupport.Response updatedPage = http.get(
                "/system/user/list" + HttpApiTestSupport.query(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "userName", importedUsername
                )), adminToken).expectPage();
        assertEquals("Excel覆盖用户", object(rows(updatedPage).get(0)).get("nickName"));

        http.delete("/system/user/" + importedUserId, adminToken).expectSuccess();
        importedUserCreated = false;
        importedUserId = null;
    }

    private void assertDepartmentDropDown(byte[] workbookBytes) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            Sheet sheet = workbook.getSheet("用户数据");
            assertNotNull(sheet, "用户导入模板缺少用户数据工作表");
            DataValidation validation = sheet.getDataValidations().stream()
                    .filter(item -> List.of(item.getRegions().getCellRangeAddresses()).stream()
                            .anyMatch(range -> range.getFirstColumn() <= 1 && range.getLastColumn() >= 1))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("部门名称列缺少动态下拉数据校验"));
            String[] values = validation.getValidationConstraint().getExplicitListValues();
            assertNotNull(values, "当前部门下拉应使用显式列表");
            assertTrue(List.of(values).contains("积木区科技/总部/研发部/平台组"),
                    "部门下拉缺少完整部门路径");
        }
    }

    private void exerciseSocialAuthenticationContract() {
        assertTrue(http.get("/system/social/list", adminToken).expectSuccess().dataList().isEmpty(),
                "新测试库不应预置社会化账号");
        try {
            Map<String, String> bindingCallback = socialCallbackFromAuthorizeUrl();
            http.postJson("/auth/social/callback", Map.of(
                    "source", "gitee",
                    "socialCode", bindingCallback.get("code"),
                    "socialState", bindingCallback.get("state")
            ), adminToken).expectSuccess();

            HttpApiTestSupport.Response socialList = http.get("/system/social/list", adminToken).expectSuccess();
            assertEquals(1, socialList.dataList().size(), "绑定成功后应返回一个社会化账号");
            Map<String, Object> social = object(socialList.dataList().get(0));
            assertEquals("gitee", social.get("source"));
            assertEquals("http_contract_social", social.get("userName"));
            assertEquals("http-contract-social@jimuqu.test", social.get("email"));
            long socialId = Long.parseLong(String.valueOf(social.get("id")));

            String otherUserToken = http.login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);
            try {
                http.delete("/auth/unlock/" + socialId, otherUserToken)
                        .expectFailure(200, 500, "取消授权失败");
                assertEquals(1, http.get("/system/social/list", adminToken).expectSuccess().dataList().size(),
                        "其他用户不得解绑当前用户的社会化账号");
            } finally {
                http.postJson("/auth/logout", Map.of(), otherUserToken).expectSuccess();
            }

            Map<String, String> loginCallback = socialCallbackFromAuthorizeUrl();
            HttpApiTestSupport.Response socialLogin = http.postEncryptedJson("/auth/login", Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "social",
                    "source", "gitee",
                    "socialCode", loginCallback.get("code"),
                    "socialState", loginCallback.get("state")
            )).expectSuccess();
            String socialToken = socialLogin.dataString("access_token");
            http.get("/system/user/getInfo", socialToken).expectSuccess();
            http.postJson("/auth/logout", Map.of(), socialToken).expectSuccess();

            http.delete("/auth/unlock/" + socialId, adminToken).expectSuccess();
            assertTrue(http.get("/system/social/list", adminToken).expectSuccess().dataList().isEmpty(),
                    "解绑后社会化账号列表必须为空");

            Map<String, String> unboundCallback = socialCallbackFromAuthorizeUrl();
            HttpApiTestSupport.Response unboundLogin = http.postEncryptedJson("/auth/login", Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "social",
                    "source", "gitee",
                    "socialCode", unboundCallback.get("code"),
                    "socialState", unboundCallback.get("state")
            ));
            unboundLogin.expectStatus(200).expectCode(500);
            assertEquals("你还没有绑定第三方账号，绑定后才可以登录！", unboundLogin.json().get("msg"));
        } finally {
            for (Object item : http.get("/system/social/list", adminToken).expectSuccess().dataList()) {
                Map<String, Object> social = object(item);
                if ("gitee".equals(social.get("source"))
                        && "http_contract_social".equals(social.get("userName"))) {
                    http.delete("/auth/unlock/" + Long.parseLong(String.valueOf(social.get("id"))), adminToken)
                            .expectSuccess();
                }
            }
        }
    }

    private Map<String, String> socialCallbackFromAuthorizeUrl() {
        HttpApiTestSupport.Response binding = http.get("/auth/binding/gitee").expectSuccess();
        String authorizeUrl = String.valueOf(binding.json().get("data"));
        String query = URI.create(authorizeUrl).getRawQuery();
        Map<String, String> parameters = new java.util.LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            parameters.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts.length == 2 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        assertEquals("gitee", parameters.get("source"));
        assertEquals("http-contract-code", parameters.get("code"));
        assertTrue(parameters.get("state") != null && !parameters.get("state").isBlank(),
                "JustAuth 授权地址必须包含 state");
        return parameters;
    }

    @Test
    @Order(4)
    void userListAppliesAllFiveDataScopes() {
        deleteTemporaryUser(registeredUserId, registeredUsername, adminToken);
        registeredUserCreated = false;
        registeredUserId = null;

        Map<String, Set<String>> expectedByUsername = Map.of(
                "admin", Set.of("admin", "custom_user", "dept_child_user", "dept_user",
                        "disabled_user", "no_permission", "self_user"),
                "custom_user", Set.of("self_user"),
                "dept_user", Set.of("admin", "custom_user", "dept_child_user", "dept_user"),
                "dept_child_user", Set.of("admin", "custom_user", "dept_child_user", "dept_user", "self_user"),
                "self_user", Set.of("self_user")
        );

        expectedByUsername.forEach((username, expectedNames) -> {
            boolean reuseAdminSession = HttpApiTestSupport.ADMIN_USERNAME.equals(username);
            String token = reuseAdminSession
                    ? adminToken
                    : http.login(username, HttpApiTestSupport.DEFAULT_PASSWORD);
            try {
                HttpApiTestSupport.Response page = http.get(
                        "/system/user/list?pageNum=1&pageSize=100", token).expectPage();
                assertEquals(expectedNames, visibleUserNames(page), username + " 的数据范围不正确");
                assertEquals(expectedNames.size(),
                        ((Number) page.dataObject().get("total")).intValue(), username + " 的分页总数不正确");
            } finally {
                if (!reuseAdminSession) {
                    http.postJson("/auth/logout", Map.of(), token).expectSuccess();
                }
            }
        });
    }

    @Test
    @Order(8)
    void imageCaptchaIsConsumedAfterSuccessAndFailure() {
        CaptchaProperties properties = Solon.context().getBean(CaptchaProperties.class);
        CacheService cacheService = Solon.context().getBean(CacheService.class);
        boolean originalEnabled = Boolean.TRUE.equals(properties.getEnable());
        String successUuid = UUID.randomUUID().toString().replace("-", "");
        String failureUuid = UUID.randomUUID().toString().replace("-", "");
        String successKey = GlobalConstants.CAPTCHA_CODE_KEY + successUuid;
        String failureKey = GlobalConstants.CAPTCHA_CODE_KEY + failureUuid;
        try {
            properties.setEnable(true);
            cacheService.store(successKey, "JIMU", Constants.CAPTCHA_EXPIRATION * 60);
            HttpApiTestSupport.Response login = http.postEncryptedJson("/auth/login", Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "password",
                    "username", "dept_user",
                    "password", HttpApiTestSupport.DEFAULT_PASSWORD,
                    "uuid", successUuid,
                    "code", "jimu"
            )).expectSuccess();
            assertNull(cacheService.get(successKey, String.class), "登录成功后必须消费图片验证码");
            http.postJson("/auth/logout", Map.of(), login.dataString("access_token")).expectSuccess();

            cacheService.store(failureKey, "JIMU", Constants.CAPTCHA_EXPIRATION * 60);
            http.postEncryptedJson("/auth/login", Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "password",
                    "username", "dept_user",
                    "password", HttpApiTestSupport.DEFAULT_PASSWORD,
                    "uuid", failureUuid,
                    "code", "WRONG"
            )).expectFailure(200, 500, "验证码错误");
            assertNull(cacheService.get(failureKey, String.class), "验证码校验失败后也必须消费图片验证码");
            http.postEncryptedJson("/auth/login", Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "password",
                    "username", "dept_user",
                    "password", HttpApiTestSupport.DEFAULT_PASSWORD,
                    "uuid", failureUuid,
                    "code", "JIMU"
            )).expectFailure(200, 500, "验证码已失效");
        } finally {
            cacheService.remove(successKey);
            cacheService.remove(failureKey);
            properties.setEnable(originalEnabled);
        }
    }

    @Test
    @Order(9)
    void passwordRetryLockCanBeUnlockedAndSuccessfulLoginClearsTheCounter() {
        CacheService cacheService = Solon.context().getBean(CacheService.class);
        String username = "dept_user";
        String retryKey = GlobalConstants.PWD_ERR_CNT_KEY + username;
        cacheService.remove(retryKey);
        try {
            for (int attempt = 1; attempt < 5; attempt++) {
                passwordLogin(username, "incorrect-password")
                        .expectFailure(200, 500, "密码输入错误" + attempt + "次");
            }
            passwordLogin(username, "incorrect-password")
                    .expectFailure(200, 500, "密码输入错误5次，账户锁定10分钟");
            passwordLogin(username, HttpApiTestSupport.DEFAULT_PASSWORD)
                    .expectFailure(200, 500, "密码输入错误5次，账户锁定10分钟");

            http.get("/system/user/unlock/3", adminToken).expectSuccess();
            passwordLogin(username, "incorrect-password")
                    .expectFailure(200, 500, "密码输入错误1次");
            String token = passwordLogin(username, HttpApiTestSupport.DEFAULT_PASSWORD)
                    .expectSuccess().dataString("access_token");
            http.postJson("/auth/logout", Map.of(), token).expectSuccess();

            passwordLogin(username, "incorrect-password")
                    .expectFailure(200, 500, "密码输入错误1次");
        } finally {
            cacheService.remove(retryKey);
        }
    }

    @Test
    @Order(10)
    void clientStatusGrantHeaderPathIpAndTimeoutRulesAreEnforced() throws InterruptedException {
        String clientUsername = "dept_user";
        http.postJson("/system/client", securityClientPayload(null, null, "security-disabled",
                List.of("password"), List.of(), List.of(), -1, 60, "1"), adminToken).expectSuccess();
        securityClientCreated = true;
        Map<String, Object> client = findSecurityClient(adminToken);
        securityClientPk = longValue(client.get("id"));
        securityClientId = String.valueOf(client.get("clientId"));

        try {
            passwordLogin(securityClientId, clientUsername,
                    HttpApiTestSupport.DEFAULT_PASSWORD).expectCode(500);

            updateSecurityClient("security-grant", List.of("password"), List.of(), List.of(), -1, 60, "0");
            http.postEncryptedJson("/auth/login", Map.of(
                    "clientId", securityClientId,
                    "grantType", "sms",
                    "phoneNumber", "13800000002",
                    "smsCode", "1234"
            )).expectCode(500);

            updateSecurityClient("security-header", List.of("password"), List.of(), List.of(), -1, 60, "0");
            String headerToken = passwordLogin(securityClientId, clientUsername,
                    HttpApiTestSupport.DEFAULT_PASSWORD).expectSuccess().dataString("access_token");
            http.get("/system/user/getInfo", headerToken).expectStatus(401).expectCode(401);
            requestForSecurityClient("GET", "/system/user/getInfo", null, null, headerToken).expectSuccess();
            requestForSecurityClient("POST", "/auth/logout", "{}", "application/json", headerToken)
                    .expectSuccess();

            updateSecurityClient("security-path", List.of("password"),
                    List.of("/system/user/getInfo", "/auth/logout"), List.of(), -1, 60, "0");
            String pathToken = passwordLogin(securityClientId, clientUsername,
                    HttpApiTestSupport.DEFAULT_PASSWORD).expectSuccess().dataString("access_token");
            requestForSecurityClient("GET", "/system/user/getInfo", null, null, pathToken).expectSuccess();
            requestForSecurityClient("GET", "/system/user/profile", null, null, pathToken)
                    .expectStatus(403).expectCode(403);
            requestForSecurityClient("POST", "/auth/logout", "{}", "application/json", pathToken)
                    .expectSuccess();

            updateSecurityClient("security-ip", List.of("password"), List.of(),
                    List.of("203.0.113.10"), -1, 60, "0");
            String ipToken = passwordLogin(securityClientId, clientUsername,
                    HttpApiTestSupport.DEFAULT_PASSWORD).expectSuccess().dataString("access_token");
            requestForSecurityClient("GET", "/system/user/getInfo", null, null, ipToken)
                    .expectStatus(403).expectCode(403);

            updateSecurityClient("security-fixed-timeout", List.of("password"), List.of(), List.of(), -1, 1, "0");
            String fixedTimeoutToken = passwordLogin(securityClientId, clientUsername,
                    HttpApiTestSupport.DEFAULT_PASSWORD).expectSuccess().dataString("access_token");
            Thread.sleep(2_200L);
            requestForSecurityClient("GET", "/system/user/getInfo", null, null, fixedTimeoutToken)
                    .expectStatus(401).expectCode(401);

            updateSecurityClient("security-active-timeout", List.of("password"), List.of(), List.of(), 1, 60, "0");
            String activeTimeoutToken = passwordLogin(securityClientId, clientUsername,
                    HttpApiTestSupport.DEFAULT_PASSWORD).expectSuccess().dataString("access_token");
            requestForSecurityClient("GET", "/system/user/getInfo", null, null, activeTimeoutToken).expectSuccess();
            Thread.sleep(2_200L);
            requestForSecurityClient("GET", "/system/user/getInfo", null, null, activeTimeoutToken)
                    .expectStatus(401).expectCode(401);
        } finally {
            deleteSecurityClient(adminToken);
        }
    }

    @Test
    @Order(99)
    void logoutInvalidatesTheSession() {
        deleteTemporaryUser(registeredUserId, registeredUsername, adminToken);
        registeredUserCreated = false;
        registeredUserId = null;
        HttpApiTestSupport.Response logout = http.postJson("/auth/logout", Map.of(), adminToken);
        HttpApiTestSupport.Response afterLogout = http.get("/auth/codes", adminToken);

        logout.expectStatus(200).expectSuccess();
        afterLogout.expectStatus(401).expectCode(401);
    }

    private HttpApiTestSupport.Response passwordLogin(String username, String password) {
        return passwordLogin(HttpApiTestSupport.PC_CLIENT_ID, username, password);
    }

    private HttpApiTestSupport.Response passwordLogin(String clientId, String username, String password) {
        return http.postEncryptedJson("/auth/login", Map.of(
                "clientId", clientId,
                "grantType", "password",
                "username", username,
                "password", password
        ));
    }

    private HttpApiTestSupport.Response requestForSecurityClient(String method, String path, String body,
                                                                  String contentType, String token) {
        return http.requestWithHeaders(method, path, body, contentType, token,
                Map.of(LoginHelper.CLIENT_KEY, securityClientId));
    }

    private void updateSecurityClient(String deviceType, List<String> grantTypes, List<String> accessPaths,
                                      List<String> ipWhitelist, long activeTimeout, long timeout, String status) {
        http.putJson("/system/client", securityClientPayload(securityClientPk, securityClientId, deviceType,
                grantTypes, accessPaths, ipWhitelist, activeTimeout, timeout, status), adminToken).expectSuccess();
    }

    private Map<String, Object> securityClientPayload(Long id, String clientId, String deviceType,
                                                       List<String> grantTypes, List<String> accessPaths,
                                                       List<String> ipWhitelist, long activeTimeout,
                                                       long timeout, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("id", id);
        }
        if (clientId != null) {
            payload.put("clientId", clientId);
        }
        payload.put("clientKey", securityClientKey);
        payload.put("clientSecret", securityClientSecret);
        payload.put("grantTypeList", grantTypes);
        payload.put("deviceType", deviceType);
        payload.put("accessPathList", accessPaths);
        payload.put("ipWhitelistList", ipWhitelist);
        payload.put("activeTimeout", activeTimeout);
        payload.put("timeout", timeout);
        payload.put("status", status);
        return payload;
    }

    private Map<String, Object> findSecurityClient(String token) {
        HttpApiTestSupport.Response page = http.get("/system/client/list" + HttpApiTestSupport.query(Map.of(
                "clientKey", securityClientKey,
                "pageNum", 1,
                "pageSize", 10
        )), token).expectPage();
        for (Object item : rows(page)) {
            Map<String, Object> client = object(item);
            if (securityClientKey.equals(client.get("clientKey"))) {
                return client;
            }
        }
        throw new AssertionError("未查询到认证安全测试客户端: " + securityClientKey);
    }

    private void deleteSecurityClient(String token) {
        if (!securityClientCreated) {
            return;
        }
        Long clientPk = securityClientPk;
        if (clientPk == null) {
            clientPk = longValue(findSecurityClient(token).get("id"));
        }
        http.delete("/system/client/" + clientPk, token).expectSuccess();
        securityClientCreated = false;
        securityClientPk = null;
        securityClientId = null;
    }

    static boolean ownsRoute(RuntimeRouteCoverage.RouteKey key) {
        String path = key.path();
        return "/".equals(path)
                || path.startsWith("/auth/")
                || "/resource/email/code".equals(path)
                || "/resource/sms/code".equals(path)
                || "/system/user".equals(path)
                || path.startsWith("/system/user/")
                || "/system/social/list".equals(path);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> rows(HttpApiTestSupport.Response response) {
        return (List<Object>) response.dataObject().get("rows");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        assertInstanceOf(Map.class, value);
        return (Map<String, Object>) value;
    }

    private static Set<Long> numericIds(Object value) {
        assertTrue(value instanceof List<?>, "关联 ID 必须为数组: " + value);
        Set<Long> ids = new LinkedHashSet<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Number number) {
                ids.add(number.longValue());
            } else if (item instanceof String text && !text.isBlank()) {
                ids.add(Long.parseLong(text));
            } else {
                throw new AssertionError("关联 ID 必须为数值或数字字符串: " + item);
            }
        }
        return ids;
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new AssertionError("值必须为数值或数字字符串: " + value);
    }

    private static Long findUserId(HttpApiTestSupport.Response response, String username) {
        Long userId = findUserIdOrNull(response, username);
        if (userId != null) {
            return userId;
        }
        throw new AssertionError("未查询到刚创建的用户: " + username);
    }

    private static Set<String> visibleUserNames(HttpApiTestSupport.Response response) {
        Set<String> names = new LinkedHashSet<>();
        for (Object value : rows(response)) {
            names.add(String.valueOf(object(value).get("userName")));
        }
        return names;
    }

    private HttpApiTestSupport.Response uploadUserWorkbook(byte[] template, String username, String nickname,
                                                           boolean updateSupport) {
        ByteArrayOutputStream workbook = new ByteArrayOutputStream();
        try (XSSFWorkbook source = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            Sheet sheet = source.getSheet("用户数据");
            org.apache.poi.ss.usermodel.Row header = sheet.getRow(0);
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(1);
            setCellByHeader(header, row, "部门名称", "积木区科技/总部/研发部/平台组");
            setCellByHeader(header, row, "用户账号", username);
            setCellByHeader(header, row, "用户昵称", nickname);
            setCellByHeader(header, row, "用户邮箱", username + "@jimuqu.local");
            setCellByHeader(header, row, "手机号码", "137" + suffix.replaceAll("[^0-9]", "")
                    .concat("00000000").substring(0, 8));
            source.write(workbook);
        } catch (Exception exception) {
            throw new AssertionError("构造用户导入工作簿失败", exception);
        }
        String boundary = "JimuquExcelBoundary" + suffix;
        byte[] body = multipartWorkbook(boundary, workbook.toByteArray(), updateSupport);
        return http.requestBytes("POST", "/system/user/importData", body,
                "multipart/form-data; boundary=" + boundary, adminToken);
    }

    private static void setCellByHeader(org.apache.poi.ss.usermodel.Row header,
                                        org.apache.poi.ss.usermodel.Row row,
                                        String name, String value) {
        for (org.apache.poi.ss.usermodel.Cell cell : header) {
            if (name.equals(cell.getStringCellValue())) {
                row.createCell(cell.getColumnIndex()).setCellValue(value);
                return;
            }
        }
        throw new AssertionError("用户导入模板缺少列: " + name);
    }

    private static byte[] multipartWorkbook(String boundary, byte[] workbook, boolean updateSupport) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"updateSupport\"\r\n\r\n"
                + updateSupport + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"users.xlsx\"\r\n"
                + "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.writeBytes(workbook);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }

    private static Long findUserIdOrNull(HttpApiTestSupport.Response response, String username) {
        for (Object value : rows(response)) {
            Map<String, Object> user = object(value);
            if (username.equals(user.get("userName"))) {
                Object id = user.get("userId");
                if (id instanceof Number number) {
                    return number.longValue();
                }
                if (id instanceof String text && !text.isBlank()) {
                    return Long.parseLong(text);
                }
                throw new AssertionError("userId 必须为数值或数字字符串: " + id);
            }
        }
        return null;
    }

    private void cleanUpTemporaryUsers() {
        if (!registeredUserCreated && !createdUserCreated && !importedUserCreated && !securityClientCreated) {
            return;
        }
        String cleanupToken = http.loginAdmin();
        try {
            deleteTemporaryUser(registeredUserId, registeredUsername, cleanupToken);
            deleteTemporaryUser(createdUserId, createdUsername, cleanupToken);
            deleteTemporaryUser(importedUserId, importedUsername, cleanupToken);
            deleteSecurityClient(cleanupToken);
            registeredUserCreated = false;
            createdUserCreated = false;
            importedUserCreated = false;
            registeredUserId = null;
            createdUserId = null;
            importedUserId = null;
        } finally {
            http.postJson("/auth/logout", Map.of(), cleanupToken).expectSuccess();
        }
    }

    private void deleteTemporaryUser(Long userId, String username, String token) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            HttpApiTestSupport.Response page = http.get(
                    "/system/user/list" + HttpApiTestSupport.query(Map.of(
                            "pageNum", 1,
                            "pageSize", 10,
                            "userName", username
                    )), token).expectPage();
            targetUserId = findUserIdOrNull(page, username);
        }
        if (targetUserId != null) {
            http.delete("/system/user/" + targetUserId, token).expectSuccess();
        }
    }
}
