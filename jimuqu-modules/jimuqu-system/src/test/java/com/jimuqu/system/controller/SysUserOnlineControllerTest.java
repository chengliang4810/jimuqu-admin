package com.jimuqu.system.controller;

import java.util.List;

class SysUserOnlineControllerTest {

    public static void main(String[] args) {
        assert SysUserOnlineController.ownsToken(List.of("current", "other-device"), "other-device");
        assert !SysUserOnlineController.ownsToken(List.of("current", "other-device"), "foreign");
    }
}
