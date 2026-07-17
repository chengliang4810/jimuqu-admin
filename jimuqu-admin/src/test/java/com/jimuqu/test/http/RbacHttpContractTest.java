package com.jimuqu.test.http;

import com.jimuqu.Application;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.noear.solon.test.SolonTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 角色、菜单、部门和岗位的真实 HTTP 契约。 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RbacHttpContractTest {

    private HttpApiTestSupport api;
    private String adminToken;
    private String deniedToken;
    private String suffix;

    @BeforeAll
    void setUp() {
        api = new HttpApiTestSupport(RbacHttpContractTest::ownsRoute);
        adminToken = api.loginAdmin();
        deniedToken = api.login("no_permission", HttpApiTestSupport.DEFAULT_PASSWORD);
        suffix = Long.toUnsignedString(System.nanoTime(), 36);
    }

    static boolean ownsRoute(com.jimuqu.test.coverage.RuntimeRouteCoverage.RouteKey key) {
        return key.path().startsWith("/system/role")
                || key.path().startsWith("/system/menu")
                || key.path().startsWith("/system/dept")
                || key.path().startsWith("/system/post");
    }

    @AfterAll
    void assertRouteCoverage() {
        api.assertCoverageComplete();
    }

    @Test
    @Order(1)
    void rejectsUnauthenticatedAndUnprivilegedAccess() {
        api.get("/system/role/list?pageNum=1&pageSize=10")
                .expectStatus(401)
                .expectCode(401);
        api.get("/system/role/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.get("/system/menu/list", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.postJson("/system/dept", deptPayload("denied-" + suffix), deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.delete("/system/post/1", deniedToken)
                .expectStatus(403)
                .expectCode(403);
    }

    @Test
    @Order(2)
    void exercisesAllRbacRoutesAndProtectsCriticalWrites() {
        String deptName = "HTTP部门-" + suffix;
        String roleName = "HTTP角色-" + suffix;
        String roleKey = "http_role_" + suffix;
        String menuName = "HTTP菜单-" + suffix;
        String cascadeMenuName = "HTTP级联菜单-" + suffix;
        String postName = "HTTP岗位-" + suffix;
        String postCode = "http_post_" + suffix;

        api.postJson("/system/dept", deptPayload(deptName), adminToken).expectSuccess();
        long deptId = rowId(api.get("/system/dept/list" + HttpApiTestSupport.query(Map.of("deptName", deptName)), adminToken)
                .expectSuccess().dataList(), "deptName", deptName, "deptId", "id");

        api.postJson("/system/menu", menuPayload(menuName, "http-menu-" + suffix), adminToken).expectSuccess();
        long menuId = rowId(api.get("/system/menu/list" + HttpApiTestSupport.query(Map.of("menuName", menuName)), adminToken)
                .expectSuccess().dataList(), "menuName", menuName, "menuId", "id");

        api.postJson("/system/menu", menuPayload(cascadeMenuName, "http-cascade-" + suffix), adminToken)
                .expectSuccess();
        long cascadeMenuId = rowId(api.get("/system/menu/list"
                        + HttpApiTestSupport.query(Map.of("menuName", cascadeMenuName)), adminToken)
                .expectSuccess().dataList(), "menuName", cascadeMenuName, "menuId", "id");

        api.postJson("/system/role", rolePayload(roleName, roleKey, List.of(menuId)), adminToken).expectSuccess();
        long roleId = pageRowId(api.get("/system/role/list" + HttpApiTestSupport.query(Map.of(
                        "roleName", roleName, "pageNum", 1, "pageSize", 20)), adminToken).expectPage(),
                "roleName", roleName, "roleId", "id");

        api.postJson("/system/post", postPayload(null, deptId, postCode, postName, "0"), adminToken)
                .expectSuccess();
        long postId = pageRowId(api.get("/system/post/list" + HttpApiTestSupport.query(Map.of(
                        "postCode", postCode, "pageNum", 1, "pageSize", 20)), adminToken).expectPage(),
                "postCode", postCode, "postId");

        api.postForm("/system/role/export", Map.of("roleName", roleName), adminToken)
                .expectSpreadsheet();
        api.postForm("/system/post/export", Map.of("postCode", postCode), adminToken)
                .expectSpreadsheet();

        exerciseRoleRoutes(roleId, roleName, roleKey, deptId, menuId);
        exerciseMenuRoutes(menuId, menuName, roleId);
        exerciseDeptRoutes(deptId, deptName);
        exercisePostRoutes(postId, deptId, postCode, postName);

        api.delete("/system/post/" + postId, adminToken).expectSuccess();
        api.delete("/system/role/" + roleId, adminToken).expectSuccess();
        api.delete("/system/menu/" + menuId, adminToken).expectSuccess();
        api.delete("/system/menu/cascade/" + cascadeMenuId, adminToken).expectSuccess();
        api.delete("/system/dept/" + deptId, adminToken).expectSuccess();
    }

    private void exerciseRoleRoutes(long roleId, String roleName, String roleKey, long deptId, long menuId) {
        api.get("/system/role/" + roleId, adminToken).expectSuccess();
        api.get("/system/role/optionselect?roleIds=" + roleId, adminToken).expectSuccess();
        api.get("/system/role/deptTree/" + roleId, adminToken).expectSuccess();

        api.putJson("/system/role/permission", Map.of(
                "roleId", roleId,
                "menuIds", List.of(menuId),
                "menuCheckStrictly", true
        ), adminToken).expectSuccess();
        api.putJson("/system/role/dataScope", Map.of(
                "roleId", roleId,
                "dataScope", "2",
                "deptIds", List.of(deptId),
                "deptCheckStrictly", true
        ), adminToken).expectSuccess();
        api.putJson("/system/role/changeStatus", Map.of("roleId", roleId, "status", "0"), adminToken)
                .expectSuccess();

        api.request("PUT", "/system/role/authUser/selectAll?roleId=" + roleId + "&userIds=5",
                null, null, adminToken).expectSuccess();
        api.get("/system/role/authUser/allocatedList?roleId=" + roleId + "&pageNum=1&pageSize=20", adminToken)
                .expectPage();
        api.get("/system/role/authUser/unallocatedList?roleId=" + roleId + "&pageNum=1&pageSize=20", adminToken)
                .expectPage();
        api.putJson("/system/role/authUser/cancel", Map.of("roleId", roleId, "userId", 5), adminToken)
                .expectSuccess();
        api.request("PUT", "/system/role/authUser/selectAll?roleId=" + roleId + "&userIds=5",
                null, null, adminToken).expectSuccess();
        api.request("PUT", "/system/role/authUser/cancelAll?roleId=" + roleId + "&userIds=5",
                null, null, adminToken).expectSuccess();

        Map<String, Object> edit = new LinkedHashMap<>(rolePayload(roleName + "-改", roleKey, List.of(menuId)));
        edit.put("roleId", roleId);
        api.putJson("/system/role", edit, adminToken).expectSuccess();

        HttpApiTestSupport.Response protectedRole = api.putJson("/system/role/changeStatus",
                Map.of("roleId", 1, "status", "1"), adminToken).expectEnvelope();
        assertNotEquals(200, protectedRole.code(), "超级管理员角色不得被停用");

        HttpApiTestSupport.Response currentUserRole = api.putJson("/system/role/authUser/cancel",
                Map.of("roleId", 1, "userId", 1), adminToken).expectEnvelope();
        assertNotEquals(200, currentUserRole.code(), "不得取消当前用户自己的角色");
        assertTrue(String.valueOf(currentUserRole.json().get("msg")).contains("当前用户"));
        assertTrue(api.get("/system/role/authUser/allocatedList?roleId=1&pageNum=1&pageSize=20", adminToken)
                .expectPage().dataObject().get("rows").toString().contains("admin"), "拒绝后角色关联必须保持不变");
    }

    private void exerciseMenuRoutes(long menuId, String menuName, long roleId) {
        HttpApiTestSupport.Response routers = api.get("/system/menu/getRouters", adminToken).expectSuccess();
        assertTrue(routers.json().toString().contains("Http-menu-" + suffix + menuId),
                "动态路由 name 必须包含菜单 ID");
        HttpApiTestSupport.Response detail = api.get("/system/menu/" + menuId, adminToken).expectSuccess();
        assertEquals("/system/menu", detail.dataObject().get("activeMenu"));
        assertEquals("{\"badge\":\"test\"}", detail.dataObject().get("ext"));
        api.get("/system/menu/treeselect", adminToken).expectSuccess();
        api.get("/system/menu/roleMenuTreeselect/" + roleId, adminToken).expectSuccess();

        Map<String, Object> edit = menuPayload(menuName + "-改", "http-menu-" + suffix);
        edit.put("menuId", menuId);
        api.putJson("/system/menu", edit, adminToken).expectSuccess();

        Map<String, Object> invalidExternal = menuPayload("非法外链-" + suffix, "relative-path");
        invalidExternal.put("isFrame", "0");
        HttpApiTestSupport.Response response = api.postJson("/system/menu", invalidExternal, adminToken)
                .expectEnvelope();
        assertNotEquals(200, response.code(), "外链菜单必须使用 http(s) 地址");
        assertTrue(String.valueOf(response.json().get("msg")).contains("http"));
    }

    private void exerciseDeptRoutes(long deptId, String deptName) {
        api.get("/system/dept/tree", adminToken).expectSuccess();
        api.get("/system/dept/list/exclude/" + deptId, adminToken).expectSuccess();
        api.get("/system/dept/" + deptId, adminToken).expectSuccess();

        Map<String, Object> edit = deptPayload(deptName + "-改");
        edit.put("deptId", deptId);
        api.putJson("/system/dept", edit, adminToken).expectSuccess();

        HttpApiTestSupport.Response protectedDept = api.delete("/system/dept/100", adminToken)
                .expectEnvelope();
        assertNotEquals(200, protectedDept.code(), "默认部门不得删除");
        assertTrue(String.valueOf(protectedDept.json().get("msg")).contains("不允许删除"));
    }

    private void exercisePostRoutes(long postId, long deptId, String postCode, String postName) {
        api.get("/system/post/" + postId, adminToken).expectSuccess();
        api.get("/system/post/optionselect?postIds=" + postId, adminToken).expectSuccess();
        api.get("/system/post/deptTree", adminToken).expectSuccess();
        api.putJson("/system/post", postPayload(postId, deptId, postCode, postName + "-改", "0"), adminToken)
                .expectSuccess();

        HttpApiTestSupport.Response assignedPost = api.putJson("/system/post",
                postPayload(1L, 103L, "admin", "管理员", "1"), adminToken).expectEnvelope();
        assertNotEquals(200, assignedPost.code(), "已分配用户的岗位不得停用");
        assertTrue(String.valueOf(assignedPost.json().get("msg")).contains("不能禁用"));
    }

    private Map<String, Object> rolePayload(String name, String key, List<Long> menuIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roleName", name);
        payload.put("roleKey", key);
        payload.put("roleSort", 90);
        payload.put("dataScope", "1");
        payload.put("menuCheckStrictly", true);
        payload.put("deptCheckStrictly", true);
        payload.put("status", "0");
        payload.put("menuIds", menuIds);
        return payload;
    }

    private Map<String, Object> menuPayload(String name, String path) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("parentId", 1);
        payload.put("menuName", name);
        payload.put("orderNum", 90);
        payload.put("path", path);
        payload.put("component", "system/http/index");
        payload.put("isFrame", "1");
        payload.put("isCache", "0");
        payload.put("menuType", "C");
        payload.put("visible", "0");
        payload.put("status", "0");
        payload.put("perms", "test:rbac:list");
        payload.put("icon", "test");
        payload.put("activeMenu", "/system/menu");
        payload.put("ext", "{\"badge\":\"test\"}");
        return payload;
    }

    private Map<String, Object> deptPayload(String name) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("parentId", 100);
        payload.put("deptName", name);
        payload.put("orderNum", 90);
        payload.put("status", "0");
        return payload;
    }

    private Map<String, Object> postPayload(Long id, long deptId, String code, String name, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("postId", id);
        }
        payload.put("deptId", deptId);
        payload.put("postCode", code);
        payload.put("postName", name);
        payload.put("postSort", 90);
        payload.put("status", status);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private long pageRowId(HttpApiTestSupport.Response response, String matchField, Object expected,
                           String... idFields) {
        Object rows = response.dataObject().get("rows");
        assertTrue(rows instanceof List<?>, "分页 rows 必须为数组");
        return rowId((List<Object>) rows, matchField, expected, idFields);
    }

    @SuppressWarnings("unchecked")
    private long rowId(List<Object> rows, String matchField, Object expected, String... idFields) {
        Map<String, Object> row = rows.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(item -> expected.equals(item.get(matchField)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 " + matchField + "=" + expected + " 的响应行"));
        for (String field : idFields) {
            Object value = row.get(field);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Long.parseLong(text);
            }
        }
        throw new AssertionError("响应行缺少数值主键 " + List.of(idFields) + ": " + row);
    }
}
