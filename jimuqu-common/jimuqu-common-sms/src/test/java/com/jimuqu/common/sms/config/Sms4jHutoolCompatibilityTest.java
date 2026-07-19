package com.jimuqu.common.sms.config;

import cn.hutool.core.bean.copier.CopyOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class Sms4jHutoolCompatibilityTest {

    @Test
    void shouldProvideTheHutoolApiRequiredBySms4jInitializer() {
        assertDoesNotThrow(() -> CopyOptions.create().setFormatIfDate("yyyy-MM-dd HH:mm:ss"));
    }
}
