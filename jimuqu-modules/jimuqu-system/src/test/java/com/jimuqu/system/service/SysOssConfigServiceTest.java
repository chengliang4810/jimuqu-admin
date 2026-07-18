package com.jimuqu.system.service;

class SysOssConfigServiceTest {

    public static void main(String[] args) {
        assert SysOssConfigService.SYSTEM_CONFIG_IDS.contains(1761900000000000001L);
        assert SysOssConfigService.SYSTEM_CONFIG_IDS.contains(1761900000000000004L);
        assert !SysOssConfigService.SYSTEM_CONFIG_IDS.contains(1761900000000000005L);
    }
}
