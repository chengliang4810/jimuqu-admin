package com.jimuqu.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.auth.domain.bo.LoginBody;
import com.jimuqu.auth.domain.vo.LoginVo;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

/**
 * 身份验证服务
 *
 * @author chengliang
 * @since 2024/04/02
 */
@Slf4j
@Component
public class AuthService {

    /**
     * 登录
     *
     * @param loginBody 登录主体
     * @return {@link LoginVo}
     */
    public LoginVo login(LoginBody loginBody) {

        log.info("登录信息：{}", loginBody);

        // 登录
        // StpUtil.login(loginBody.getAccount());

        // 返回 Token
        String tokenValue = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();

        return new LoginVo().setAccessToken(tokenValue).setExpireIn(Math.toIntExact(tokenTimeout));
    }

}
