package com.jimuqu.test.http;

import com.jimuqu.Application;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.system.domain.vo.SysUserImportVo;
import com.jimuqu.test.coverage.RuntimeRouteCoverage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.noear.solon.test.SolonTest;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Health、认证、用户、个人资料和社会化账号的真实 HTTP 契约测试。
 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HealthAuthUserHttpContractTest {

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
        HttpApiTestSupport.Response captcha = http.get("/auth/code");
        HttpApiTestSupport.Response invalidEmail = http.get("/resource/email/code?email=invalid");
        HttpApiTestSupport.Response invalidPhone = http.get("/resource/sms/code?phoneNumber=invalid");
        HttpApiTestSupport.Response unsupportedBinding = http.get("/auth/binding/not-configured");
        HttpApiTestSupport.Response invalidClient = http.postJson("/auth/login", Map.of(
                "clientId", "invalid-client",
                "grantType", "password",
                "username", HttpApiTestSupport.ADMIN_USERNAME,
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        ));
        HttpApiTestSupport.Response disabledUser = http.postJson("/auth/login", Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", "disabled_user",
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        ));
        HttpApiTestSupport.Response unauthenticatedCodes = http.get("/auth/codes");
        HttpApiTestSupport.Response unauthenticatedCallback = http.postJson("/auth/social/callback", Map.of(
                "source", "not-configured",
                "socialCode", "invalid",
                "socialState", "invalid"
        ));
        HttpApiTestSupport.Response unauthenticatedUnlock = http.delete("/auth/unlock/999999");
        HttpApiTestSupport.Response disabledRegister = http.postJson("/auth/register", Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", registeredUsername,
                "password", "Register123!",
                "userType", "pc_user"
        ));

        captcha.expectStatus(200).expectSuccess();
        assertInstanceOf(Boolean.class, captcha.dataObject().get("captchaEnabled"));
        invalidEmail.expectStatus(200).expectCode(500);
        invalidPhone.expectStatus(200).expectCode(500);
        unsupportedBinding.expectStatus(200).expectCode(500);
        invalidClient.expectStatus(200).expectCode(500);
        disabledUser.expectStatus(200).expectCode(500);
        unauthenticatedCodes.expectStatus(401).expectCode(401);
        unauthenticatedCallback.expectStatus(401).expectCode(401);
        unauthenticatedUnlock.expectStatus(401).expectCode(401);
        disabledRegister.expectStatus(200).expectCode(500);
        assertEquals("当前系统没有开启注册功能！", disabledRegister.json().get("msg"));
    }

    @Test
    @Order(2)
    void loginRegisterAndAuthenticatedAuthContracts() {
        HttpApiTestSupport.Response login = http.postJson("/auth/login", Map.of(
                "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                "grantType", "password",
                "username", HttpApiTestSupport.ADMIN_USERNAME,
                "password", HttpApiTestSupport.DEFAULT_PASSWORD
        ));
        login.expectStatus(200).expectSuccess();
        Object expireIn = login.dataObject().get("expire_in");
        assertInstanceOf(Number.class, expireIn, "expire_in 必须是 JSON Number");
        assertTrue(((Number) expireIn).intValue() != 0, "expire_in 必须表示有效的令牌 TTL");
        adminToken = login.dataString("access_token");

        try (HttpApiTestSupport.SseSubscription stream = http.openSse("/resource/message", adminToken)) {
            stream.expectEvent("connected", "");
            stream.expectBellMessage("message", "backend", "欢迎登录积木区后台管理系统");
        }

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
            http.postJson("/auth/register", Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "password",
                    "username", "x",
                    "password", "",
                    "userType", "pc_user"
            )).expectCode(500);
            register = http.postJson("/auth/register", Map.of(
                    "clientId", HttpApiTestSupport.PC_CLIENT_ID,
                    "grantType", "password",
                    "username", registeredUsername,
                    "password", "Register123!",
                    "userType", "pc_user"
            ));
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
        HttpApiTestSupport.Response deptTree = http.get("/system/user/deptTree", adminToken);
        HttpApiTestSupport.Response profile = http.get("/system/user/profile", adminToken);
        HttpApiTestSupport.Response social = http.get("/system/social/list", adminToken);

        list.expectStatus(200).expectPage();
        assertFalse(rows(list).isEmpty(), "种子用户列表不能为空");
        byDept.expectStatus(200).expectSuccess();
        assertFalse(byDept.dataList().isEmpty(), "研发部门种子用户不能为空");
        detail.expectStatus(200).expectSuccess();
        assertEquals("custom_user", object(detail.dataObject().get("user")).get("userName"));
        health.expectStatus(200).expectSuccess();
        assertTrue(health.dataObject().containsKey("version"));
        createInfo.expectStatus(200).expectSuccess();
        assertTrue(createInfo.dataObject().containsKey("roles"));
        currentInfo.expectStatus(200).expectSuccess();
        assertEquals("admin", object(currentInfo.dataObject().get("user")).get("userName"));
        assertEquals(List.of("superadmin"), currentInfo.dataObject().get("roles"));
        authRole.expectStatus(200).expectSuccess();
        assertEquals("custom_user", object(authRole.dataObject().get("user")).get("userName"));
        deptTree.expectStatus(200).expectSuccess();
        assertFalse(deptTree.dataList().isEmpty(), "部门树不能为空");
        profile.expectStatus(200).expectSuccess();
        assertEquals("admin", object(profile.dataObject().get("user")).get("userName"));
        social.expectStatus(200).expectSuccess();
        assertTrue(social.dataList().isEmpty(), "新测试库不应预置社会化账号");
    }

    @Test
    @Order(7)
    void unauthorizedAndInvalidUserWritesAreRejected() {
        restrictedToken = http.login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);

        HttpApiTestSupport.Response unauthenticatedProfile = http.get("/system/user/profile");
        HttpApiTestSupport.Response forbiddenList = http.get("/system/user/list?pageNum=1&pageSize=10", restrictedToken);
        HttpApiTestSupport.Response invalidAdd = http.postJson("/system/user", Map.of(), adminToken);
        HttpApiTestSupport.Response invalidSelfDelete = http.delete("/system/user/1", adminToken);

        unauthenticatedProfile.expectStatus(401).expectCode(401);
        forbiddenList.expectStatus(403).expectCode(403);
        invalidAdd.expectStatus(200).expectCode(400);
        invalidSelfDelete.expectStatus(200).expectCode(500);
    }

    @Test
    @Order(6)
    void userWriteProfileAndExportContracts() {
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
        HttpApiTestSupport.Response resetPassword = http.putJson("/system/user/resetPwd", Map.of(
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
        HttpApiTestSupport.Response updateProfile = http.putJson("/system/user/profile", Map.of(
                "nickName", "系统管理员",
                "email", "admin@jimuqu.local",
                "phoneNumber", "13800000001",
                "sex", "0"
        ), adminToken);
        HttpApiTestSupport.Response updatePassword = http.putJson("/system/user/profile/updatePwd", Map.of(
                "oldPassword", HttpApiTestSupport.DEFAULT_PASSWORD,
                "newPassword", "Temporary123!"
        ), adminToken);
        HttpApiTestSupport.Response restorePassword = http.putJson("/system/user/profile/updatePwd", Map.of(
                "oldPassword", "Temporary123!",
                "newPassword", HttpApiTestSupport.DEFAULT_PASSWORD
        ), adminToken);
        HttpApiTestSupport.Response export = http.postForm("/system/user/export", Map.of(), adminToken);
        HttpApiTestSupport.Response delete = http.delete("/system/user/" + createdUserId, adminToken);

        edit.expectStatus(200).expectSuccess();
        resetPassword.expectStatus(200).expectSuccess();
        changeStatus.expectStatus(200).expectSuccess();
        grantRole.expectStatus(200).expectSuccess();
        updateProfile.expectStatus(200).expectSuccess();
        updatePassword.expectStatus(200).expectSuccess();
        restorePassword.expectStatus(200).expectSuccess();
        export.expectSpreadsheet();
        delete.expectStatus(200).expectSuccess();
        createdUserCreated = false;
        createdUserId = null;
    }

    @Test
    @Order(5)
    void userImportTemplateAndUploadContracts() {
        http.request("POST", "/system/user/importTemplate", null, null, adminToken)
                .expectSpreadsheet();

        HttpApiTestSupport.Response imported = uploadUserWorkbook(importedUsername, "Excel导入用户", false);
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

        uploadUserWorkbook(importedUsername, "Excel覆盖用户", true).expectSuccess();
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

    @Test
    @Order(4)
    void userListAppliesAllFiveDataScopes() {
        deleteTemporaryUser(registeredUserId, registeredUsername, adminToken);
        registeredUserCreated = false;
        registeredUserId = null;

        Map<String, Set<String>> expectedByUsername = Map.of(
                "admin", Set.of("admin", "custom_user", "dept_child_user", "dept_user",
                        "disabled_user", "no_permission", "self_user"),
                "custom_user", Set.of("admin", "custom_user", "dept_child_user", "dept_user"),
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
    void logoutInvalidatesTheSession() {
        deleteTemporaryUser(registeredUserId, registeredUsername, adminToken);
        registeredUserCreated = false;
        registeredUserId = null;
        HttpApiTestSupport.Response logout = http.postJson("/auth/logout", Map.of(), adminToken);
        HttpApiTestSupport.Response afterLogout = http.get("/auth/codes", adminToken);

        logout.expectStatus(200).expectSuccess();
        afterLogout.expectStatus(401).expectCode(401);
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

    private HttpApiTestSupport.Response uploadUserWorkbook(String username, String nickname,
                                                           boolean updateSupport) {
        SysUserImportVo imported = new SysUserImportVo();
        imported.setDeptId(103L);
        imported.setUserName(username);
        imported.setNickName(nickname);
        imported.setEmail(username + "@jimuqu.local");
        imported.setPhonenumber("137" + suffix.replaceAll("[^0-9]", "")
                .concat("00000000").substring(0, 8));

        ByteArrayOutputStream workbook = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(List.of(imported), "用户数据", SysUserImportVo.class, workbook);
        String boundary = "JimuquExcelBoundary" + suffix;
        byte[] body = multipartWorkbook(boundary, workbook.toByteArray(), updateSupport);
        return http.requestBytes("POST", "/system/user/importData", body,
                "multipart/form-data; boundary=" + boundary, adminToken);
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
        if (!registeredUserCreated && !createdUserCreated && !importedUserCreated) {
            return;
        }
        String cleanupToken = http.loginAdmin();
        try {
            deleteTemporaryUser(registeredUserId, registeredUsername, cleanupToken);
            deleteTemporaryUser(createdUserId, createdUsername, cleanupToken);
            deleteTemporaryUser(importedUserId, importedUsername, cleanupToken);
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
