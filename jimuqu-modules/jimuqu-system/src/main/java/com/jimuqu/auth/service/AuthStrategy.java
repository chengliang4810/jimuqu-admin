package com.jimuqu.auth.service;


import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysClient;
import org.noear.solon.Solon;

/**
 * 授权策略
 *
 * @author Michelle.Chung
 */
public interface AuthStrategy {

    String BASE_NAME = "AuthStrategy";

    /**
     * 登录
     */
    static LoginVo login(String body, SysClient client, String grantType) {
        // 授权类型和客户端id
        String beanName = grantType + BASE_NAME;
        AuthStrategyService instance = Solon.context().getBean(beanName);
        if (instance == null) {
            throw new ServiceException("授权类型不正确!");
        }
        return instance.login(body, client);
    }

    /**
     * 根据客户端配置构建 Sa-Token 登录参数。
     */
    static SaLoginParameter buildLoginParameter(SysClient client) {
        SaLoginParameter parameter = new SaLoginParameter();
        parameter.setDeviceType(client.getDeviceType());
        parameter.setTimeout(client.getTimeout());
        parameter.setActiveTimeout(client.getActiveTimeout());
        parameter.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        parameter.setExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY, client.getAccessPath());
        parameter.setExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY, client.getIpWhitelist());
        return parameter;
    }

}
