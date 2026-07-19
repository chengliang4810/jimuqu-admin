package com.jimuqu.common.web.filter;

import com.jimuqu.common.core.utils.JsonUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 将 Bell 的 params[key] 查询参数聚合为可绑定的 Map。 */
@Component(index = -110)
public class BracketedParamsFilter implements Filter {

    private static final Pattern BRACKETED_PARAM = Pattern.compile("^params\\[([^\\[\\]]+)]$");

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        if (ctx.param("params") == null) {
            Map<String, String> params = collect(ctx);
            if (!params.isEmpty()) {
                ctx.paramMap().put("params", JsonUtil.toString(params));
            }
        }
        chain.doFilter(ctx);
    }

    static Map<String, String> collect(Context ctx) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String name : new ArrayList<>(ctx.paramNames())) {
            Matcher matcher = BRACKETED_PARAM.matcher(name);
            if (matcher.matches()) {
                params.put(matcher.group(1), ctx.param(name));
            }
        }
        return params;
    }
}
