package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.bo.SysMenuBo;
import com.jimuqu.system.domain.vo.SysMenuVo;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Mapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RbacControllerContractTest {

    @Test
    void controllerBindingsMatchBellContract() throws Exception {
        Method optionselect = SysUserController.class.getMethod("optionselect", Long[].class, Long.class);
        assertEquals("/optionselect", optionselect.getAnnotation(Mapping.class).value());

        Method addRole = SysRoleController.class.getMethod("add", SysRoleBo.class);
        assertTrue(addRole.getParameters()[0].isAnnotationPresent(Body.class));

        assertSuperAdminRestriction(SysMenuController.class.getMethod("list", com.jimuqu.system.domain.query.SysMenuQuery.class));
        assertSuperAdminRestriction(SysMenuController.class.getMethod("getInfo", Long.class));
        assertSuperAdminRestriction(SysMenuController.class.getMethod("add", SysMenuBo.class));
        assertSuperAdminRestriction(SysMenuController.class.getMethod("edit", SysMenuBo.class));
        assertSuperAdminRestriction(SysMenuController.class.getMethod("delete", java.util.List.class));
        assertSuperAdminRestriction(SysMenuController.class.getMethod("cascadeDelete", java.util.List.class));
    }

    @Test
    void legacyMenuFlagsAreRenderedAsBellValues() {
        assertEquals("Y", new SysMenuVo().setIsFrame("0").getIsFrame());
        assertEquals("N", new SysMenuVo().setIsFrame("1").getIsFrame());
        assertEquals("Y", new SysMenuVo().setIsCache("0").getIsCache());
        assertEquals("N", new SysMenuVo().setIsCache("1").getIsCache());
    }

    private static void assertSuperAdminRestriction(Method method) {
        SaCheckRole annotation = method.getAnnotation(SaCheckRole.class);
        assertEquals(GlobalConstants.SUPER_ADMIN_ROLE_KEY, annotation.value()[0]);
    }
}
