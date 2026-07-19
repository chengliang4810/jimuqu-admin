package com.jimuqu.common.social.local;

import com.jimuqu.common.social.config.properties.SocialLoginConfigProperties;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.enums.AuthResponseStatus;
import me.zhyd.oauth.enums.AuthUserGender;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.utils.UrlBuilder;

import java.util.Objects;

/**
 * 可配置的本地 OAuth 提供器，用于在无外部平台时验证完整社交授权链路。
 */
public final class AuthLocalRequest extends AuthDefaultRequest {

    private final String sourceName;
    private final SocialLoginConfigProperties properties;

    public AuthLocalRequest(AuthConfig config, AuthStateCache stateCache, String sourceName,
                            SocialLoginConfigProperties properties) {
        super(config, new LocalAuthSource(sourceName), stateCache);
        this.sourceName = sourceName;
        this.properties = properties;
    }

    @Override
    public String authorize(String state) {
        return UrlBuilder.fromBaseUrl(config.getRedirectUri())
                .queryParam("code", properties.getLocalCode())
                .queryParam("state", getRealState(state))
                .build();
    }

    @Override
    public AuthToken getAccessToken(AuthCallback callback) {
        if (!Objects.equals(properties.getLocalCode(), callback.getCode())) {
            throw new AuthException(AuthResponseStatus.ILLEGAL_CODE, source);
        }
        return AuthToken.builder()
                .accessToken("local-" + sourceName + '-' + properties.getLocalUserId())
                .expireIn(300)
                .uid(properties.getLocalUserId())
                .openId(properties.getLocalUserId())
                .build();
    }

    @Override
    public AuthUser getUserInfo(AuthToken token) {
        return AuthUser.builder()
                .uuid(properties.getLocalUserId())
                .username(properties.getLocalUsername())
                .nickname(properties.getLocalNickname())
                .email(properties.getLocalEmail())
                .gender(AuthUserGender.UNKNOWN)
                .source(sourceName)
                .token(token)
                .build();
    }

    private record LocalAuthSource(String name) implements AuthSource {

        @Override
        public String authorize() {
            return "http://localhost/local-oauth/authorize";
        }

        @Override
        public String accessToken() {
            return "http://localhost/local-oauth/token";
        }

        @Override
        public String userInfo() {
            return "http://localhost/local-oauth/user";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<? extends AuthDefaultRequest> getTargetClass() {
            return AuthLocalRequest.class;
        }
    }
}
