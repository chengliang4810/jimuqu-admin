package com.jimuqu.system.domain;

import com.jimuqu.common.core.enums.UserType;
import org.dromara.autotable.annotation.AutoColumn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemUserTypeContractTest {

    @Test
    void usesJimuSystemUserCodeAndAutoTableDefault() throws Exception {
        assertEquals("sys_user", UserType.SYS_USER.getUserType());
        assertEquals(UserType.SYS_USER, UserType.getUserType("sys_user"));

        AutoColumn column = SysUser.class.getDeclaredField("userType").getAnnotation(AutoColumn.class);
        assertEquals("sys_user", column.defaultValue());
    }
}
