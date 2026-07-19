package com.jimuqu.common.social.topiam;

import cn.hutool.v7.core.map.Dict;
import com.jimuqu.common.core.utils.JsonUtil;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.utils.UrlBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** TopIAM OIDC 认证请求。 */
public class AuthTopIamRequest extends AuthDefaultRequest {

    public AuthTopIamRequest(AuthConfig config, AuthStateCache stateCache, String serverUrl) {
        super(config, source(serverUrl), stateCache);
    }

    @Override
    public AuthToken getAccessToken(AuthCallback callback) {
        Dict body = JsonUtil.toMap(postToken(callback.getCode()));
        checkResponse(body);
        return AuthToken.builder()
                .accessToken(body.getStr("access_token"))
                .refreshToken(body.getStr("refresh_token"))
                .idToken(body.getStr("id_token"))
                .tokenType(body.getStr("token_type"))
                .scope(body.getStr("scope"))
                .build();
    }

    @Override
    public AuthUser getUserInfo(AuthToken token) {
        Dict body = JsonUtil.toMap(send(HttpRequest.newBuilder(URI.create(source.userInfo()))
                .header("Authorization", "Bearer " + token.getAccessToken())
                .GET().build()));
        checkResponse(body);
        return AuthUser.builder()
                .uuid(body.getStr("sub"))
                .username(body.getStr("preferred_username"))
                .nickname(body.getStr("nickname"))
                .avatar(body.getStr("picture"))
                .email(body.getStr("email"))
                .token(token)
                .source(source.getName())
                .build();
    }

    @Override
    public String authorize(String state) {
        UrlBuilder url = UrlBuilder.fromBaseUrl(super.authorize(state));
        if (config.getScopes() != null && !config.getScopes().isEmpty()) {
            url.queryParam("scope", String.join(" ", config.getScopes()));
        }
        return url.build();
    }

    private String postToken(String code) {
        String form = "grant_type=authorization_code&code=" + encode(code)
                + "&redirect_uri=" + encode(config.getRedirectUri());
        String credentials = Base64.getEncoder().encodeToString(
                (config.getClientId() + ":" + config.getClientSecret()).getBytes(StandardCharsets.UTF_8));
        return send(HttpRequest.newBuilder(URI.create(source.accessToken()))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build());
    }

    private static String send(HttpRequest request) {
        try {
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthException(e.getMessage());
        } catch (Exception e) {
            throw new AuthException(e.getMessage());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void checkResponse(Dict body) {
        if (body.containsKey("error")) {
            throw new AuthException(body.getStr("error_description"));
        }
        if (body.containsKey("message")) {
            throw new AuthException(body.getStr("message"));
        }
    }

    static AuthSource source(String serverUrl) {
        String base = serverUrl == null ? "" : serverUrl.replaceAll("/+$", "");
        return new AuthSource() {
            public String authorize() { return base + "/oauth2/auth"; }
            public String accessToken() { return base + "/oauth2/token"; }
            public String userInfo() { return base + "/oauth2/userinfo"; }
            public Class<? extends AuthDefaultRequest> getTargetClass() { return AuthTopIamRequest.class; }
            public String getName() { return "TOPIAM"; }
        };
    }
}
