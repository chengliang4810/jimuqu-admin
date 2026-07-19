package com.jimuqu.common.log.aspect;

import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import com.jimuqu.common.log.event.OperLogEvent;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.UploadedFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogAspectTest {

    @Test
    void removesSensitiveAndExplicitlyExcludedFieldsFromNestedRequestBody() {
        String body = "{\"username\":\"admin\",\"password\":\"secret\",\"items\":[{\"newPassword\":\"next\",\"token\":\"hidden\",\"name\":\"kept\"}]}";

        assertEquals("{\"username\":\"admin\",\"items\":[{\"name\":\"kept\"}]}",
                LogAspect.sanitizeRequestBody(body, new String[]{"token"}));
    }

    @Test
    void masksAnnotatedFieldsFromTypedRequestBody() {
        String body = "{\"email\":\"admin@jimuqu.test\",\"phoneNumber\":\"13800138000\",\"name\":\"kept\"}";

        String sanitized = LogAspect.sanitizeRequestBody(body, SensitiveBody.class, new String[0]);

        assertEquals("{\"email\":\"a***@jimuqu.test\",\"phoneNumber\":\"138****8000\",\"name\":\"kept\"}",
                sanitized);
    }

    @Test
    void malformedRequestBodyFailsClosed() {
        assertEquals("[请求体无法解析]",
                LogAspect.sanitizeRequestBody("{\"password\":\"secret\"", SensitiveBody.class, new String[0]));
    }

    @Test
    void snapshotsOperatorIdentityAndPrefersRequestClientKey() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(12L);
        loginUser.setDeptId(34L);
        loginUser.setDeptName("研发部");
        loginUser.setUsername("admin");
        loginUser.setClientKey("session-client");
        loginUser.setDeviceType("pc");
        loginUser.setBrowser("Chrome");
        loginUser.setOs("Windows");
        OperLogEvent event = new OperLogEvent();

        LogAspect.snapshotOperator(event, loginUser, "request-client");

        assertAll(
                () -> assertEquals(12L, event.getUserId()),
                () -> assertEquals(34L, event.getDeptId()),
                () -> assertEquals("研发部", event.getDeptName()),
                () -> assertEquals("admin", event.getOperName()),
                () -> assertEquals("request-client", event.getClientKey()),
                () -> assertEquals("pc", event.getDeviceType()),
                () -> assertEquals("Chrome", event.getBrowser()),
                () -> assertEquals("Windows", event.getOs())
        );
    }

    @Test
    void fallsBackToSessionClientKeyWhenHeaderIsMissing() {
        LoginUser loginUser = new LoginUser();
        loginUser.setClientKey("session-client");
        OperLogEvent event = new OperLogEvent();

        LogAspect.snapshotOperator(event, loginUser, null);

        assertEquals("session-client", event.getClientKey());
    }

    @Test
    void filtersUploadedFilesEvenWhenTheyAreNotTheFirstCollectionItem() {
        assertTrue(new LogAspect().isFilterObject(List.of("metadata", new UploadedFile())));
    }

    private static class SensitiveBody {
        @Sensitive(type = SensitiveType.EMAIL)
        private String email;

        @Sensitive(type = SensitiveType.MOBILE)
        private String phoneNumber;

        private String name;
    }
}
