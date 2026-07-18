package com.jimuqu.system.controller;

import java.util.List;
import java.util.Date;

import com.jimuqu.system.domain.vo.SysUserOnlineVo;

class SysUserOnlineControllerTest {

    public static void main(String[] args) {
        assert SysUserOnlineController.ownsToken(List.of("current", "other-device"), "other-device");
        assert !SysUserOnlineController.ownsToken(List.of("current", "other-device"), "foreign");

        SysUserOnlineVo older = new SysUserOnlineVo().setTokenId("older").setLoginTime(new Date(1));
        SysUserOnlineVo newer = new SysUserOnlineVo().setTokenId("newer").setLoginTime(new Date(2));
        SysUserOnlineVo unknown = new SysUserOnlineVo().setTokenId("unknown");
        assert SysUserOnlineController.newestFirst(List.of(older, unknown, newer))
                .stream().map(SysUserOnlineVo::getTokenId).toList()
                .equals(List.of("newer", "older", "unknown"));
    }
}
