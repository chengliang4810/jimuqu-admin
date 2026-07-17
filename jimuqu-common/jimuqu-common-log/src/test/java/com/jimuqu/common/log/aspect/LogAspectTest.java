package com.jimuqu.common.log.aspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogAspectTest {

    @Test
    void removesSensitiveAndExplicitlyExcludedFieldsFromNestedRequestBody() {
        String body = "{\"username\":\"admin\",\"password\":\"secret\",\"items\":[{\"newPassword\":\"next\",\"token\":\"hidden\",\"name\":\"kept\"}]}";

        assertEquals("{\"username\":\"admin\",\"items\":[{\"name\":\"kept\"}]}",
                LogAspect.sanitizeRequestBody(body, new String[]{"token"}));
    }
}
