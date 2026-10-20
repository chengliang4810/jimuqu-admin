package com.jimuqu.common.web.filter;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

/** 在表单参数解析前修正 FeatHTTP 空请求流的 EOF 语义。 */
@Component(index = -120)
public class FeatHttpEmptyBodyFilter implements Filter {
    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        if (ctx.request() instanceof HttpEndpoint endpoint
                && ctx.contentLength() <= 0 && ctx.header("Transfer-Encoding") == null) {
            endpoint.setInputStream(BodyInputStream.EMPTY_INPUT_STREAM);
            ctx.bodyNew("");
        }
        chain.doFilter(ctx);
    }
}
