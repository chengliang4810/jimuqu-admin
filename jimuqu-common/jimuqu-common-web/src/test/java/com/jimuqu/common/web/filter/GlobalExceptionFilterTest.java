package com.jimuqu.common.web.filter;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.exception.check.AssertException;
import com.jimuqu.common.core.domain.model.PasswordLoginBody;
import com.jimuqu.common.web.validation.ValidationMessageResolver;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.exception.StatusException;
import org.noear.snack4.json.JsonParseException;
import org.noear.solon.validation.ValidUtils;
import org.noear.solon.validation.ValidatorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionFilterTest {

    @Test
    void preservesServiceErrorCodeAndMessage() {
        R<Void> response = GlobalExceptionFilter.serviceError(new ServiceException("数据已存在", 409));

        assertEquals(409, response.getCode());
        assertEquals("数据已存在", response.getMsg());
    }

    @Test
    void preservesBaseErrorCodeAndMessage() {
        R<Void> response = GlobalExceptionFilter.baseError(new AssertException("客户端key已存在"));

        assertEquals(500, response.getCode());
        assertEquals("客户端key已存在", response.getMsg());
    }

    @Test
    void resolvesValidationMessageKeyAndLengthArguments() {
        PasswordLoginBody body = new PasswordLoginBody();
        body.setUsername("x");
        body.setPassword("Password123!");

        ValidatorException exception = assertThrows(ValidatorException.class,
                () -> ValidUtils.validateEntity(body));

        assertEquals("账户长度必须在2到30个字符之间",
                ValidationMessageResolver.resolve(exception, null));
        assertEquals("Account length must be between 2 and 30 characters",
                ValidationMessageResolver.resolve(exception, "en-US,en;q=0.9"));
        assertEquals("账户长度必须在2到30个字符之间",
                ValidationMessageResolver.resolve(exception, "zh-CN,en;q=0.9"));
        assertEquals(500, ValidationMessageResolver.errorCode(exception));
    }

    @Test
    void preservesSolonNotFoundAndMethodNotAllowedSemantics() {
        R<Void> notFound = GlobalExceptionFilter.statusError(
                new StatusException("Not Found: GET /missing", 404));
        R<Void> methodNotAllowed = GlobalExceptionFilter.statusError(
                new StatusException("Method Not Allowed: POST /auth/code", 405));

        assertEquals(404, notFound.getCode());
        assertEquals("请求地址不存在", notFound.getMsg());
        assertEquals(405, methodNotAllowed.getCode());
        assertEquals("Method Not Allowed: POST /auth/code", methodNotAllowed.getMsg());
    }

    @Test
    void recognizesWrappedSnackJsonParseException() {
        RuntimeException wrapped = new RuntimeException(new JsonParseException("invalid json"));

        assertEquals(JsonParseException.class,
                GlobalExceptionFilter.findCause(wrapped, JsonParseException.class).getClass());
    }

    @Test
    void addsUpstreamStyleErrorIdWithoutLeakingExceptionDetails() {
        R<Void> runtime = GlobalExceptionFilter.unexpectedError(
                new IllegalStateException("database password leaked"), "12345678");
        R<Void> checked = GlobalExceptionFilter.unexpectedError(
                new Exception("internal path leaked"), "87654321");

        assertEquals(500, runtime.getCode());
        assertEquals("发生未知异常，请联系管理员 [错误编号: 12345678]", runtime.getMsg());
        assertNull(runtime.getData());
        assertEquals(500, checked.getCode());
        assertEquals("发生系统异常，请联系管理员 [错误编号: 87654321]", checked.getMsg());
        assertNull(checked.getData());
        assertTrue(GlobalExceptionFilter.newErrorId().matches("\\d{8}"));
    }

    @Test
    void onlyAuthenticationAndPermissionErrorsUseNonOkTransportStatus() {
        assertEquals(401, GlobalExceptionFilter.transportStatus(401));
        assertEquals(403, GlobalExceptionFilter.transportStatus(403));
        assertEquals(200, GlobalExceptionFilter.transportStatus(400));
        assertEquals(200, GlobalExceptionFilter.transportStatus(404));
        assertEquals(200, GlobalExceptionFilter.transportStatus(405));
        assertEquals(200, GlobalExceptionFilter.transportStatus(500));
    }
}
