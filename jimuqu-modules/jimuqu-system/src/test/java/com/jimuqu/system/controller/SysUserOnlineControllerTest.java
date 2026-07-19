package com.jimuqu.system.controller;

import java.util.List;

import com.jimuqu.system.domain.vo.SysUserOnlineVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysUserOnlineControllerTest {

    @Test
    void recognizesOnlyTokensOwnedByCurrentLogin() {
        assertTrue(SysUserOnlineController.ownsToken(List.of("current", "other-device"), "other-device"));
        assertFalse(SysUserOnlineController.ownsToken(List.of("current", "other-device"), "foreign"));
    }

    @Test
    void sortsKnownLoginTimesNewestFirstAndUnknownLast() {
        SysUserOnlineVo older = new SysUserOnlineVo().setTokenId("older").setLoginTime(1L);
        SysUserOnlineVo newer = new SysUserOnlineVo().setTokenId("newer").setLoginTime(2L);
        SysUserOnlineVo unknown = new SysUserOnlineVo().setTokenId("unknown");
        assertEquals(List.of("newer", "older", "unknown"),
                SysUserOnlineController.newestFirst(List.of(older, unknown, newer))
                        .stream().map(SysUserOnlineVo::getTokenId).toList());
    }
}
