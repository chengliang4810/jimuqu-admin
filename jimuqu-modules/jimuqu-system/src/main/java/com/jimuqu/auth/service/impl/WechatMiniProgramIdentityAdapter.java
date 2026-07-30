package com.jimuqu.auth.service.impl;

import cn.hutool.core.lang.Dict;
import com.jimuqu.auth.config.properties.MiniProgramProperties;
import com.jimuqu.auth.service.MiniProgramIdentityAdapter;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 微信 code2session 身份适配器。
 */
public class WechatMiniProgramIdentityAdapter implements MiniProgramIdentityAdapter {

    private final MiniProgramProperties properties;
    private final HttpClient httpClient;

    public WechatMiniProgramIdentityAdapter(MiniProgramProperties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    WechatMiniProgramIdentityAdapter(MiniProgramProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && StringUtil.isNotBlank(properties.getEndpoint())
                && !configuredApps().isEmpty();
    }

    @Override
    public MiniProgramIdentity authenticate(String appId, String code) {
        if (!isAvailable()) {
            throw new ServiceException("小程序登录未启用，请先配置小程序身份服务");
        }
        if (StringUtil.isBlank(code)) {
            throw new ServiceException("小程序登录码不能为空");
        }
        MiniProgramProperties.App app = resolveApp(appId);
        HttpRequest request = HttpRequest.newBuilder(code2SessionUri(app, code))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException("小程序身份服务请求失败，HTTP " + response.statusCode());
            }
            return parseIdentity(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceException("小程序身份服务请求被中断");
        } catch (IOException exception) {
            throw new ServiceException("小程序身份服务请求失败");
        }
    }

    private MiniProgramIdentity parseIdentity(String body) {
        Dict response = JsonUtil.toMap(body);
        Integer errorCode = response.getInt("errcode");
        if (errorCode != null && errorCode != 0) {
            String errorMessage = StringUtil.defaultIfBlank(response.getStr("errmsg"), "未知错误");
            throw new ServiceException("小程序身份校验失败：" + errorMessage);
        }
        String openId = response.getStr("openid");
        if (StringUtil.isBlank(openId)) {
            throw new ServiceException("小程序身份服务未返回 openid");
        }
        return new MiniProgramIdentity(openId, response.getStr("unionid"));
    }

    private MiniProgramProperties.App resolveApp(String requestedAppId) {
        List<MiniProgramProperties.App> apps = configuredApps();
        if (StringUtil.isBlank(requestedAppId)) {
            if (apps.size() == 1) {
                return apps.getFirst();
            }
            throw new ServiceException("存在多个小程序配置时 appid 不能为空");
        }
        return apps.stream()
                .filter(app -> requestedAppId.equals(app.getAppid()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("小程序 appid 未配置"));
    }

    private List<MiniProgramProperties.App> configuredApps() {
        if (properties.getApps() == null) {
            return List.of();
        }
        return properties.getApps().values().stream()
                .filter(app -> app != null
                        && StringUtil.isNotBlank(app.getAppid())
                        && StringUtil.isNotBlank(app.getSecret()))
                .toList();
    }

    private URI code2SessionUri(MiniProgramProperties.App app, String code) {
        String separator = properties.getEndpoint().contains("?") ? "&" : "?";
        return URI.create(properties.getEndpoint() + separator
                + "appid=" + encode(app.getAppid())
                + "&secret=" + encode(app.getSecret())
                + "&js_code=" + encode(code)
                + "&grant_type=authorization_code");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
