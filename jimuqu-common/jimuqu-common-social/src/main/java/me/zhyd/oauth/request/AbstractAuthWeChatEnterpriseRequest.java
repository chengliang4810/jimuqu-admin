package me.zhyd.oauth.request;

import com.alibaba.fastjson2.JSONObject;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.constant.Keys;
import me.zhyd.oauth.enums.AuthResponseStatus;
import me.zhyd.oauth.enums.AuthUserGender;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.utils.HttpUtils;
import me.zhyd.oauth.utils.StringUtils;
import me.zhyd.oauth.utils.UrlBuilder;

/** 企业微信登录父类。 */
public abstract class AbstractAuthWeChatEnterpriseRequest extends AuthDefaultRequest {

    public AbstractAuthWeChatEnterpriseRequest(AuthConfig config, AuthSource source) {
        super(config, source);
    }

    public AbstractAuthWeChatEnterpriseRequest(AuthConfig config, AuthSource source, AuthStateCache authStateCache) {
        super(config, source, authStateCache);
    }

    @Override
    public AuthToken getAccessToken(AuthCallback authCallback) {
        String response = doGetAuthorizationCode(accessTokenUrl(null));
        JSONObject object = checkResponse(response);
        return AuthToken.builder()
            .accessToken(object.getString(Keys.OAUTH2_ACCESS_TOKEN))
            .expireIn(object.getIntValue(Keys.OAUTH2_EXPIRES_IN))
            .code(authCallback.getCode())
            .build();
    }

    @Override
    public AuthUser getUserInfo(AuthToken authToken) {
        JSONObject object = checkResponse(doGetUserInfo(authToken));
        String userId = resolveUserId(object);
        if (userId == null) {
            throw new AuthException(AuthResponseStatus.UNIDENTIFIED_PLATFORM, source);
        }
        String userTicket = object.getString("user_ticket");
        JSONObject userDetail = getUserDetail(authToken.getAccessToken(), userId, userTicket);

        return AuthUser.builder()
            .rawUserInfo(userDetail)
            .username(userDetail.getString(Keys.NAME))
            .nickname(userDetail.getString("alias"))
            .avatar(userDetail.getString(Keys.AVATAR))
            .location(userDetail.getString(Keys.OAUTH2_SCOPE__ADDRESS))
            .email(userDetail.getString(Keys.OAUTH2_SCOPE__EMAIL))
            .uuid(userId)
            .gender(AuthUserGender.getWechatRealGender(userDetail.getString(Keys.GENDER)))
            .token(authToken)
            .source(source.toString())
            .build();
    }

    static String resolveUserId(JSONObject object) {
        if (object.containsKey("userid")) {
            return object.getString("userid");
        }
        if (object.containsKey("UserId")) {
            return object.getString("UserId");
        }
        return null;
    }

    private JSONObject checkResponse(String response) {
        JSONObject object = JSONObject.parseObject(response);
        if (object.containsKey(Keys.VARIANT__ERRCODE) && object.getIntValue(Keys.VARIANT__ERRCODE) != 0) {
            throw new AuthException(object.getString(Keys.VARIANT__ERRMSG), source);
        }
        return object;
    }

    @Override
    protected String accessTokenUrl(String code) {
        return UrlBuilder.fromBaseUrl(source.accessToken())
            .queryParam("corpid", config.getClientId())
            .queryParam("corpsecret", config.getClientSecret())
            .build();
    }

    @Override
    protected String userInfoUrl(AuthToken authToken) {
        return UrlBuilder.fromBaseUrl(source.userInfo())
            .queryParam(Keys.OAUTH2_ACCESS_TOKEN, authToken.getAccessToken())
            .queryParam(Keys.OAUTH2_CODE, authToken.getCode())
            .build();
    }

    private JSONObject getUserDetail(String accessToken, String userId, String userTicket) {
        String userInfoUrl = UrlBuilder.fromBaseUrl("https://qyapi.weixin.qq.com/cgi-bin/user/get")
            .queryParam(Keys.OAUTH2_ACCESS_TOKEN, accessToken)
            .queryParam(Keys.USERID, userId)
            .build();
        String userInfoResponse = new HttpUtils(config.getHttpConfig()).get(userInfoUrl).getBody();
        JSONObject userInfo = checkResponse(userInfoResponse);

        if (StringUtils.isNotEmpty(userTicket)) {
            String userDetailUrl = UrlBuilder.fromBaseUrl("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail")
                .queryParam(Keys.OAUTH2_ACCESS_TOKEN, accessToken)
                .build();
            JSONObject param = new JSONObject();
            param.put("user_ticket", userTicket);
            String userDetailResponse = new HttpUtils(config.getHttpConfig())
                .post(userDetailUrl, param.toJSONString()).getBody();
            userInfo.putAll(checkResponse(userDetailResponse));
        }
        return userInfo;
    }
}
