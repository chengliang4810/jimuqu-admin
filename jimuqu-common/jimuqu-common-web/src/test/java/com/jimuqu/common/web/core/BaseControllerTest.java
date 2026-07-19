package com.jimuqu.common.web.core;

import com.jimuqu.common.core.domain.PageResult;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.base.BaseException;
import com.jimuqu.common.core.utils.JsonUtil;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.Result;
import org.noear.solon.validation.ValidatorException;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NoRepeatSubmit;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BaseControllerTest {

    @Test
    void rendersNoRepeatSubmitFailureWithLocalizedBellMessage() throws Exception {
        Method method = BaseControllerTest.class.getDeclaredMethod("protectedWrite");
        NoRepeatSubmit annotation = method.getAnnotation(NoRepeatSubmit.class);
        ValidatorException exception = new ValidatorException(
                400, "@NoRepeatSubmit verification failed", annotation, Result.failure(), method);

        R<Void> chinese = BaseController.validationError(exception, null);
        R<Void> english = BaseController.validationError(exception, "en-US,en;q=0.9");

        assertEquals(500, chinese.getCode());
        assertEquals("不允许重复提交，请稍候再试", chinese.getMsg());
        assertNull(chinese.getData());
        assertEquals(500, english.getCode());
        assertEquals("Repeat submit is not allowed, please try again later", english.getMsg());
        assertNull(english.getData());
    }

    @Test
    void resolvesAllLoginModeValidationKeysAndPrefersContentLanguage() throws Exception {
        Method method = BaseControllerTest.class.getDeclaredMethod("protectedEmailWrite", String.class);
        NotBlank annotation = method.getParameters()[0].getAnnotation(NotBlank.class);
        ValidatorException exception = new ValidatorException(
                400, annotation.message(), annotation, Result.failure(), method);

        R<Void> response = BaseController.validationError(exception, "en_US", "zh-CN");

        assertEquals(500, response.getCode());
        assertEquals("Email code cannot be blank", response.getMsg());
        assertNull(response.getData());
    }

    @Test
    void unknownControllerErrorsUseTraceableUpstreamStyleMessages() {
        R<Void> runtime = BaseController.unexpectedError(
                new IllegalStateException("sensitive runtime detail"), "12345678");
        R<Void> checked = BaseController.unexpectedError(
                new Exception("sensitive checked detail"), "87654321");

        assertEquals(500, runtime.getCode());
        assertEquals("发生未知异常，请联系管理员 [错误编号: 12345678]", runtime.getMsg());
        assertNull(runtime.getData());
        assertEquals(500, checked.getCode());
        assertEquals("发生系统异常，请联系管理员 [错误编号: 87654321]", checked.getMsg());
        assertNull(checked.getData());
    }

    @Test
    void preservesBaseExceptionMessageAndDefaultsMissingCode() {
        R<Void> response = BaseController.baseError(
                new BaseException("ratelimit", null, "访问过于频繁，请稍候再试"));

        assertEquals(500, response.getCode());
        assertEquals("访问过于频繁，请稍候再试", response.getMsg());
        assertNull(response.getData());
    }

    @Test
    void normalizesInternalPaginationToThePublicRowsAndTotalContract() {
        InternalPage<String> internalPage = new InternalPage<>();
        internalPage.setRows(List.of("row"));
        internalPage.setTotal(1L);

        R<?> response = BaseController.normalizeResponse(internalPage);
        PageResult<?> data = assertInstanceOf(PageResult.class, response.getData());

        assertEquals(200, response.getCode());
        assertEquals(PageResult.class, data.getClass());
        assertEquals(List.of("row"), data.getRows());
        assertEquals(1L, data.getTotal());
        assertEquals("{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"rows\":[\"row\"],\"total\":1}}",
                JsonUtil.toString(response));
    }

    @Test
    void preservesAnExistingEnvelopeWhileNormalizingItsPaginationData() {
        InternalPage<String> internalPage = new InternalPage<>();
        internalPage.setRows(List.of("row"));
        internalPage.setTotal(1L);
        R<InternalPage<String>> source = R.ok("分页成功", internalPage);

        R<?> response = BaseController.normalizeResponse(source);

        assertEquals(source.getCode(), response.getCode());
        assertEquals(source.getMsg(), response.getMsg());
        assertEquals(PageResult.class, response.getData().getClass());
        R<String> plain = R.ok("plain");
        assertSame(plain, BaseController.normalizeResponse(plain));
    }

    @NoRepeatSubmit
    private void protectedWrite() {
    }

    private void protectedEmailWrite(@NotBlank(message = "{email.code.not.blank}") String code) {
    }

    private static final class InternalPage<T> extends PageResult<T> {

        private int currentPage = 1;

        public int getCurrentPage() {
            return currentPage;
        }
    }
}
