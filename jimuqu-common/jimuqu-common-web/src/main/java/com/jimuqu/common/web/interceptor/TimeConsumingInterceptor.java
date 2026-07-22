package com.jimuqu.common.web.interceptor;

import com.jimuqu.common.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 耗时拦截器
 *
 * @author chengliang
 * @date 2025/12/14
 */
@Slf4j
@Component(index = -99)
public class TimeConsumingInterceptor implements RouterInterceptor {

    private static final int MAX_PARAM_LOG_LENGTH = 4000;

    /**
     * 排除敏感属性字段
     */
    private static final Set<String> EXCLUDE_PROPERTIES = Set.of(
            "password", "oldpassword", "newpassword", "confirmpassword", "authorization", "clientid");

    /**
     * 执行拦截
     */
    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {

        Action action = mainHandler instanceof Action handlerAction ? handlerAction : ctx.action();
        if (action == null){
            chain.doIntercept(ctx, mainHandler);
            return;
        }

        String url = ctx.method() + " " + ctx.path();
        Map<String, List<String>> params = sanitizeParams(ctx.paramMap().toValuesMap());
        String body = sanitizeRequestBody(ctx.contentType(), ctx.bodyNew());
        log.info("[PLUS]开始请求 => URL[{}],参数:[{}],Body:[{}]",
                url, limit(JsonUtil.toString(params)), limit(body));

        // 开始计时
        long startTime = System.currentTimeMillis();
        try {
            chain.doIntercept(ctx, mainHandler);
        }finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[PLUS]结束请求 => URL[{}],耗时:[{}]毫秒", url, executionTime);
        }
    }

    static Map<String, List<String>> sanitizeParams(Map<String, List<String>> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (!isExcluded(key)) {
                result.put(key, value);
            }
        });
        return result;
    }

    static String sanitizeRequestBody(String contentType, String body) {
        if (StrUtil.isBlank(body) || !StrUtil.startWithIgnoreCase(contentType, "application/json")) {
            return "";
        }
        try {
            Object value = JsonUtil.toObject(body, Object.class);
            removeExcludedFields(value);
            return JsonUtil.toString(value);
        } catch (RuntimeException e) {
            return "[无法解析的 JSON 请求体]";
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeExcludedFields(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> values = (Map<String, Object>) map;
            values.keySet().removeIf(TimeConsumingInterceptor::isExcluded);
            values.values().forEach(TimeConsumingInterceptor::removeExcludedFields);
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(TimeConsumingInterceptor::removeExcludedFields);
        }
    }

    private static boolean isExcluded(String key) {
        return key != null && EXCLUDE_PROPERTIES.contains(key.toLowerCase(Locale.ROOT));
    }

    private static String limit(String value) {
        return value != null && value.length() > MAX_PARAM_LOG_LENGTH
                ? value.substring(0, MAX_PARAM_LOG_LENGTH)
                : value;
    }

}
