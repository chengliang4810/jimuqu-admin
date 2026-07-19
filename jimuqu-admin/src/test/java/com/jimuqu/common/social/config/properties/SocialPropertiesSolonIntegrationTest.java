package com.jimuqu.common.social.config.properties;

import com.jimuqu.Application;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SolonTest(value = Application.class, env = "test", debug = false)
public class SocialPropertiesSolonIntegrationTest {

    @Inject
    private SocialProperties socialProperties;

    @Test
    void bindsJustAuthProviderIntoTheRuntimeBean() {
        assertTrue(Solon.cfg().getBool("justauth.enabled", false));
        assertSame(socialProperties, Solon.context().getBean(SocialProperties.class));
        assertTrue(socialProperties.getEnabled());

        SocialLoginConfigProperties gitee = socialProperties.getType().get("gitee");
        assertNotNull(gitee);
        assertEquals("http-contract-client", gitee.getClientId());
        assertEquals("http-contract-secret", gitee.getClientSecret());
        assertEquals("http://127.0.0.1:15555/social-callback?source=gitee", gitee.getRedirectUri());
        assertEquals("http-contract-code", gitee.getLocalCode());
        assertEquals("http-contract-user", gitee.getLocalUserId());
    }
}
