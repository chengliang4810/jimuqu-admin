package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserUnlockContractTest {

    @Test
    void exposesUpstreamUnlockOperation() throws NoSuchMethodException {
        Method method = SysUserController.class.getMethod("unlock", Long.class);
        assertNotNull(method.getAnnotation(Get.class));
        assertEquals("/unlock/{userId}", method.getAnnotation(Mapping.class).value());
        assertEquals("system:user:edit", method.getAnnotation(SaCheckPermission.class).value()[0]);
    }
}
