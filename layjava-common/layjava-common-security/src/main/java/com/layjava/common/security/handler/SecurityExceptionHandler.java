package com.layjava.common.security.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.filter.SaFilterErrorStrategy;
import com.layjava.common.core.exception.auth.AuthException;
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
        } else if (throwable instanceof NotLoginException) {
            throw new AuthException(401, "认证失败，无法访问系统资源");
        } else {
            throw new AuthException(500, throwable.getMessage());
        }
    }

}
