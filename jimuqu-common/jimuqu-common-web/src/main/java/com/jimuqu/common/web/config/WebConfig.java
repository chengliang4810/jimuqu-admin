package com.jimuqu.common.web.config;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.Solon;
import org.noear.solon.serialization.snack4.Snack4StringSerializer;
import org.noear.solon.web.cors.CrossInterceptor;
import com.jimuqu.common.web.sensitive.SensitiveJsonRender;

/**
 * web 通用配置
 *
 * @author chengliang
 * @since 2024/04/02
 */
@Configuration
public class WebConfig {

    /**
     * 跨域配置
     *
     * @return 跨域配置
     * @author chengliang
     * @since 2025/05/07
     */
    @Bean(index = -1)
    public CrossInterceptor crossInterceptor() {
        return new CrossInterceptor()
                // 设置访问源地址
                .allowedOrigins("*")
                // 设置访问源请求方法
                .allowedMethods("*")
                // 设置访问源请求头
                .allowedHeaders("*")
                // 是否支持用户凭据。
                .allowCredentials(true)
                // 有效期 1800秒
                .maxAge(3600);
    }

    /**
     * 替换默认 JSON 渲染器，统一处理 @Sensitive 响应字段。
     */
    @Bean
    public void sensitiveJsonRender(Snack4StringSerializer snack4StringSerializer) {
        Solon.app().renders().register("@json", new SensitiveJsonRender(snack4StringSerializer));
    }

}
