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

    private static final long MISSING_ID = 9_223_372_036_854_775_000L;

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
        String deniedDeptName = "denied-" + suffix;
        api.get("/system/role/list?pageNum=1&pageSize=10")
                .expectStatus(401)
                .expectCode(401);
        api.get("/system/role/list?pageNum=1&pageSize=10", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.get("/system/menu/list", deniedToken)
                .expectStatus(403)
                .expectCode(403);
        api.postJson("/system/dept", deptPayload(deniedDeptName), deniedToken)
                .expectStatus(403)
                .expectCode(403);
        assertTrue(api.get("/system/dept/list" + HttpApiTestSupport.query(Map.of("deptName", deniedDeptName)),
                adminToken).expectSuccess().dataList().isEmpty(), "无权限部门写入不得改变数据库");
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
        List<Object> deptRows = api.get("/system/dept/list" + HttpApiTestSupport.query(Map.of("deptName", deptName)),
                adminToken).expectSuccess().dataList();
        Map<String, Object> deptRow = row(deptRows, "deptName", deptName);
        long deptId = rowId(deptRows, "deptName", deptName, "deptId", "id");
        assertEquals("dept-category-" + suffix, deptRow.get("deptCategory"));
        assertResponseFields(deptRow, "createTime", "children");

        api.postJson("/system/menu", menuPayload(menuName, "http-menu-" + suffix), adminToken).expectSuccess();
        List<Object> menuRows = api.get("/system/menu/list" + HttpApiTestSupport.query(Map.of("menuName", menuName)),
                adminToken).expectSuccess().dataList();
        Map<String, Object> menuRow = row(menuRows, "menuName", menuName);
        long menuId = rowId(menuRows, "menuName", menuName, "menuId", "id");
        assertEquals("{\"source\":\"http\"}", menuRow.get("queryParam"));
        assertEquals("N", menuRow.get("isFrame"));
        assertEquals("Y", menuRow.get("isCache"));
        assertEquals("103", String.valueOf(menuRow.get("createDept")));
        assertResponseFields(menuRow, "createTime", "children");

        api.postJson("/system/menu", menuPayload(cascadeMenuName, "http-cascade-" + suffix), adminToken)
                .expectSuccess();
        long cascadeMenuId = rowId(api.get("/system/menu/list"
                        + HttpApiTestSupport.query(Map.of("menuName", cascadeMenuName)), adminToken)
                .expectSuccess().dataList(), "menuName", cascadeMenuName, "menuId", "id");
        Map<String, Object> disabledMenu = menuPayload(cascadeMenuName, "http-cascade-" + suffix);
        disabledMenu.put("menuId", cascadeMenuId);
        disabledMenu.put("status", "1");
        api.putJson("/system/menu", disabledMenu, adminToken).expectSuccess();
        assertTrue(treeContainsId(api.get("/system/menu/treeselect", adminToken)
                        .expectSuccess().dataList(), cascadeMenuId),
                "禁用菜单仍须出现在菜单选择树中");

        api.postJson("/system/role", rolePayload(roleName, roleKey, List.of(menuId)), adminToken).expectSuccess();
        long roleId = pageRowId(api.get("/system/role/list" + HttpApiTestSupport.query(Map.of(
                        "roleName", roleName, "pageNum", 1, "pageSize", 20)), adminToken).expectPage(),
                "roleName", roleName, "roleId", "id");
        HttpApiTestSupport.Response futureRolePage = api.get("/system/role/list"
                + HttpApiTestSupport.query(Map.of(
                "roleName", roleName,
                "pageNum", 1,
                "pageSize", 20,
                "params[beginTime]", "2999-01-01 00:00:00",
                "params[endTime]", "2999-12-31 23:59:59")), adminToken).expectPage();
        assertTrue(pageRows(futureRolePage).isEmpty(), "Bell 角色创建时间范围必须实际参与查询");

        api.postJson("/system/post", postPayload(null, deptId, postCode, postName, "0"), adminToken)
                .expectSuccess();
        HttpApiTestSupport.Response postPage = api.get("/system/post/list" + HttpApiTestSupport.query(Map.of(
                "postCode", postCode, "pageNum", 1, "pageSize", 20)), adminToken).expectPage();
        long postId = pageRowId(postPage, "postCode", postCode, "postId");
        Map<String, Object> postRow = row(pageRows(postPage), "postCode", postCode);
        assertEquals(deptName, postRow.get("deptName"));
        assertEquals("post-category-" + suffix, postRow.get("postCategory"));
        assertResponseFields(postRow, "createTime");

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
                "menuCheckStrictly", true,
                "dataScope", "1",
                "deptIds", List.of()
        ), adminToken).expectSuccess();
        Object checkedMenus = api.get("/system/menu/roleMenuTreeselect/" + roleId, adminToken)
                .expectSuccess().dataObject().get("checkedKeys");
        assertNotEquals(200, api.putJson("/system/role/permission", Map.of(
                "roleId", roleId,
                "menuIds", List.of(MISSING_ID),
                "menuCheckStrictly", true,
                "dataScope", "1",
                "deptIds", List.of()
        ), adminToken).expectEnvelope().code(), "不存在的菜单不得写入角色权限");
        assertEquals(checkedMenus, api.get("/system/menu/roleMenuTreeselect/" + roleId, adminToken)
                .expectSuccess().dataObject().get("checkedKeys"), "拒绝后原菜单权限必须保持不变");

        api.putJson("/system/role/dataScope", Map.of(
                "roleId", roleId,
                "dataScope", "2",
                "deptIds", List.of(deptId),
                "deptCheckStrictly", false
        ), adminToken).expectSuccess();
        Object checkedDepts = api.get("/system/role/deptTree/" + roleId, adminToken)
                .expectSuccess().dataObject().get("checkedKeys");
        assertNotEquals(200, api.putJson("/system/role/dataScope", Map.of(
                "roleId", roleId,
                "dataScope", "2",
                "deptIds", List.of(MISSING_ID),
                "deptCheckStrictly", true
        ), adminToken).expectEnvelope().code(), "不存在的部门不得写入角色数据范围");
        assertEquals(checkedDepts, api.get("/system/role/deptTree/" + roleId, adminToken)
                .expectSuccess().dataObject().get("checkedKeys"), "拒绝后原部门权限必须保持不变");
        assertNotEquals(200, api.putJson("/system/role/dataScope", Map.of(
                "roleId", roleId, "dataScope", "9", "deptIds", List.of(deptId)), adminToken)
                .expectEnvelope().code(), "未知数据范围不得写入");
        assertEquals("2", api.get("/system/role/" + roleId, adminToken)
                .expectSuccess().dataObject().get("dataScope"), "拒绝后原数据范围必须保持不变");
        api.putJson("/system/role/dataScope", Map.of(
                "roleId", roleId,
                "dataScope", "6",
                "deptIds", List.of(),
                "deptCheckStrictly", true
        ), adminToken).expectSuccess();
        assertEquals("6", api.get("/system/role/" + roleId, adminToken)
                .expectSuccess().dataObject().get("dataScope"), "组合数据范围必须可配置");
        api.putJson("/system/role/dataScope", Map.of(
                "roleId", roleId,
                "dataScope", "2",
                "deptIds", List.of(deptId),
                "deptCheckStrictly", true
        ), adminToken).expectSuccess();
        api.putJson("/system/role/changeStatus", Map.of("roleId", roleId, "status", "1"), adminToken)
                .expectSuccess();
        assertTrue(checkedKeys(api.get("/system/menu/roleMenuTreeselect/" + roleId, adminToken)
                .expectSuccess()).isEmpty(), "停用角色不得回显菜单绑定");
        assertTrue(checkedKeys(api.get("/system/role/deptTree/" + roleId, adminToken)
                .expectSuccess()).isEmpty(), "停用角色不得回显部门绑定");
        api.putJson("/system/role/changeStatus", Map.of("roleId", roleId, "status", "0"), adminToken)
                .expectSuccess();

        api.request("PUT", "/system/role/authUser/selectAll?roleId=" + roleId + "&userIds=5",
                null, null, adminToken).expectSuccess();
        Map<String, Object> menuPermission = Map.of(
                "roleId", roleId,
                "menuIds", List.of(102L),
                "menuCheckStrictly", true,
                "dataScope", "2",
                "deptIds", List.of(deptId),
                "deptCheckStrictly", true
        );
        api.putJson("/system/role/permission", menuPermission, adminToken).expectSuccess();
        String menuOperatorToken = api.login("self_user", HttpApiTestSupport.DEFAULT_PASSWORD);
        try {
            assertTrue(api.get("/auth/codes", menuOperatorToken).expectSuccess().dataList()
                            .contains("system:menu:list"),
                    "测试用户必须先真实获得菜单列表权限");
            api.get("/system/menu/list", menuOperatorToken)
                    .expectStatus(403)
                    .expectCode(403);
        } finally {
            api.postJson("/auth/logout", Map.of(), menuOperatorToken).expectSuccess();
        }
        api.putJson("/system/role/permission", Map.of(
                "roleId", roleId,
                "menuIds", List.of(menuId),
                "menuCheckStrictly", true,
                "dataScope", "2",
                "deptIds", List.of(deptId),
                "deptCheckStrictly", true
        ), adminToken).expectSuccess();
        Map<String, Object> assignedRoleEdit = new LinkedHashMap<>(
                rolePayload(roleName, roleKey, List.of(menuId)));
        assignedRoleEdit.put("roleId", roleId);
        assignedRoleEdit.put("status", "1");
        assertNotEquals(200, api.putJson("/system/role", assignedRoleEdit, adminToken)
                .expectEnvelope().code(), "已分配用户的角色不得通过基础编辑停用");
        assertEquals("0", api.get("/system/role/" + roleId, adminToken)
                .expectSuccess().dataObject().get("status"), "拒绝后角色状态必须保持不变");
        assertNotEquals(200, api.request("PUT", "/system/role/authUser/selectAll?roleId="
                        + MISSING_ID + "&userIds=5", null, null, adminToken)
                .expectEnvelope().code(), "不存在的角色不得授权用户");
        api.get("/system/role/authUser/allocatedList?roleId=" + roleId + "&pageNum=1&pageSize=20", adminToken)
                .expectPage();
        api.get("/system/role/authUser/unallocatedList?roleId=" + roleId + "&pageNum=1&pageSize=20", adminToken)
                .expectPage();
        api.putJson("/system/role/authUser/cancel", Map.of("roleId", roleId, "userId", 5), adminToken)
                .expectSuccess();
        api.request("PUT", "/system/role/authUser/selectAll?roleId=" + roleId + "&userIds=4",
                null, null, adminToken).expectSuccess();
        api.request("PUT", "/system/role/authUser/cancelAll?roleId=" + roleId + "&userIds=4",
                null, null, adminToken).expectSuccess();

        Map<String, Object> edit = new LinkedHashMap<>(rolePayload(roleName + "-改", roleKey, List.of(menuId)));
        edit.put("roleId", roleId);
        api.putJson("/system/role", edit, adminToken).expectSuccess();

        HttpApiTestSupport.Response protectedRole = api.putJson("/system/role/changeStatus",
                Map.of("roleId", 1, "status", "1"), adminToken).expectEnvelope();
        assertNotEquals(200, protectedRole.code(), "超级管理员角色不得被停用");
        assertEquals("0", api.get("/system/role/1", adminToken).expectSuccess().dataObject().get("status"),
                "超级管理员角色停用失败后状态必须保持不变");

        HttpApiTestSupport.Response currentUserRole = api.putJson("/system/role/authUser/cancel",
                Map.of("roleId", 1, "userId", 1), adminToken).expectEnvelope();
        assertNotEquals(200, currentUserRole.code(), "不得取消当前用户自己的角色");
        assertTrue(String.valueOf(currentUserRole.json().get("msg")).contains("当前用户"));
        assertTrue(api.get("/system/role/authUser/allocatedList?roleId=1&pageNum=1&pageSize=20", adminToken)
                .expectPage().dataObject().get("rows").toString().contains("admin"), "拒绝后角色关联必须保持不变");
    }

    private void exerciseMenuRoutes(long menuId, String menuName, long roleId) {
        HttpApiTestSupport.Response routers = api.get("/system/menu/getRouters", adminToken).expectSuccess();
        String routerJson = routers.json().toString();
        assertTrue(routerJson.contains("eos-icons:system-group"));
        assertTrue(routerJson.contains("solar:monitor-camera-outline"));
        assertTrue(routerJson.contains("solar:folder-with-files-outline"));
        assertTrue(routers.json().toString().contains("Http-menu-" + suffix + menuId),
                "动态路由 name 必须包含菜单 ID");
        HttpApiTestSupport.Response detail = api.get("/system/menu/" + menuId, adminToken).expectSuccess();
        assertEquals("/system/menu", detail.dataObject().get("activeMenu"));
        assertEquals("{\"badge\":\"test\"}", detail.dataObject().get("ext"));
        assertEquals("{\"source\":\"http\"}", detail.dataObject().get("queryParam"));
        assertEquals("N", detail.dataObject().get("isFrame"));
        assertEquals("Y", detail.dataObject().get("isCache"));
        api.get("/system/menu/treeselect", adminToken).expectSuccess();
        api.get("/system/menu/roleMenuTreeselect/" + roleId, adminToken).expectSuccess();

        Map<String, Object> edit = menuPayload(menuName + "-改", "http-menu-" + suffix);
        edit.put("menuId", menuId);
        api.putJson("/system/menu", edit, adminToken).expectSuccess();

        String invalidExternalName = "非法外链-" + suffix;
        Map<String, Object> invalidExternal = menuPayload(invalidExternalName, "relative-path");
        invalidExternal.put("isFrame", "Y");
        HttpApiTestSupport.Response response = api.postJson("/system/menu", invalidExternal, adminToken)
                .expectEnvelope();
        assertNotEquals(200, response.code(), "外链菜单必须使用 http(s) 地址");
        assertTrue(String.valueOf(response.json().get("msg")).contains("http"));
        assertTrue(api.get("/system/menu/list" + HttpApiTestSupport.query(Map.of("menuName", invalidExternalName)),
                adminToken).expectSuccess().dataList().isEmpty(), "非法外链失败后不得留下菜单");

        String duplicateRouteName = "重复路由-" + suffix;
        HttpApiTestSupport.Response duplicateRoute = api.postJson("/system/menu",
                menuPayload(duplicateRouteName, "http-menu-" + suffix), adminToken).expectEnvelope();
        assertNotEquals(200, duplicateRoute.code(), "同级路由地址不得重复");
        assertTrue(String.valueOf(duplicateRoute.json().get("msg")).contains("路由"));
        assertTrue(api.get("/system/menu/list" + HttpApiTestSupport.query(Map.of("menuName", duplicateRouteName)),
                adminToken).expectSuccess().dataList().isEmpty(), "重复路由失败后不得留下菜单");

        String sharedPath = "shared-route-" + suffix;
        Map<String, Object> directory = menuPayload("共享目录-" + suffix, sharedPath);
        directory.put("parentId", 0);
        directory.put("menuType", "M");
        directory.put("component", "");
        api.postJson("/system/menu", directory, adminToken).expectSuccess();
        long directoryId = rowId(api.get("/system/menu/list"
                        + HttpApiTestSupport.query(Map.of("menuName", "共享目录-" + suffix)), adminToken)
                .expectSuccess().dataList(), "menuName", "共享目录-" + suffix, "menuId", "id");

        Map<String, Object> page = menuPayload("共享页面-" + suffix, sharedPath);
        page.put("parentId", 1);
        api.postJson("/system/menu", page, adminToken).expectSuccess();
        long pageId = rowId(api.get("/system/menu/list"
                        + HttpApiTestSupport.query(Map.of("menuName", "共享页面-" + suffix)), adminToken)
                .expectSuccess().dataList(), "menuName", "共享页面-" + suffix, "menuId", "id");
        api.delete("/system/menu/" + pageId, adminToken).expectSuccess();
        api.delete("/system/menu/" + directoryId, adminToken).expectSuccess();
    }

    private void exerciseDeptRoutes(long deptId, String deptName) {
        api.get("/system/dept/optionselect?deptIds=" + deptId, adminToken).expectSuccess();
        api.get("/system/dept/list/exclude/" + deptId, adminToken).expectSuccess();
        api.get("/system/dept/" + deptId, adminToken).expectSuccess();

        Map<String, Object> edit = deptPayload(deptName + "-改");
        edit.put("deptId", deptId);
        api.putJson("/system/dept", edit, adminToken).expectSuccess();
        assertEquals("dept-category-" + suffix, api.get("/system/dept/" + deptId, adminToken)
                .expectSuccess().dataObject().get("deptCategory"));

        Map<String, Object> child = deptPayload("HTTP子部门-" + suffix);
        child.put("parentId", deptId);
        api.postJson("/system/dept", child, adminToken).expectSuccess();
        long childId = rowId(api.get("/system/dept/list" + HttpApiTestSupport.query(Map.of(
                "deptName", "HTTP子部门-" + suffix)), adminToken).expectSuccess().dataList(),
                "deptName", "HTTP子部门-" + suffix, "deptId", "id");
        Map<String, Object> childDetail = api.get("/system/dept/" + childId, adminToken)
                .expectSuccess().dataObject();
        assertTrue(String.valueOf(childDetail.get("ancestors")).endsWith("," + deptId));
        assertEquals(deptName + "-改", childDetail.get("parentName"));
        assertEquals("dept-category-" + suffix, childDetail.get("deptCategory"));
        api.delete("/system/dept/" + childId, adminToken).expectSuccess();

        String missingParentName = "无父部门-" + suffix;
        Map<String, Object> missingParent = deptPayload(missingParentName);
        missingParent.put("parentId", Long.MAX_VALUE);
        assertNotEquals(200, api.postJson("/system/dept", missingParent, adminToken)
                .expectEnvelope().code(), "父部门不存在时不得新增");
        assertTrue(api.get("/system/dept/list" + HttpApiTestSupport.query(Map.of("deptName", missingParentName)),
                adminToken).expectSuccess().dataList().isEmpty(), "父部门校验失败后不得留下部门");

        HttpApiTestSupport.Response protectedDept = api.delete("/system/dept/100", adminToken)
                .expectEnvelope();
        assertNotEquals(200, protectedDept.code(), "默认部门不得删除");
        assertTrue(String.valueOf(protectedDept.json().get("msg")).contains("不允许删除"));
        api.get("/system/dept/100", adminToken).expectSuccess();
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
        HttpApiTestSupport.Response assignedDelete = api.delete("/system/post/1", adminToken).expectEnvelope();
        assertNotEquals(200, assignedDelete.code(), "已分配用户的岗位不得删除");
        assertTrue(String.valueOf(assignedDelete.json().get("msg")).contains("不能删除"));
        assertEquals("0", api.get("/system/post/1", adminToken).expectSuccess().dataObject().get("status"),
                "岗位停用和删除失败后状态必须保持不变");
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
        payload.put("queryParam", "{\"source\":\"http\"}");
        payload.put("isFrame", "N");
        payload.put("isCache", "Y");
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
        payload.put("parentId", 0);
        payload.put("deptName", name);
        payload.put("deptCategory", "dept-category-" + suffix);
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
        payload.put("postCategory", "post-category-" + suffix);
        payload.put("postSort", 90);
        payload.put("status", status);
        return payload;
    }

    private long pageRowId(HttpApiTestSupport.Response response, String matchField, Object expected,
                           String... idFields) {
        return rowId(pageRows(response), matchField, expected, idFields);
    }

    @SuppressWarnings("unchecked")
    private List<Object> pageRows(HttpApiTestSupport.Response response) {
        Object rows = response.dataObject().get("rows");
        assertTrue(rows instanceof List<?>, "分页 rows 必须为数组");
        return (List<Object>) rows;
    }

    @SuppressWarnings("unchecked")
    private long rowId(List<Object> rows, String matchField, Object expected, String... idFields) {
        Map<String, Object> row = row(rows, matchField, expected);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> row(List<Object> rows, String matchField, Object expected) {
        return rows.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(item -> expected.equals(item.get(matchField)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 " + matchField + "=" + expected + " 的响应行"));
    }

    private void assertResponseFields(Map<String, Object> row, String... fields) {
        for (String field : fields) {
            assertTrue(row.containsKey(field), () -> "响应缺少字段 " + field + ": " + row);
            assertTrue(row.get(field) != null, () -> "响应字段不得为 null " + field + ": " + row);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> checkedKeys(HttpApiTestSupport.Response response) {
        Object checkedKeys = response.dataObject().get("checkedKeys");
        assertTrue(checkedKeys instanceof List<?>, "checkedKeys 必须为数组");
        return (List<Object>) checkedKeys;
    }

    @SuppressWarnings("unchecked")
    private boolean treeContainsId(List<Object> nodes, long expectedId) {
        for (Object node : nodes) {
            if (!(node instanceof Map<?, ?> map)) {
                continue;
            }
            Object id = map.get("id");
            if (id instanceof Number number && number.longValue() == expectedId) {
                return true;
            }
            if (id instanceof String text && !text.isBlank() && Long.parseLong(text) == expectedId) {
                return true;
            }
            Object children = map.get("children");
            if (children instanceof List<?> list && treeContainsId((List<Object>) list, expectedId)) {
                return true;
            }
        }
        return false;
    }
}
