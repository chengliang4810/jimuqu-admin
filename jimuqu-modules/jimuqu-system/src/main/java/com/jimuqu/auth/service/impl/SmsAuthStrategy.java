package com.jimuqu.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.AuthStrategy;
import com.jimuqu.auth.service.AuthStrategyService;
import com.jimuqu.auth.service.SysLoginService;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.domain.model.SmsLoginBody;
import com.jimuqu.common.core.enums.LoginType;
import com.jimuqu.common.core.enums.UserStatus;
import com.jimuqu.common.core.exception.user.CaptchaExpireException;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.MessageUtils;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.validation.ValidUtils;


/**
 * 短信认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component("sms" + AuthStrategy.BASE_NAME)
public class SmsAuthStrategy implements AuthStrategyService {

    @Inject
    private SysLoginService loginService;
    @Inject
    private CacheService cacheService;
    @Inject
    private SysUserMapper userMapper;

    @Override
    public LoginVo login(String body, SysClient client) {
        SmsLoginBody loginBody = JsonUtil.toObject(body, SmsLoginBody.class);
        ValidUtils.validateEntity(loginBody);
        String phonenumber = loginBody.getPhonenumber();
        String smsCode = loginBody.getSmsCode();

        // 通过手机号查找用户
        SysUserVo user = loadUserByPhonenumber(phonenumber);

        loginService.checkLogin(LoginType.SMS, user.getUserName(), () -> !validateSmsCode(phonenumber, smsCode));
        // 此处可根据登录用户的数据不同 自行创建 loginUser 属性不够用继承扩展就行了
        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter model = AuthStrategy.buildLoginParameter(client);
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        return loginVo;
    }

    /**
     * 校验短信验证码
     */
    private boolean validateSmsCode(String phonenumber, String smsCode) {
        String code = cacheService.get(GlobalConstants.CAPTCHA_CODE_KEY + phonenumber, String.class);
        if (StringUtil.isBlank(code)) {
            loginService.recordLogininfor(phonenumber, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(smsCode);
    }

    private SysUserVo loadUserByPhonenumber(String phonenumber) {
        return ensureLoginAllowed(userMapper.selectUserByPhonenumber(phonenumber), phonenumber);
    }

    static SysUserVo ensureLoginAllowed(SysUserVo user, String phonenumber) {
        if (ObjUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", phonenumber);
            throw new UserException("user.not.exists", phonenumber);
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", phonenumber);
            throw new UserException("user.blocked", phonenumber);
        }
        return user;
    }

}
