package com.jimuqu.common.core.xss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XssValidatorTest {

    private final Xss annotation = Sample.class.getDeclaredFields()[0].getAnnotation(Xss.class);

    @Test
    void rejectsHtmlAndAcceptsPlainText() {
        assertEquals(400, XssValidator.INSTANCE.validateOfValue(annotation, "<script>alert(1)</script>", new StringBuilder()).getCode());
        assertEquals(200, XssValidator.INSTANCE.validateOfValue(annotation, "普通用户", new StringBuilder()).getCode());
    }

    private static class Sample {
        @Xss
        private String value;
    }
}
