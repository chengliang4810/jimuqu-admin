package com.jimuqu.auth.service;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthStrategyClientRuleTest {

    @Test
    void carriesAllClientRulesIntoTokenParameters() {
        SysClient client = new SysClient()
                .setClientId("client")
                .setDeviceType("pc")
                .setTimeout(60L)
                .setActiveTimeout(30L)
                .setAccessPath("/system/**")
                .setIpWhitelist("127.0.0.1");

        SaLoginParameter parameter = AuthStrategy.buildLoginParameter(client);

        assertEquals("client", parameter.getExtra(LoginHelper.CLIENT_KEY));
        assertEquals("/system/**", parameter.getExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY));
        assertEquals("127.0.0.1", parameter.getExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY));
        assertEquals(60L, parameter.getTimeout());
        assertEquals(30L, parameter.getActiveTimeout());
    }
}
