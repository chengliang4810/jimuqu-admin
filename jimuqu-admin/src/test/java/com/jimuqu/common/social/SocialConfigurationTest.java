package com.jimuqu.common.social;

import com.jimuqu.common.social.config.properties.SocialLoginConfigProperties;
import com.jimuqu.common.social.utils.SocialUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialConfigurationTest {

    @Test
    void validatesProviderSpecificRequiredConfiguration() {
        SocialLoginConfigProperties config = new SocialLoginConfigProperties();
        config.setClientId("client");
        config.setClientSecret("secret");
        config.setRedirectUri("https://admin.example.test/social-callback?source=github");

        assertTrue(SocialUtils.isConfigured("github", config));
        assertFalse(SocialUtils.isConfigured("gitea", config));
        config.setServerUrl("https://git.example.test");
        assertTrue(SocialUtils.isConfigured("gitea", config));
        config.setClientSecret(" ");
        assertFalse(SocialUtils.isConfigured("github", config));
    }

    @Test
    void appConfigKeepsBellProvidersEnvironmentBacked() throws IOException {
        String yaml;
        try (var input = getClass().getClassLoader().getResourceAsStream("app.yml")) {
            yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String source : new String[]{"gitee", "github", "maxkey", "topiam", "gitea", "wechat"}) {
            assertTrue(yaml.contains("    " + source + ":"), source);
        }
        assertTrue(yaml.contains("JIMU_JUSTAUTH_ENABLED"));
        assertTrue(yaml.contains("JIMU_JUSTAUTH_GITEA_SERVER_URL"));
        assertFalse(yaml.contains("client-secret: 1f7d08"));
    }
}
