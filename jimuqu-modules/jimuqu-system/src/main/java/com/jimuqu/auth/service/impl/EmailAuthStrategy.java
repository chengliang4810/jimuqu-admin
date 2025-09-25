package com.jimuqu.auth.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.xbatis.core.sql.executor.Where;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.AuthStrategy;
import com.jimuqu.auth.service.AuthStrategyService;
import com.jimuqu.auth.service.SysLoginService;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.model.EmailLoginBody;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.LoginType;
import com.jimuqu.common.core.enums.UserStatus;
import com.jimuqu.common.core.exception.user.CaptchaExpireException;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.validation.ValidUtils;


/**
 * 邮件认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component("email" + AuthStrategy.BASE_NAME)
public class EmailAuthStrategy implements AuthStrategyService {

    @Inject
    private SysLoginService loginService;
    @Inject
    private CacheService cacheService;
    @Inject
    private SysUserMapper userMapper;

    @Override
    public LoginVo login(String body, SysClient client) {
        EmailLoginBody loginBody = JsonUtil.toObject(body, EmailLoginBody.class);
        ValidUtils.validateEntity(loginBody);
        String email = loginBody.getEmail();
        String emailCode = loginBody.getEmailCode();

        // 通过邮箱查找用户
        SysUserVo user = loadUserByEmail(email);

        loginService.checkLogin(LoginType.EMAIL, user.getUserName(), () -> !validateEmailCode(email, emailCode));
        // 此处可根据登录用户的数据不同 自行创建 loginUser 属性不够用继承扩展就行了
        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginModel model = new SaLoginModel();
        model.setDevice(client.getDeviceType());
        // 自定义分配 不同用户体系 不同 token 授权时间 不设置默认走全局 yml 配置
        // 例如: 后台用户30分钟过期 app用户1天过期
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        return loginVo;
    }

    /**
     * 校验邮箱验证码
     */
    private boolean validateEmailCode(String email, String emailCode) {
        String code = cacheService.get(GlobalConstants.CAPTCHA_CODE_KEY + email, String.class);
        if (StringUtil.isBlank(code)) {
            loginService.recordLogininfor(email, Constants.LOGIN_FAIL, "验证码已失效" );
            throw new CaptchaExpireException();
        }
        return code.equals(emailCode);
    }

    private SysUserVo loadUserByEmail(String email) {
        SysUser user = userMapper.get(Where.create().eq(SysUser::getEmail, email), SysUser::getEmail, SysUser::getStatus);

        if (ObjUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", email);
            throw new UserException("user.not.exists", email);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", email);
            throw new UserException("user.blocked", email);
        }
        return userMapper.selectUserByEmail(email);
    }

}
