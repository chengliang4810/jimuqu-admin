package com.jimuqu.system.domain.vo;

import cn.hutool.core.lang.Dict;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.bo.SysDictDataBo;
import com.jimuqu.system.domain.bo.SysDictTypeBo;
import com.jimuqu.system.domain.bo.SysMenuBo;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.bo.SysUserProfileBo;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BellVoJsonContractTest {

    @Test
    void serializesLoginTokenWithBellSnakeCaseFields() {
        LoginVo login = new LoginVo()
                .setAccessToken("access-token")
                .setRefreshToken("refresh-token")
                .setExpireIn(3_000_000_000L)
                .setRefreshExpireIn(4_000_000_000L)
                .setClientId("client-id")
                .setScope("all")
                .setOpenid("openid");

        Dict json = JsonUtil.toMap(JsonUtil.toString(login));

        assertEquals("access-token", json.get("access_token"));
        assertEquals("refresh-token", json.get("refresh_token"));
        assertEquals("3000000000", String.valueOf(json.get("expire_in")));
        assertEquals("4000000000", String.valueOf(json.get("refresh_expire_in")));
        assertEquals("client-id", json.get("client_id"));
        assertEquals("all", json.get("scope"));
        assertEquals("openid", json.get("openid"));
        assertFalse(json.containsKey("accessToken"));
        assertFalse(json.containsKey("refreshToken"));
        assertFalse(json.containsKey("expireIn"));
        assertFalse(json.containsKey("refreshExpireIn"));
        assertFalse(json.containsKey("clientId"));
    }

    @Test
    void serializesUserAndProfileWithoutInternalOrSecretFields() {
        SysUserVo user = new SysUserVo()
                .setId(21L)
                .setPhonenumber("13800138000")
                .setPassword("must-not-leak")
                .setRoleIds(List.of(1L, 2L))
                .setPostIds(List.of(3L));
        Dict userJson = JsonUtil.toMap(JsonUtil.toString(user));

        assertEquals("21", String.valueOf(userJson.get("userId")));
        assertEquals("13800138000", userJson.get("phoneNumber"));
        assertNumberList(List.of(1L, 2L), userJson.get("roleIds"));
        assertNumberList(List.of(3L), userJson.get("postIds"));
        assertFalse(userJson.containsKey("id"));
        assertFalse(userJson.containsKey("phonenumber"));
        assertFalse(userJson.containsKey("password"));

        ProfileUserVo profile = new ProfileUserVo();
        profile.setId(22L);
        profile.setPhonenumber("13900139000");
        Dict profileJson = JsonUtil.toMap(JsonUtil.toString(profile));
        assertEquals("22", String.valueOf(profileJson.get("userId")));
        assertEquals("13900139000", profileJson.get("phoneNumber"));
        assertFalse(profileJson.containsKey("id"));
        assertFalse(profileJson.containsKey("phonenumber"));

        SysClientVo client = new SysClientVo().setId(23L).setClientKey("pc");
        Dict clientJson = JsonUtil.toMap(JsonUtil.toString(client));
        assertFalse(clientJson.containsKey("delFlag"));
    }

    @Test
    void serializesDictionaryComputedBellFields() {
        SysDictDataVo dictData = new SysDictDataVo()
                .setId(31L)
                .setDictTypeKey("sys_normal_disable");
        Dict dictDataJson = JsonUtil.toMap(JsonUtil.toString(dictData));
        assertEquals("31", String.valueOf(dictDataJson.get("dictCode")));
        assertEquals("sys_normal_disable", dictDataJson.get("dictType"));
        assertFalse(dictDataJson.containsKey("id"));
        assertFalse(dictDataJson.containsKey("dictTypeKey"));

        SysDictTypeVo dictType = new SysDictTypeVo()
                .setDictId(32L)
                .setDictKey("sys_user_gender")
                .setDictType("L");
        Dict dictTypeJson = JsonUtil.toMap(JsonUtil.toString(dictType));
        assertEquals("sys_user_gender", dictTypeJson.get("dictType"));
        assertFalse(dictTypeJson.containsKey("dictKey"));
    }

    @Test
    void deserializesBellRequestFieldNamesWithSnack4() {
        SysUserBo user = JsonUtil.toObject("""
                {"userId":41,"phoneNumber":"13700137000","roleIds":[1,2],"postIds":[3],
                 "loginIp":"203.0.113.9","loginDate":"2026-07-19 12:00:00"}
                """, SysUserBo.class);
        assertEquals(41L, user.getId());
        assertEquals("13700137000", user.getPhonenumber());
        assertNumberList(List.of(1L, 2L), user.getRoleIds());
        assertNumberList(List.of(3L), user.getPostIds());
        Dict userJson = JsonUtil.toMap(JsonUtil.toString(user));
        assertFalse(userJson.containsKey("loginIp"));
        assertFalse(userJson.containsKey("loginDate"));

        SysUserProfileBo profile = JsonUtil.toObject(
                "{\"userId\":999,\"phoneNumber\":\"13600136000\"}",
                SysUserProfileBo.class);
        Dict profileJson = JsonUtil.toMap(JsonUtil.toString(profile));
        assertEquals("13600136000", profileJson.get("phoneNumber"));
        assertFalse(profileJson.containsKey("userId"));

        SysRoleBo role = JsonUtil.toObject("{\"roleId\":42}", SysRoleBo.class);
        assertEquals(42L, role.getId());

        SysDictDataBo dictData = JsonUtil.toObject(
                "{\"dictCode\":43,\"dictType\":\"sys_common_status\"}", SysDictDataBo.class);
        assertEquals(43L, dictData.getId());
        assertEquals("sys_common_status", dictData.getDictType());

        SysDictTypeBo dictType = JsonUtil.toObject(
                "{\"dictId\":44,\"dictType\":\"sys_notice_type\"}", SysDictTypeBo.class);
        assertEquals(44L, dictType.getDictId());
        assertEquals("sys_notice_type", dictType.getDictType());
    }

    @Test
    void serializesBellFieldAliasesWithSnack4() {
        assertAlias(new SysConfigVo().setId(11L), "configId", "id", 11L);
        assertAlias(new SysDeptVo().setId(12L), "deptId", "id", 12L);
        assertAlias(new SysMenuVo().setId(13L), "menuId", "id", 13L);
        assertAlias(new SysRoleVo().setId(14L), "roleId", "id", 14L);
        assertAlias(new SysDictTypeVo().setDictKey("sys_test").setDictType("L"),
                "dictType", "dictKey", "sys_test");

        Dict menuJson = JsonUtil.toMap(JsonUtil.toString(
                new SysMenuVo().setQueryParam("{\"id\":1}")));
        assertEquals("{\"id\":1}", menuJson.get("queryParam"));
        assertFalse(menuJson.containsKey("query"));

        SysMenuBo menu = JsonUtil.toObject("{\"queryParam\":\"{\\\"id\\\":1}\"}", SysMenuBo.class);
        assertEquals("{\"id\":1}", menu.getQueryParam());

        Dict legacyMenuJson = JsonUtil.toMap(JsonUtil.toString(
                new SysMenuVo().setIsFrame("1").setIsCache("0")));
        assertEquals("N", legacyMenuJson.get("isFrame"));
        assertEquals("Y", legacyMenuJson.get("isCache"));

        SysMenu defaults = new SysMenu();
        assertEquals("N", defaults.getIsFrame());
        assertEquals("Y", defaults.getIsCache());
    }

    @Test
    void exposesFieldsConsumedByBellLists() {
        Date createdAt = new Date(0L);
        assertFields(new SysDeptVo().setDeptCategory("company").setCreateTime(createdAt),
                "deptCategory", "createTime", "children");
        assertFields(new SysMenuVo().setCreateDept(100L).setCreateTime(createdAt),
                "createDept", "createTime", "children");
        assertFields(new SysPostVo().setDeptName("研发部").setCreateTime(createdAt),
                "deptName", "createTime");
        assertFields(new SysDictDataVo().setCreateTime(createdAt), "createTime");
        assertFields(new SysDictTypeVo().setCreateTime(createdAt), "createTime");
        assertFields(new SysConfigVo().setCreateTime(createdAt), "createTime");
        assertFields(new SysUserVo().setDeptName("研发部").setCreateTime(createdAt),
                "deptName", "createTime");
        assertFields(new SysOssVo().setExt1("{}"), "ext1");
        Integer[] businessTypes = {1, 2};
        SysOperLogVo operLog = new SysOperLogVo();
        operLog.setBusinessTypes(businessTypes);
        assertFields(operLog, "businessTypes");
    }

    private void assertAlias(Object value, String expectedKey, String legacyKey, Object expectedValue) {
        Dict json = JsonUtil.toMap(JsonUtil.toString(value));
        assertTrue(json.containsKey(expectedKey), () -> "缺少 Bell 字段: " + expectedKey + ", json=" + json);
        assertFalse(json.containsKey(legacyKey), () -> "不应暴露内部字段: " + legacyKey + ", json=" + json);
        assertEquals(String.valueOf(expectedValue), String.valueOf(json.get(expectedKey)));
    }

    private void assertFields(Object value, String... fields) {
        Dict json = JsonUtil.toMap(JsonUtil.toString(value));
        for (String field : fields) {
            assertTrue(json.containsKey(field), () -> "缺少 Bell 字段: " + field + ", json=" + json);
        }
    }

    private void assertNumberList(List<Long> expected, Object actual) {
        assertTrue(actual instanceof List<?>);
        assertEquals(expected, ((List<?>) actual).stream()
                .map(value -> ((Number) value).longValue())
                .toList());
    }
}
