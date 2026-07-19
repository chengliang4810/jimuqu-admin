package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.bo.SysUserProfileBo;
import com.jimuqu.system.service.SysDeptService;
import com.jimuqu.system.service.SysRoleService;
import com.jimuqu.system.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.ValidUtils;
import org.noear.solon.validation.ValidatorException;
import org.noear.solon.validation.annotation.NoRepeatSubmit;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorParityContractTest {

    @Test
    void roleWriteOperationsUseRepeatSubmitProtection() throws Exception {
        assertNoRepeatSubmit("dataScope", SysRoleBo.class);
        assertNoRepeatSubmit("changeStatus", SysRoleBo.class);
        assertNoRepeatSubmit("cancelAuthUser", SysUserRole.class);
        assertNoRepeatSubmit("cancelAuthUserAll", Long.class, Long[].class);
        assertNoRepeatSubmit("selectAuthUserAll", Long.class, Long[].class);
    }

    @Test
    void changingRoleStatusDelegatesToRoleService() {
        AtomicInteger updateCount = new AtomicInteger();
        SysRoleService roleService = proxy(SysRoleService.class, (method, arguments) -> {
            if ("updateRoleStatus".equals(method.getName())) {
                assertEquals(7L, arguments[0]);
                assertEquals("1", arguments[1]);
                updateCount.incrementAndGet();
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        SysRoleController controller = new SysRoleController(
                proxy(SysUserService.class, (method, arguments) -> defaultValue(method.getReturnType())),
                proxy(SysDeptService.class, (method, arguments) -> defaultValue(method.getReturnType())),
                roleService);

        assertEquals(200, controller.changeStatus(new SysRoleBo().setId(7L)
                .setRoleName("测试角色").setStatus("1")).getCode());
        assertEquals(1, updateCount.get());
    }

    @Test
    void importTemplateDoesNotRequireImportPermission() throws Exception {
        Method method = SysUserController.class.getMethod("importTemplate");

        assertFalse(method.isAnnotationPresent(SaCheckPermission.class));
    }

    @Test
    void uploadAllowsRepeatedFiles() throws Exception {
        Method method = SysFileController.class.getMethod("upload", UploadedFile.class, String.class);

        assertFalse(method.isAnnotationPresent(NoRepeatSubmit.class));
    }

    @Test
    void loginUnlockUsesRepeatSubmitProtection() throws Exception {
        Method method = SysLoginInfoController.class.getMethod("unlock", String.class);

        assertTrue(method.isAnnotationPresent(NoRepeatSubmit.class));
    }

    @Test
    void menuQueryParamMustBeAJsonObject() {
        assertDoesNotThrow(() -> SysMenuController.validateQueryParam(null));
        assertDoesNotThrow(() -> SysMenuController.validateQueryParam("  "));
        assertDoesNotThrow(() -> SysMenuController.validateQueryParam("{}"));

        ServiceException arrayError = assertThrows(ServiceException.class,
                () -> SysMenuController.validateQueryParam("[]"));
        ServiceException malformedError = assertThrows(ServiceException.class,
                () -> SysMenuController.validateQueryParam("{bad json}"));

        assertEquals("路由参数必须符合JSON格式", arrayError.getMessage());
        assertEquals("路由参数必须符合JSON格式", malformedError.getMessage());
    }

    @Test
    void userNameRequiresAtLeastTwoCharacters() {
        SysUserBo user = new SysUserBo().setUserName("x").setNickName("测试用户");

        assertThrows(ValidatorException.class, () -> ValidUtils.validateEntity(user));
    }

    @Test
    void profileRejectsInvalidPhoneNumber() {
        SysUserProfileBo profile = new SysUserProfileBo();
        profile.setPhonenumber("12345");

        assertThrows(ValidatorException.class, () -> ValidUtils.validateEntity(profile));
    }

    private static void assertNoRepeatSubmit(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = SysRoleController.class.getMethod(methodName, parameterTypes);
        assertTrue(method.isAnnotationPresent(NoRepeatSubmit.class));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> invocation.invoke(method, arguments));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(Method method, Object[] arguments) throws Throwable;
    }
}
