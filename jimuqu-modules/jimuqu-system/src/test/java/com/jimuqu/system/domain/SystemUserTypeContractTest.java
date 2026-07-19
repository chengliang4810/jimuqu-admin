package com.jimuqu.system.domain;

import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.system.GenderEnum;
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

    @Test
    void usesBellGenderCodes() {
        assertEquals("0", GenderEnum.MAN.getValue());
        assertEquals("男", GenderEnum.MAN.getLabel());
        assertEquals("1", GenderEnum.WOMAN.getValue());
        assertEquals("女", GenderEnum.WOMAN.getLabel());
        assertEquals("2", GenderEnum.UNKNOWN.getValue());
        assertEquals("未知", GenderEnum.UNKNOWN.getLabel());
    }
}
