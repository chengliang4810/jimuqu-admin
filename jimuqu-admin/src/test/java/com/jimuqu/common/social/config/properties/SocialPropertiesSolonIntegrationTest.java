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

/**
 * 验证 Solon 运行时能够完整绑定社交登录配置。
 */
@SolonTest(value = Application.class, env = "test", debug = false)
public class SocialPropertiesSolonIntegrationTest {

    /**
     * 运行时社交登录配置。
     */
    @Inject
    private SocialProperties socialProperties;

    /**
     * 验证 Gitee 测试提供器配置及回调地址环境变量覆盖。
     */
    @Test
    void bindsJustAuthProviderIntoTheRuntimeBean() {
        assertTrue(Solon.cfg().getBool("justauth.enabled", false));
        assertSame(socialProperties, Solon.context().getBean(SocialProperties.class));
        assertTrue(socialProperties.getEnabled());

        SocialLoginConfigProperties gitee = socialProperties.getType().get("gitee");
        assertNotNull(gitee);
        assertEquals("http-contract-client", gitee.getClientId());
        assertEquals("http-contract-secret", gitee.getClientSecret());
        assertEquals(System.getenv().getOrDefault(
                        "JIMU_JUSTAUTH_GITEE_REDIRECT_URI",
                        "http://127.0.0.1:15555/social-callback?source=gitee"),
                gitee.getRedirectUri());
        assertEquals("http-contract-code", gitee.getLocalCode());
        assertEquals("http-contract-user", gitee.getLocalUserId());
    }
}
