package com.jimuqu.auth.config;

import com.jimuqu.auth.config.properties.MiniProgramProperties;
import com.jimuqu.auth.service.MiniProgramIdentityAdapter;
import com.jimuqu.auth.service.impl.WechatMiniProgramIdentityAdapter;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.core.Props;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序身份服务配置。
 */
@Configuration
public class MiniProgramAutoConfig {

    @Bean(typed = true)
    public MiniProgramProperties miniProgramProperties() {
        return bind(Solon.cfg());
    }

    @Bean
    @Condition(onMissingBean = MiniProgramIdentityAdapter.class)
    public MiniProgramIdentityAdapter miniProgramIdentityAdapter(MiniProgramProperties properties) {
        return new WechatMiniProgramIdentityAdapter(properties);
    }

    static MiniProgramProperties bind(Props configuration) {
        MiniProgramProperties properties = new MiniProgramProperties();
        properties.setEnabled(configuration.getBool("auth.mini-program.enabled", false));
        properties.setEndpoint(configuration.get(
                "auth.mini-program.endpoint", MiniProgramProperties.DEFAULT_ENDPOINT));
        Map<String, MiniProgramProperties.App> apps = new LinkedHashMap<>();
        configuration.getGroupedProp("auth.mini-program.apps").forEach((name, appProperties) ->
                apps.put(name, appProperties.toBean(MiniProgramProperties.App.class)));
        properties.setApps(apps);
        return properties;
    }
}
