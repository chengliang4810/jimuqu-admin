package com.jimuqu.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.v7.core.bean.BeanUtil;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.AuthStrategy;
import com.jimuqu.auth.service.AuthStrategyService;
import com.jimuqu.auth.service.MiniProgramIdentityAdapter;
import com.jimuqu.auth.service.SysLoginService;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.domain.model.XcxLoginBody;
import com.jimuqu.common.core.domain.model.XcxLoginUser;
import com.jimuqu.common.core.enums.UserStatus;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.vo.SysSocialVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.service.SysSocialService;
import com.jimuqu.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.validation.ValidUtils;

import java.util.List;

/**
 * 小程序登录策略。
 */
@Slf4j
@Component("xcx" + AuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class XcxAuthStrategy implements AuthStrategyService {

    private final MiniProgramIdentityAdapter identityAdapter;
    private final SysSocialService socialService;
    private final SysUserService userService;
    private final SysLoginService loginService;

    @Override
    public LoginVo login(String body, SysClient client) {
        XcxLoginBody loginBody = JsonUtil.toObject(body, XcxLoginBody.class);
        ValidUtils.validateEntity(loginBody);
        if (!identityAdapter.isAvailable()) {
            throw new ServiceException("小程序登录未启用，请先配置小程序身份服务");
        }
        MiniProgramIdentityAdapter.MiniProgramIdentity identity = identityAdapter.authenticate(
                loginBody.getAppid(), loginBody.getXcxCode());
        if (identity == null || StringUtil.isBlank(identity.openId())) {
            throw new ServiceException("小程序身份服务未返回 openid");
        }

        SysUserVo user = loadUser(resolveUserId(identity), identity.openId());
        LoginUser baseLoginUser = loginService.buildLoginUser(user);
        XcxLoginUser loginUser = BeanUtil.toBean(baseLoginUser, XcxLoginUser.class);
        loginUser.setOpenid(identity.openId());
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter parameter = AuthStrategy.buildLoginParameter(client);
        LoginHelper.login(loginUser, parameter);

        return new LoginVo()
                .setAccessToken(StpUtil.getTokenValue())
                .setExpireIn(StpUtil.getTokenTimeout())
                .setClientId(client.getClientId())
                .setOpenid(identity.openId());
    }

    private Long resolveUserId(MiniProgramIdentityAdapter.MiniProgramIdentity identity) {
        if (identity.userId() != null) {
            return identity.userId();
        }
        List<SysSocialVo> bindings = socialService.selectByAuthId(
                MiniProgramIdentityAdapter.WECHAT_MINI_PROGRAM_SOURCE + identity.openId());
        if (bindings.isEmpty()) {
            throw new ServiceException("你还没有绑定小程序账号，绑定后才可以登录！");
        }
        return bindings.get(0).getUserId();
    }

    private SysUserVo loadUser(Long userId, String openId) {
        SysUserVo user = userService.queryById(userId);
        if (user == null) {
            log.info("小程序登录绑定的用户不存在，openid：{}", openId);
            throw new UserException("user.not.exists", openId);
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("小程序登录绑定的用户已停用，用户ID：{}", userId);
            throw new UserException("user.blocked", user.getUserName());
        }
        return user;
    }
}
