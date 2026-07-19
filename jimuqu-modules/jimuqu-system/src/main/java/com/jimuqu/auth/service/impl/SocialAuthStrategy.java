package com.jimuqu.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.AuthStrategy;
import com.jimuqu.auth.service.AuthStrategyService;
import com.jimuqu.auth.service.SysLoginService;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.domain.model.SocialLoginBody;
import com.jimuqu.common.core.enums.UserStatus;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.social.config.properties.SocialProperties;
import com.jimuqu.common.social.utils.SocialUtils;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.vo.SysSocialVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.service.SysSocialService;
import com.jimuqu.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.noear.solon.annotation.Component;
import org.noear.solon.validation.ValidUtils;

import java.util.List;

/**
 * 第三方授权登录策略。
 */
@Slf4j
@Component("social" + AuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SocialAuthStrategy implements AuthStrategyService {

    private final SocialProperties socialProperties;
    private final SysSocialService socialService;
    private final SysUserService userService;
    private final SysLoginService loginService;

    @Override
    public LoginVo login(String body, SysClient client) {
        SocialLoginBody loginBody = JsonUtil.toObject(body, SocialLoginBody.class);
        ValidUtils.validateEntity(loginBody);
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
                loginBody.getSource(), loginBody.getSocialCode(),
                loginBody.getSocialState(), socialProperties);
        if (!response.ok()) {
            throw new ServiceException(response.getMsg());
        }

        AuthUser authUser = response.getData();
        List<SysSocialVo> bindings = socialService.selectByAuthId(authUser.getSource() + authUser.getUuid());
        if (bindings.isEmpty()) {
            throw new ServiceException("你还没有绑定第三方账号，绑定后才可以登录！");
        }
        SysUserVo user = loadUser(bindings.get(0).getUserId());
        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter parameter = AuthStrategy.buildLoginParameter(client);
        LoginHelper.login(loginUser, parameter);

        return new LoginVo()
                .setAccessToken(StpUtil.getTokenValue())
                .setExpireIn(StpUtil.getTokenTimeout())
                .setClientId(client.getClientId());
    }

    private SysUserVo loadUser(Long userId) {
        SysUserVo user = userService.queryById(userId);
        if (user == null) {
            log.info("第三方登录绑定的用户不存在，用户ID：{}", userId);
            throw new UserException("user.not.exists", String.valueOf(userId));
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("第三方登录绑定的用户已停用，用户ID：{}", userId);
            throw new UserException("user.blocked", user.getUserName());
        }
        return user;
    }
}
