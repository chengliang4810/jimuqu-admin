package com.jimuqu.common.core.sensitive;

import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import com.jimuqu.common.core.sensitive.utils.SensitiveUtil;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SensitiveUtilTest {

    @Test
    void maskKnownSensitiveTypes() {
        assertEquals("138****5678", SensitiveUtil.desensitize("13812345678", SensitiveType.MOBILE));
        assertEquals("t***@example.com", SensitiveUtil.desensitize("test@example.com", SensitiveType.EMAIL));
        assertEquals("1101**********1234", SensitiveUtil.desensitize("110101199001011234", SensitiveType.ID_CARD));
        assertEquals("6222***********8888", SensitiveUtil.desensitize("6222020202028888", SensitiveType.BANK_CARD));
        assertEquals("张*三", SensitiveUtil.desensitize("张三三", SensitiveType.NAME));
        assertEquals("北京市朝阳区******", SensitiveUtil.desensitize("北京市朝阳区建国路88号", SensitiveType.ADDRESS));
    }

    @Test
    void nullEmptyAndTooShortValuesDoNotThrow() {
        assertNull(SensitiveUtil.desensitize(null, SensitiveType.MOBILE));
        assertEquals("", SensitiveUtil.desensitize("", SensitiveType.EMAIL));
        assertEquals("12", SensitiveUtil.desensitize("12", SensitiveType.MOBILE));
        assertEquals("a@b.com", SensitiveUtil.desensitize("a@b.com", SensitiveType.EMAIL));
    }

    @Test
    void customKeepPrefixAndSuffixUsesAnnotationConfiguration() throws Exception {
        Sensitive sensitive = Demo.class.getDeclaredField("token").getAnnotation(Sensitive.class);

        assertEquals("ab***yz", SensitiveUtil.desensitize("abcdefyz", sensitive));
    }

    @Data
    static class Demo {
        @Sensitive(type = SensitiveType.CUSTOM, prefixKeep = 2, suffixKeep = 2, mask = "***")
        private String token;
    }
}
