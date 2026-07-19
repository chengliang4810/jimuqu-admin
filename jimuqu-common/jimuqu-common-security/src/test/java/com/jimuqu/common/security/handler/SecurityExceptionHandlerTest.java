package com.jimuqu.common.security.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.jimuqu.common.core.exception.auth.AuthException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler = new SecurityExceptionHandler();

    @Test
    void convertsKnownAuthorizationFailuresToThePublicContract() {
        AuthException exception = assertThrows(AuthException.class,
                () -> handler.run(new NotPermissionException("internal permission")));

        assertEquals(403, exception.getCode());
        assertEquals("没有访问权限，请联系管理员授权", exception.getMessage());

        AuthException roleException = assertThrows(AuthException.class,
                () -> handler.run(new NotRoleException("internal role")));
        assertEquals(403, roleException.getCode());
        assertEquals("没有访问权限，请联系管理员授权", roleException.getMessage());
    }

    @Test
    void preservesSixXLoginStateMessages() {
        assertLoginFailure(NotLoginException.TOKEN_TIMEOUT, "登录已过期，请重新登录");
        assertLoginFailure(NotLoginException.TOKEN_FREEZE, "登录已过期，请重新登录");
        assertLoginFailure(NotLoginException.BE_REPLACED,
                "当前账号已在其他设备登录，您已被强制下线");
        assertLoginFailure(NotLoginException.KICK_OUT, "账号已被管理员强制下线");
        assertLoginFailure(NotLoginException.NOT_TOKEN, "登录状态异常，请重新登录");
    }

    @Test
    void letsUnexpectedRuntimeFailuresReachTheGlobalSanitizer() {
        IllegalStateException source = new IllegalStateException("database secret");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> handler.run(source));

        assertSame(source, exception);
    }

    @Test
    void wrapsCheckedFailuresWithoutPublishingTheirMessage() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> handler.run(new IOException("filesystem secret")));

        assertEquals("权限校验异常", exception.getMessage());
    }

    private void assertLoginFailure(String type, String expectedMessage) {
        NotLoginException source = NotLoginException.newInstance(
                "login", type, "internal token state", "token-secret");
        AuthException exception = assertThrows(AuthException.class, () -> handler.run(source));

        assertEquals(401, exception.getCode());
        assertEquals(expectedMessage, exception.getMessage());
    }
}
