package com.jimuqu.system.service.impl;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.jimuqu.common.mybatis.model.DataScopeAccess;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataScopeAccessResolverTest {

    @Test
    void resolvesPermissionAndRoleBypassFromActionMethod() throws Exception {
        Method method = SampleController.class.getDeclaredMethod("list");

        DataScopeAccess access = DataScopeAccessResolver.resolve(method, SampleController.class);

        assertEquals(Set.of("system:user:list", "system:user:query"), access.permissions());
        assertEquals(Set.of("admin", "auditor"), access.roleKeys());
    }

    @Test
    void fallsBackToControllerAnnotations() throws Exception {
        Method method = ClassSecuredController.class.getDeclaredMethod("list");

        DataScopeAccess access = DataScopeAccessResolver.resolve(method, ClassSecuredController.class);

        assertEquals(Set.of("system:notice:list"), access.permissions());
        assertEquals(Set.of("notice_admin"), access.roleKeys());
    }

    private static class SampleController {
        @SaCheckPermission(value = {"system:user:list", " system:user:query "}, orRole = {"admin"})
        @SaCheckRole("auditor")
        void list() {
        }
    }

    @SaCheckPermission("system:notice:list")
    @SaCheckRole("notice_admin")
    private static class ClassSecuredController {
        void list() {
        }
    }
}
