package com.jimuqu.system.runner;

import com.jimuqu.system.service.SystemSeedService;
import com.jimuqu.system.service.SysOssConfigService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemApplicationRunnerTest {

    @Test
    void restoresOssPlatformsAfterSeedData() {
        List<String> calls = new ArrayList<>();
        SystemSeedService seed = new SystemSeedService(null, null, null, null, null, null, null,
                null, null, null, null, null, null) {
            @Override
            public void initialize() {
                calls.add("seed");
            }
        };
        SysOssConfigService oss = new SysOssConfigService(null, null) {
            @Override
            public void initPlatforms() {
                calls.add("oss");
            }
        };

        new SystemApplicationRunner(seed, oss).start();

        assertEquals(List.of("seed", "oss"), calls);
    }
}
