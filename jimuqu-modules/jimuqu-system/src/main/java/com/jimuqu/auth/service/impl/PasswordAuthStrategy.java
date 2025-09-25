package com.jimuqu.auth.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.xbatis.core.sql.executor.Where;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.AuthStrategy;
import com.jimuqu.auth.service.AuthStrategyService;
import com.jimuqu.auth.service.SysLoginService;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.domain.model.PasswordLoginBody;
import com.jimuqu.common.core.enums.LoginType;
import com.jimuqu.common.core.enums.UserStatus;
import com.jimuqu.common.core.exception.user.CaptchaException;
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
 * 密码认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Component("password" + AuthStrategy.BASE_NAME)
public class PasswordAuthStrategy implements AuthStrategyService {

//    @Inject
//    private ConfigService configService;
    @Inject
    private CacheService cacheService;
    @Inject
    private SysLoginService loginService;
    @Inject
    private SysUserMapper userMapper;

    @Override
    public LoginVo login(String body, SysClient client) {
        PasswordLoginBody loginBody = JsonUtil.toObject(body, PasswordLoginBody.class);
        ValidUtils.validateEntity(loginBody);
        String username = loginBody.getUsername();
        String password = loginBody.getPassword();
        String code = loginBody.getCode();
        String uuid = loginBody.getUuid();

//        String captchaEnabled = configService.getConfigValue("system_captcha_enable" );
//        // TODO 验证码开关
//        if ("on".equals(captchaEnabled)) {
//            validateCaptcha(username, code, uuid);
//        }

        SysUserVo user = loadUserByUsername(username);
        loginService.checkLogin(LoginType.PASSWORD, username, () -> !BCrypt.checkpw(password, user.getPassword()));
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
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
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    private void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtil.defaultIfBlank(uuid, "" );
        String captcha = cacheService.get(verifyKey, String.class);
        cacheService.remove(verifyKey);
        if (captcha == null) {
            loginService.recordLogininfor(username, Constants.LOGIN_FAIL, "验证码过期" );
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            loginService.recordLogininfor(username, Constants.LOGIN_FAIL, "验证码错误" );
            throw new CaptchaException();
        }
    }

    private SysUserVo loadUserByUsername(String username) {
        SysUser user = userMapper.get(Where.create().eq(SysUser::getUserName, username), SysUser::getUserName, SysUser::getStatus);
        if (ObjUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UserException("user.not.exists", username);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UserException("user.blocked", username);
        }
        return userMapper.selectUserByUserName(username);
    }

}
