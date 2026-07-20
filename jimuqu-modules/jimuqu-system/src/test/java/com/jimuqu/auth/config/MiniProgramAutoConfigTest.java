package com.jimuqu.auth.config;

import com.jimuqu.auth.config.properties.MiniProgramProperties;
import com.jimuqu.auth.service.MiniProgramIdentityAdapter;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Condition;
import org.noear.solon.core.Props;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniProgramAutoConfigTest {

    @Test
    void bindsMultipleApplicationsFromSolonConfiguration() {
        Props configuration = new Props();
        configuration.put("auth.mini-program.enabled", "true");
        configuration.put("auth.mini-program.endpoint", "http://127.0.0.1/code2session");
        configuration.put("auth.mini-program.apps.primary.appid", "wx-primary");
        configuration.put("auth.mini-program.apps.primary.secret", "primary-secret");
        configuration.put("auth.mini-program.apps.secondary.appid", "wx-secondary");
        configuration.put("auth.mini-program.apps.secondary.secret", "secondary-secret");

        MiniProgramProperties properties = MiniProgramAutoConfig.bind(configuration);

        assertTrue(properties.isEnabled());
        assertEquals("http://127.0.0.1/code2session", properties.getEndpoint());
        assertEquals("wx-primary", properties.getApps().get("primary").getAppid());
        assertEquals("secondary-secret", properties.getApps().get("secondary").getSecret());
    }

    @Test
    void defaultAdapterCanBeReplacedByAnApplicationBean() throws Exception {
        Condition condition = MiniProgramAutoConfig.class
                .getMethod("miniProgramIdentityAdapter", MiniProgramProperties.class)
                .getAnnotation(Condition.class);

        assertEquals(MiniProgramIdentityAdapter.class, condition.onMissingBean());
    }
}
