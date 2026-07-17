package com.jimuqu.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.xbatis.core.sql.executor.Where;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.model.RegisterBody;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.common.core.exception.user.CaptchaException;
import com.jimuqu.common.core.exception.user.CaptchaExpireException;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.log.event.LogininforEvent;
import com.jimuqu.common.web.config.properties.CaptchaProperties;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.service.SysUserService;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.data.cache.CacheService;


/**
 * 注册校验方法
 *
 * @author Lion Li,chengliang4810
 */

@Component
public class SysRegisterService {

    @Inject
    private SysUserService userService;
    @Inject
    private CacheService cacheService;
    @Inject
    private SysUserMapper userMapper;
    @Inject
    private CaptchaProperties captchaProperties;

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        boolean captchaEnabled = Boolean.TRUE.equals(captchaProperties.getEnable());
        // 验证码开关
        if (captchaEnabled) {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }
        SysUserBo sysUser = new SysUserBo();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(userType);


        boolean exist = userMapper.exists(Where.create()
                .eq(SysUser::getUserName, sysUser.getUserName()));
        if (exist) {
            throw new UserException("保存用户 {} 失败，注册账号已存在", username);
        }
        boolean regFlag = userService.registerUser(sysUser);
        if (!regFlag) {
            throw new UserException("user.register.error" );
        }
        recordLogininfor(username, Constants.REGISTER, "注册成功" );
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtil.defaultIfBlank(uuid, "" );
        String captcha = cacheService.get(verifyKey, String.class);
        cacheService.remove(verifyKey);
        if (captcha == null) {
            recordLogininfor(username, Constants.REGISTER, "验证码已失效" );
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            recordLogininfor(username, Constants.REGISTER, "验证码错误" );
            throw new CaptchaException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     * @return
     */
    private void recordLogininfor(String username, String status, String message) {
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        EventBus.publish(logininforEvent);
    }

}
