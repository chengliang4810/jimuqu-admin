package com.jimuqu.common.web.config.properties;

import lombok.Data;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

import java.util.ArrayList;
import java.util.List;

/** 跨域配置属性。 */
@Data
@Configuration
@Inject(value = "${web.cors}", required = false)
public class CorsProperties {

    private boolean allowCredentials = true;

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));

    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    private List<String> allowedMethods = new ArrayList<>(List.of("*"));

    private int maxAge = 1800;
}
