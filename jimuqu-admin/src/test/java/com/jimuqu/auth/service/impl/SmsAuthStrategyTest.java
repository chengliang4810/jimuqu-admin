package com.jimuqu.auth.service.impl;

import com.jimuqu.common.core.enums.UserStatus;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.system.domain.vo.SysUserVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmsAuthStrategyTest {

    @Test
    void rejectsMissingAndDisabledUsers() {
        assertThrows(UserException.class,
                () -> SmsAuthStrategy.ensureLoginAllowed(null, "13800000000"));

        SysUserVo disabled = new SysUserVo().setStatus(UserStatus.DISABLE.getCode());
        assertThrows(UserException.class,
                () -> SmsAuthStrategy.ensureLoginAllowed(disabled, "13800000000"));

        SysUserVo enabled = new SysUserVo().setStatus(UserStatus.OK.getCode());
        assertSame(enabled, SmsAuthStrategy.ensureLoginAllowed(enabled, "13800000000"));
    }
}
