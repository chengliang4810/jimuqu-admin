package com.jimuqu.system.controller;

import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.auth.controller.AuthController;
import com.jimuqu.common.core.domain.model.RegisterBody;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.bo.SysUserPasswordBo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PasswordEndpointEncryptionTest {

    @Test
    void passwordWriteEndpointsRequireEncryptedRequests() throws NoSuchMethodException {
        assertNotNull(AuthController.class
                .getMethod("login", String.class)
                .getAnnotation(ApiEncrypt.class));
        assertNotNull(SysUserController.class
                .getMethod("resetPwd", SysUserBo.class)
                .getAnnotation(ApiEncrypt.class));
        assertNotNull(SysProfileController.class
                .getMethod("updatePwd", SysUserPasswordBo.class)
                .getAnnotation(ApiEncrypt.class));
        assertNotNull(AuthController.class
                .getMethod("register", RegisterBody.class)
                .getAnnotation(ApiEncrypt.class));
    }
}
