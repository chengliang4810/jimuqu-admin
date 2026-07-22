package com.jimuqu.common.web.filter;

import cn.hutool.core.util.StrUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.web.config.properties.XssProperties;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

import java.util.ArrayList;
import java.util.List;

/** 对解密后的写请求执行与 Bell 6.X 一致的 HTML 标签清洗。 */
@Component(index = -90)
public class XssInterceptor implements RouterInterceptor {

    @Inject
    private XssProperties properties;

    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
        if (properties.isEnabled() && isWrite(ctx) && !isExcluded(ctx.path())) {
            cleanParameters(ctx);
            if (StrUtil.startWithIgnoreCase(ctx.contentType(), "application/json")) {
                String body = ctx.bodyNew();
                if (StrUtil.isNotEmpty(body)) {
                    ctx.bodyNew(StringUtil.cleanHtmlTag(body).trim());
                }
            }
        }
        chain.doIntercept(ctx, mainHandler);
    }

    private boolean isWrite(Context ctx) {
        return "POST".equalsIgnoreCase(ctx.method()) || "PUT".equalsIgnoreCase(ctx.method());
    }

    private boolean isExcluded(String path) {
        return properties.getExcludeUrls().stream().anyMatch(pattern -> StringUtil.isMatch(pattern, path));
    }

    private void cleanParameters(Context ctx) {
        for (String name : new ArrayList<>(ctx.paramNames())) {
            List<String> values = ctx.paramMap().toValuesMap().get(name);
            if (values == null) {
                continue;
            }
            ctx.paramMap().remove(name);
            values.stream().map(StringUtil::cleanHtmlTag).map(String::trim)
                    .forEach(value -> ctx.paramMap().put(name, value));
        }
    }
}
