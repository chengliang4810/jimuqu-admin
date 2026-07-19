package com.jimuqu.common.security.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.filter.SaFilterErrorStrategy;
import com.jimuqu.common.core.exception.auth.AuthException;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限异常处理器
 *
 * @author chengliang
 * @since 2024/02/26
 */
@Slf4j
public class SecurityExceptionHandler implements SaFilterErrorStrategy {

    @Override
    public SecurityExceptionHandler run(Throwable throwable) {
        if (throwable instanceof NotPermissionException) {
            throw new AuthException(403, "没有访问权限，请联系管理员授权");
        } else if (throwable instanceof NotRoleException) {
            throw new AuthException(403, "没有访问权限，请联系管理员授权");
        } else if (throwable instanceof NotLoginException exception) {
            String message = switch (exception.getType()) {
                case NotLoginException.TOKEN_TIMEOUT, NotLoginException.TOKEN_FREEZE ->
                        "登录已过期，请重新登录";
                case NotLoginException.BE_REPLACED ->
                        "当前账号已在其他设备登录，您已被强制下线";
                case NotLoginException.KICK_OUT ->
                        "账号已被管理员强制下线";
                default -> "登录状态异常，请重新登录";
            };
            throw new AuthException(401, message);
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException("权限校验异常", throwable);
    }

}
