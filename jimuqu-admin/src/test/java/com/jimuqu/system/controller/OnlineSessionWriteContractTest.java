package com.jimuqu.system.controller;

import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import org.junit.jupiter.api.Test;
import org.noear.solon.validation.annotation.NoRepeatSubmit;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OnlineSessionWriteContractTest {

    @Test
    void sessionRemovalKeepsAuditAndRepeatSubmitProtection() throws Exception {
        assertWriteContract(SysUserOnlineController.class.getMethod("forceLogout", String.class), "在线用户");
        assertWriteContract(SysUserOnlineController.class.getMethod("removeMyself", String.class), "在线设备");
    }

    private void assertWriteContract(Method method, String title) {
        Log log = method.getAnnotation(Log.class);
        assertNotNull(log);
        assertEquals(title, log.title());
        assertEquals(BusinessType.FORCE, log.businessType());
        assertNotNull(method.getAnnotation(NoRepeatSubmit.class));
    }
}
