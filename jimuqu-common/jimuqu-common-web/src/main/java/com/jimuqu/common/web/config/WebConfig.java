package com.jimuqu.common.web.config;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.Solon;
import org.noear.solon.core.convert.Converter;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptorChain;
import org.noear.snack4.Feature;
import org.noear.solon.serialization.snack4.Snack4StringSerializer;
import org.noear.solon.web.cors.CrossInterceptor;
import com.jimuqu.common.web.config.properties.CorsProperties;
import com.jimuqu.common.web.sensitive.SensitiveJsonRender;
import com.jimuqu.common.core.service.SensitiveService;
import com.jimuqu.common.core.utils.JsonNumberCodec;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.xss.Xss;
import com.jimuqu.common.core.xss.XssValidator;
import org.noear.solon.validation.ValidatorManager;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * web 通用配置
 *
 * @author chengliang
 * @since 2024/04/02
 */
@Configuration
public class WebConfig {

    @Bean
    public void xssValidator() {
        ValidatorManager.register(Xss.class, XssValidator.INSTANCE);
    }

    @Bean
    public void bracketedParamsConverter() {
        Solon.app().converters().register(new JsonMapConverter());
    }

    /**
     * 跨域配置
     *
     * @return 跨域配置
     * @author chengliang
     * @since 2025/05/07
     */
    @Bean(index = -200)
    public CrossInterceptor crossInterceptor(CorsProperties properties) {
        return new OriginPatternCrossInterceptor(properties.getAllowedOriginPatterns())
                // 设置访问源请求方法
                .allowedMethods(String.join(",", properties.getAllowedMethods()))
                // 设置访问源请求头
                .allowedHeaders(String.join(",", properties.getAllowedHeaders()))
                // 是否支持用户凭据。
                .allowCredentials(properties.isAllowCredentials())
                .maxAge(properties.getMaxAge());
    }

    /** Solon 原生 CORS 仅做字符串包含判断，这里保持 Spring allowedOriginPatterns 语义。 */
    static final class OriginPatternCrossInterceptor extends CrossInterceptor {

        private static final Pattern PORTS_PATTERN = Pattern.compile("(.*):\\[(\\*|\\d+(,\\d+)*)]");

        private final List<Pattern> allowedOriginPatterns;

        OriginPatternCrossInterceptor(List<String> patterns) {
            this.allowedOriginPatterns = patterns.stream()
                    .map(OriginPatternCrossInterceptor::compileOriginPattern)
                    .toList();
            allowedOrigins("");
        }

        @Override
        public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
            String origin = ctx.header("Origin");
            if (matchesOrigin(origin)) {
                ctx.headerSet("Access-Control-Allow-Origin", origin);
            }
            super.doIntercept(ctx, mainHandler, chain);
        }

        private boolean matchesOrigin(String origin) {
            if (origin == null || origin.isBlank()) {
                return false;
            }
            String value = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
            return allowedOriginPatterns.stream().anyMatch(pattern -> pattern.matcher(value).matches());
        }

        private static Pattern compileOriginPattern(String declaredPattern) {
            String patternValue = declaredPattern;
            String portList = null;
            Matcher matcher = PORTS_PATTERN.matcher(patternValue);
            if (matcher.matches()) {
                patternValue = matcher.group(1);
                portList = matcher.group(2);
            }

            patternValue = "\\Q" + patternValue + "\\E";
            patternValue = patternValue.replace("*", "\\E.*\\Q");
            if (portList != null) {
                patternValue += "*".equals(portList)
                        ? "(:\\d+)?"
                        : ":(" + portList.replace(',', '|') + ")";
            }
            return Pattern.compile(patternValue);
        }
    }

    /**
     * 替换默认 JSON 渲染器，统一处理 @Sensitive 响应字段。
     */
    @Bean
    public void sensitiveJsonRender(Snack4StringSerializer snack4StringSerializer,
                                    SensitiveService sensitiveService) {
        configureJsonSerializer(snack4StringSerializer);
        Solon.app().renders().register("@json", new SensitiveJsonRender(snack4StringSerializer, sensitiveService));
    }

    static void configureJsonSerializer(Snack4StringSerializer serializer) {
        serializer.getSerializeConfig().removeFeatures(
                Feature.Write_LongAsString,
                Feature.Write_NullListAsEmpty,
                Feature.Write_NullStringAsEmpty);
        serializer.getSerializeConfig().addFeatures(Feature.Write_Nulls);
        JsonNumberCodec.configure(serializer.getSerializeConfig().getOptions());
    }

    static final class JsonMapConverter implements Converter<String, Map> {

        @Override
        public Map convert(String source) {
            return JsonUtil.toObject(source, Map.class);
        }
    }

}
