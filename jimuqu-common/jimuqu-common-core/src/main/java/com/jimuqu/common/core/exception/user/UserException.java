package com.jimuqu.common.core.exception.user;

import com.jimuqu.common.core.exception.base.BaseException;

import java.io.Serial;

/**
 * 用户信息异常类
 *
 * @author jimuqu
 */
public class UserException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserException(String msg, Object... args) {
        super("user", 500, msg, args);
    }
}
