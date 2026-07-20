package com.jimuqu.auth.service;

import me.zhyd.oauth.config.AuthDefaultSource;

/**
 * 小程序外部身份适配器。
 */
public interface MiniProgramIdentityAdapter {

    String WECHAT_MINI_PROGRAM_SOURCE = AuthDefaultSource.WECHAT_MINI_PROGRAM.toString();

    /**
     * 当前适配器是否已完成可用配置。
     */
    boolean isAvailable();

    /**
     * 使用小程序临时登录码换取稳定身份。
     *
     * @param appId 小程序应用 ID
     * @param code  临时登录码
     * @return 小程序身份
     */
    MiniProgramIdentity authenticate(String appId, String code);

    /**
     * @param userId  自定义适配器可直接返回已绑定的系统用户 ID
     * @param openId  小程序用户唯一标识
     * @param unionId 微信开放平台统一用户标识，可能为空
     */
    record MiniProgramIdentity(Long userId, String openId, String unionId) {

        public MiniProgramIdentity(String openId, String unionId) {
            this(null, openId, unionId);
        }
    }
}
