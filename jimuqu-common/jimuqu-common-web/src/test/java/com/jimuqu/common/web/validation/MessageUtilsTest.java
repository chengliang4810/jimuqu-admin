package com.jimuqu.common.web.validation;

import com.jimuqu.common.core.enums.LoginType;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.core.utils.MessageUtils;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageUtilsTest {

    @Test
    void resolvesParameterizedMessagesForSupportedLocales() {
        assertEquals("对不起, 您的账号：admin 不存在.",
                MessageUtils.message("user.not.exists", Locale.SIMPLIFIED_CHINESE, "admin"));
        assertEquals("Sorry, your account: admin does not exist",
                MessageUtils.message("user.not.exists", Locale.US, "admin"));
        assertEquals("Password input error 3 times, account locked for 10 minutes",
                MessageUtils.message(LoginType.PASSWORD.getRetryLimitExceed(), Locale.US, 3, 10));
    }

    @Test
    void contentLanguageTakesPrecedenceAndUnknownKeysRemainCompatible() {
        assertEquals(Locale.US, MessageUtils.resolveLocale("en-US", "zh-CN"));
        assertEquals(Locale.SIMPLIFIED_CHINESE, MessageUtils.resolveLocale(null, "zh-CN,en;q=0.9"));
        assertEquals("自定义错误 value", MessageUtils.message("自定义错误 {}", Locale.US, "value"));
    }

    @Test
    void baseExceptionUsesTheSameMessageResolver() {
        assertEquals("对不起，您的账号：admin 已禁用，请联系管理员",
                new UserException("user.blocked", "admin").getMessage());
    }
}
