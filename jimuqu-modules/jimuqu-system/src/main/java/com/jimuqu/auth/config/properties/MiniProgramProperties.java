package com.jimuqu.auth.config.properties;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信小程序登录配置。
 */
@Data
public class MiniProgramProperties {

    public static final String DEFAULT_ENDPOINT = "https://api.weixin.qq.com/sns/jscode2session";

    private boolean enabled;
    private String endpoint = DEFAULT_ENDPOINT;
    private Map<String, App> apps = new LinkedHashMap<>();

    @Data
    public static class App {
        private String appid;
        private String secret;
    }
}
