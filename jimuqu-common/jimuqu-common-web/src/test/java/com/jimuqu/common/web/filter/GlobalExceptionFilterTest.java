package com.jimuqu.common.web.filter;

import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.exception.check.AssertException;
import com.jimuqu.common.core.domain.model.PasswordLoginBody;
import com.jimuqu.common.web.validation.ValidationMessageResolver;
import org.junit.jupiter.api.Test;
import org.noear.solon.validation.ValidUtils;
import org.noear.solon.validation.ValidatorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        assertEquals("账户长度必须在2到20个字符之间",
                ValidationMessageResolver.resolve(exception, null));
        assertEquals("Account length must be between 2 and 20 characters",
                ValidationMessageResolver.resolve(exception, "en-US,en;q=0.9"));
        assertEquals("账户长度必须在2到20个字符之间",
                ValidationMessageResolver.resolve(exception, "zh-CN,en;q=0.9"));
    }
}
