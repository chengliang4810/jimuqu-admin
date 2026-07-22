package me.zhyd.oauth.request;

import com.alibaba.fastjson2.JSONObject;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JustAuthCompatibilityTest {

    @Test
    void resolvesAllWeChatEnterpriseUserIdBranches() {
        assertEquals("lower", resolve("{\"userid\":\"lower\"}"));
        assertEquals("upper", resolve("{\"UserId\":\"upper\"}"));
        assertNull(resolve("{\"OpenId\":\"external-user\"}"));
    }

    @Test
    void dingtalkAuthorizeDoesNotPreEncodeRedirectUri() {
        AuthConfig config = AuthConfig.builder()
            .clientId("client")
            .clientSecret("secret")
            .redirectUri("https://admin.example.test/social-callback?source=dingtalk")
            .build();
        String url = new AuthDingTalkV2Request(config, new MapStateCache()).authorize("state");

        assertTrue(url.contains("redirect_uri=https://admin.example.test/social-callback?source=dingtalk&prompt=consent"), url);
        assertFalse(url.contains("redirect_uri=https%3A"));
        assertTrue(url.contains("state=state"), url);
    }

    private static String resolve(String json) {
        return AbstractAuthWeChatEnterpriseRequest.resolveUserId(JSONObject.parseObject(json));
    }

    private static final class MapStateCache implements AuthStateCache {
        private final Map<String, String> states = new HashMap<>();

        @Override
        public void cache(String key, String value) {
            states.put(key, value);
        }

        @Override
        public void cache(String key, String value, long timeout) {
            states.put(key, value);
        }

        @Override
        public String get(String key) {
            return states.get(key);
        }

        @Override
        public boolean containsKey(String key) {
            return states.containsKey(key);
        }
    }
}
