package com.jimuqu.common.social.config.properties;

import lombok.Data;

import java.util.List;

/**
 * 社交登录配置
 *
 * @author thiszhc
 */
@Data
public class SocialLoginConfigProperties {

    /**
     * 应用 ID
     */
    private String clientId;

    /**
     * 应用密钥
     */
    private String clientSecret;

    /**
     * 回调地址
     */
    private String redirectUri;

    /** Microsoft Entra ID 租户。 */
    private String tenantId;

    /** OAuth 请求范围。 */
    private List<String> scopes;

    /**
     * 是否获取unionId
     */
    private boolean unionId;

    /**
     * Coding 企业名称
     */
    private String codingGroupName;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 企业微信应用ID
     */
    private String agentId;

    /**
     * stackoverflow api key
     */
    private String stackOverflowKey;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 客户端系统类型
     */
    private String clientOsType;

    /**
     * 本地 OAuth 适配器授权码。仅在显式配置时启用，用于无外网的集成测试。
     */
    private String localCode;

    /**
     * 本地 OAuth 适配器返回的用户唯一标识。
     */
    private String localUserId;

    /**
     * 本地 OAuth 适配器返回的用户名。
     */
    private String localUsername;

    /**
     * 本地 OAuth 适配器返回的昵称。
     */
    private String localNickname;

    /**
     * 本地 OAuth 适配器返回的邮箱。
     */
    private String localEmail;

    /**
     * maxkey 服务器地址
     */
    private String serverUrl;

}
