package com.jimuqu.common.social.config;

import com.jimuqu.common.social.config.properties.SocialLoginConfigProperties;
import com.jimuqu.common.social.config.properties.SocialProperties;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.core.Props;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialPropertiesTest {

    @Test
    void usesSingleConfigurationBeanDefinition() {
        assertFalse(SocialProperties.class.isAnnotationPresent(Component.class));
        assertFalse(SocialProperties.class.isAnnotationPresent(Configuration.class),
                "配置 POJO 不应自行注册，必须由 SocialAutoConfig 创建唯一 Bean");
        assertTrue(SocialAutoConfig.class.isAnnotationPresent(Configuration.class));
    }

    @Test
    void bindsNestedProviderMapsFromSolonConfiguration() {
        Props configuration = new Props();
        configuration.put("justauth.enabled", "true");
        configuration.put("justauth.type.GITEE.client-id", "local-client");
        configuration.put("justauth.type.GITEE.client-secret", "local-secret");
        configuration.put("justauth.type.GITEE.redirect-uri", "http://127.0.0.1/social-callback?source=gitee");
        configuration.put("justauth.type.GITEE.local-code", "local-code");
        configuration.put("justauth.type.GITEE.local-user-id", "local-user");

        SocialProperties properties = SocialAutoConfig.bind(configuration);

        assertTrue(properties.getEnabled());
        assertEquals(1, properties.getType().size());
        SocialLoginConfigProperties gitee = properties.getType().get("gitee");
        assertEquals("local-client", gitee.getClientId());
        assertEquals("local-secret", gitee.getClientSecret());
        assertEquals("http://127.0.0.1/social-callback?source=gitee", gitee.getRedirectUri());
        assertEquals("local-code", gitee.getLocalCode());
        assertEquals("local-user", gitee.getLocalUserId());
    }
}
