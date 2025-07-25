package com.jimuqu.auth.listener;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.http.useragent.UserAgent;
import cn.hutool.v7.http.useragent.UserAgentUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;

import java.util.Date;

import static com.jimuqu.common.satoken.utils.LoginHelper.LOGIN_USER_KEY;

/**
 * 用户行为 侦听器的实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener implements SaTokenListener {

    /**
     * 每次登录时触发
     *
     * @param loginType      账号类别
     * @param loginId        账号id
     * @param tokenValue     本次登录产生的 token 值
     * @param loginParameter 登录参数
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter loginParameter) {
        UserAgent userAgent = UserAgentUtil.parse(Context.current().header("User-Agent"));
        String ip = Context.current().realIp();

        System.out.println("用户登录------------------------------");
        // 更新在线用户信息
        SaStorage storage = SaHolder.getStorage();
        LoginUser loginUser = storage.getModel(LOGIN_USER_KEY, LoginUser.class);

        // 补充客户端登录信息
        loginUser.setBrowser(userAgent.getBrowser().getName());
        loginUser.setOs(userAgent.getOs().getName());
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtil.getRealAddressByIP(ip));
        loginUser.setLoginTime(new Date());
        loginUser.setToken(tokenValue);
    }

    /**
     * 每次注销时触发
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {

    }

    /**
     * 每次被踢下线时触发
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        log.info("user doKickout, userId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被顶下线时触发
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        log.info("user doReplaced, userId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被封禁时触发
     */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
    }

    /**
     * 每次被解封时触发
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
    }

    /**
     * 每次打开二级认证时触发
     */
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCreateSession(String id) {
    }

    /**
     * 每次注销Session时触发
     */
    @Override
    public void doLogoutSession(String id) {
    }

    /**
     * 每次 Token 续期时触发（注意：是 timeout 续期，而不是 active-timeout 续期）
     *
     * @param loginType  账号类别
     * @param loginId    账号id
     * @param tokenValue token 值
     * @param timeout    续期时间
     */
    @Override
    public void doRenewTimeout(String loginType, Object loginId, String tokenValue, long timeout) {

    }

}
