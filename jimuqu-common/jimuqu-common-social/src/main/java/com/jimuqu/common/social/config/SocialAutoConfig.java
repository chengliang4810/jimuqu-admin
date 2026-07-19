package com.jimuqu.common.social.config;

import com.jimuqu.common.social.config.properties.SocialLoginConfigProperties;
import com.jimuqu.common.social.config.properties.SocialProperties;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.core.Props;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Social 登录配置。 */
@Configuration
public class SocialAutoConfig {

    @Bean(typed = true)
    public SocialProperties socialProperties() {
        return bind(Solon.cfg());
    }

    static SocialProperties bind(Props properties) {
        SocialProperties socialProperties = new SocialProperties();
        socialProperties.setEnabled(properties.getBool("justauth.enabled", false));

        Map<String, SocialLoginConfigProperties> providers = new LinkedHashMap<>();
        properties.getGroupedProp("justauth.type").forEach((source, providerProperties) ->
                providers.put(source.toLowerCase(Locale.ROOT),
                        providerProperties.toBean(SocialLoginConfigProperties.class)));
        socialProperties.setType(providers);
        return socialProperties;
    }
}
