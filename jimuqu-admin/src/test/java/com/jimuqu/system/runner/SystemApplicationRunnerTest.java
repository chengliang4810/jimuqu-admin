package com.jimuqu.system.runner;

import com.jimuqu.system.service.SysOssConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemApplicationRunnerTest {

    @Test
    void restoresOssPlatforms() {
        boolean[] initialized = {false};
        SysOssConfigService oss = new SysOssConfigService(null, null, null) {
            @Override
            public void initPlatforms() {
                initialized[0] = true;
            }
        };

        new SystemApplicationRunner(oss).start();

        assertTrue(initialized[0]);
    }
}
