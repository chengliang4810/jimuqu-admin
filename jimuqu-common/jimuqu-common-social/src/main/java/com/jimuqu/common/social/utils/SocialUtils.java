package com.jimuqu.common.social.utils;

import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.social.config.properties.SocialLoginConfigProperties;
import com.jimuqu.common.social.config.properties.SocialProperties;
import com.jimuqu.common.social.gitea.AuthGiteaRequest;
import com.jimuqu.common.social.local.AuthLocalRequest;
import com.jimuqu.common.social.maxkey.AuthMaxKeyRequest;
import com.jimuqu.common.social.topiam.AuthTopIamRequest;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.*;
import org.noear.solon.Solon;

/**
 * 认证授权工具类
 *
 * @author thiszhc
 */
public class SocialUtils {

    private static volatile AuthRedisStateCache STATE_CACHE;

    @SuppressWarnings("unchecked" )
    public static AuthResponse<AuthUser> loginAuth(String source, String code, String state, SocialProperties socialProperties) throws AuthException {
        AuthRequest authRequest = getAuthRequest(source, socialProperties);
        AuthCallback callback = new AuthCallback();
        callback.setCode(code);
        callback.setState(state);
        return authRequest.login(callback);
    }

    public static AuthRequest getAuthRequest(String source, SocialProperties socialProperties) throws AuthException {
        Assert.isTrue(Boolean.TRUE.equals(socialProperties.getEnabled()), "第三方登录未启用" );
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
        Assert.notNull(obj, "不支持的第三方登录类型" );
        Assert.isTrue(isConfigured(source, obj), source + "平台配置不完整" );
        STATE_CACHE = Solon.context().getBean(AuthRedisStateCache.class);

        AuthConfig.AuthConfigBuilder builder = AuthConfig.builder()
                .clientId(obj.getClientId())
                .clientSecret(obj.getClientSecret())
                .redirectUri(obj.getRedirectUri())
                .scopes(obj.getScopes());
        if (!isBlank(obj.getLocalCode())) {
            Assert.isTrue(!isBlank(obj.getLocalUserId()), source + "本地平台用户标识不能为空");
            return new AuthLocalRequest(builder.build(), STATE_CACHE, source.toLowerCase(), obj);
        }
        return switch (source.toLowerCase()) {
            case "dingtalk" -> new AuthDingTalkV2Request(builder.build(), STATE_CACHE);
            case "baidu" -> new AuthBaiduRequest(builder.build(), STATE_CACHE);
            case "github" -> new AuthGithubRequest(builder.build(), STATE_CACHE);
            case "gitee" -> new AuthGiteeRequest(builder.build(), STATE_CACHE);
            case "weibo" -> new AuthWeiboRequest(builder.build(), STATE_CACHE);
            case "coding" -> new AuthCodingRequest(builder.build(), STATE_CACHE);
            case "oschina" -> new AuthOschinaRequest(builder.build(), STATE_CACHE);
            // 支付宝在创建回调地址时，不允许使用localhost或者127.0.0.1，所以这儿的回调地址使用的局域网内的ip
            case "alipay_wallet" ->
                    new AuthAlipayRequest(builder.build(), socialProperties.getType().get("alipay_wallet" ).getAlipayPublicKey(), STATE_CACHE);
            case "qq" -> new AuthQqRequest(builder.build(), STATE_CACHE);
            case "wechat", "wechat_open" -> new AuthWeChatOpenRequest(builder.build(), STATE_CACHE);
            case "taobao" -> new AuthTaobaoRequest(builder.build(), STATE_CACHE);
            case "douyin" -> new AuthDouyinRequest(builder.build(), STATE_CACHE);
            case "linkedin" -> new AuthLinkedinRequest(builder.build(), STATE_CACHE);
            case "microsoft" -> new AuthMicrosoftRequest(builder.tenantId(obj.getTenantId()).build(), STATE_CACHE);
            case "renren" -> new AuthRenrenRequest(builder.build(), STATE_CACHE);
            case "stack_overflow" -> new AuthStackOverflowRequest(builder.stackOverflowKey(obj.getStackOverflowKey()).build(), STATE_CACHE);
            case "huawei" -> new AuthHuaweiV3Request(builder.build(), STATE_CACHE);
            case "wechat_enterprise" ->
                    new AuthWeChatEnterpriseQrcodeV2Request(builder.agentId(obj.getAgentId()).build(), STATE_CACHE);
            case "gitlab" -> new AuthGitlabRequest(builder.build(), STATE_CACHE);
            case "wechat_mp" -> new AuthWeChatMpRequest(builder.build(), STATE_CACHE);
            case "aliyun" -> new AuthAliyunRequest(builder.build(), STATE_CACHE);
            case "maxkey" -> new AuthMaxKeyRequest(builder.build(), STATE_CACHE);
            case "topiam" -> new AuthTopIamRequest(builder.build(), STATE_CACHE, obj.getServerUrl());
            case "gitea" -> new AuthGiteaRequest(builder.build(), STATE_CACHE, obj.getServerUrl());
            default -> throw new AuthException("未获取到有效的Auth配置" );
        };
    }

    public static boolean isConfigured(String source, SocialLoginConfigProperties config) {
        if (config == null || isBlank(config.getClientId()) || isBlank(config.getClientSecret())
                || isBlank(config.getRedirectUri())) {
            return false;
        }
        String normalized = source == null ? "" : source.toLowerCase();
        return switch (normalized) {
            case "maxkey", "topiam", "gitea" -> !isBlank(config.getServerUrl());
            default -> true;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

