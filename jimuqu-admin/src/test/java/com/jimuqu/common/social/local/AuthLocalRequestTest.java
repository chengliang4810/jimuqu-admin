package com.jimuqu.common.social.local;

import com.jimuqu.common.social.config.properties.SocialLoginConfigProperties;
import me.zhyd.oauth.cache.AuthDefaultStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthLocalRequestTest {

    @Test
    void exercisesTheJustAuthStateAndCallbackContract() {
        SocialLoginConfigProperties properties = new SocialLoginConfigProperties();
        properties.setLocalCode("local-code");
        properties.setLocalUserId("local-user");
        properties.setLocalUsername("local_account");
        properties.setLocalNickname("本地账号");
        properties.setLocalEmail("local@jimuqu.test");

        AuthConfig config = AuthConfig.builder()
                .clientId("local-client")
                .clientSecret("local-secret")
                .redirectUri("http://127.0.0.1:15555/social-callback?source=gitee")
                .build();
        AuthLocalRequest request = new AuthLocalRequest(
                config, AuthDefaultStateCache.INSTANCE, "gitee", properties);

        String authorizeUrl = request.authorize("local-state-success");
        assertTrue(authorizeUrl.contains("source=gitee"));
        assertTrue(authorizeUrl.contains("code=local-code"));
        assertTrue(authorizeUrl.contains("state=local-state-success"));

        AuthCallback successCallback = new AuthCallback();
        successCallback.setCode("local-code");
        successCallback.setState("local-state-success");
        AuthResponse<AuthUser> success = request.login(successCallback);
        assertTrue(success.ok());
        assertEquals("gitee", success.getData().getSource());
        assertEquals("local-user", success.getData().getUuid());
        assertEquals("local_account", success.getData().getUsername());
        assertEquals("local@jimuqu.test", success.getData().getEmail());

        request.authorize("local-state-bad-code");
        AuthCallback badCodeCallback = new AuthCallback();
        badCodeCallback.setCode("wrong-code");
        badCodeCallback.setState("local-state-bad-code");
        assertFalse(request.login(badCodeCallback).ok());

        AuthCallback unknownStateCallback = new AuthCallback();
        unknownStateCallback.setCode("local-code");
        unknownStateCallback.setState("local-state-never-authorized");
        assertFalse(request.login(unknownStateCallback).ok());
    }
}
