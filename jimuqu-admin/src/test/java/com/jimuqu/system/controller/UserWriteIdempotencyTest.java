package com.jimuqu.system.controller;

import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.bo.SysUserPasswordBo;
import com.jimuqu.system.domain.bo.SysUserProfileBo;
import org.junit.jupiter.api.Test;
import org.noear.solon.validation.annotation.NoRepeatSubmit;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserWriteIdempotencyTest {

    @Test
    void userWriteEndpointsRejectRepeatedSubmissions() throws NoSuchMethodException {
        assertProtected(SysProfileController.class.getMethod("updateProfile", SysUserProfileBo.class));
        assertProtected(SysProfileController.class.getMethod("updatePwd", SysUserPasswordBo.class));
        assertProtected(SysUserController.class.getMethod("resetPwd", SysUserBo.class));
        assertProtected(SysUserController.class.getMethod("changeStatus", SysUserBo.class));
        assertProtected(SysUserController.class.getMethod("insertAuthRole", SysUserBo.class));
    }

    private static void assertProtected(Method method) {
        assertNotNull(method.getAnnotation(NoRepeatSubmit.class));
    }
}
