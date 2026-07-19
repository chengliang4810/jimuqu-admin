package com.jimuqu.system.service.impl;

import com.jimuqu.system.domain.SysUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysUserRegistrationParityTest {

    @Test
    void registrationWritesSystemAuditIds() {
        SysUser registration = new SysUser().setUserName("registered-user");
        registration.setCreateBy(99L);
        registration.setUpdateBy(88L);

        SysUser prepared = SysUserServiceImpl.prepareRegistrationUser(registration);

        assertEquals(0L, prepared.getCreateBy());
        assertEquals(0L, prepared.getUpdateBy());
        assertEquals("registered-user", prepared.getUserName());
    }
}
