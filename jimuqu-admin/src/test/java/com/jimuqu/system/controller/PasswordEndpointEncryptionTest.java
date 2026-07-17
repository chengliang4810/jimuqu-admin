package com.jimuqu.system.controller;

import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.bo.SysUserPasswordBo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PasswordEndpointEncryptionTest {

    @Test
    void passwordWriteEndpointsRequireEncryptedRequests() throws NoSuchMethodException {
        assertNotNull(SysUserController.class
                .getMethod("resetPwd", SysUserBo.class)
                .getAnnotation(ApiEncrypt.class));
        assertNotNull(SysProfileController.class
                .getMethod("updatePwd", SysUserPasswordBo.class)
                .getAnnotation(ApiEncrypt.class));
    }
}
