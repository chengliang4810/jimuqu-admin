package com.jimuqu.common.satoken.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SaTokenConfigTest {

    @Test
    void shouldProvideJwtSimpleLoginLogic() {
        StpLogic logic = new SaTokenConfig().getStpLogicJwt();
        assertInstanceOf(StpLogicJwtForSimple.class, logic);

        logic.setConfig(new cn.dev33.satoken.config.SaTokenConfig()
                .setJwtSecretKey("jimuqu-satoken-test-secret")
                .setTokenName("Authorization"));
        String token = logic.createTokenValue(1L, "web", 3600, Map.of("userId", 1L));

        assertEquals(3, token.split("\\.", -1).length);
        assertEquals(1L, ((Number) logic.getExtra(token, "userId")).longValue());
    }
}
