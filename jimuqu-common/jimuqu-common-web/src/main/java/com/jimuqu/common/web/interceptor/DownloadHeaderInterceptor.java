package com.jimuqu.common.web.interceptor;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 为所有下载统一补齐 Bell 可读取的跨域文件名响应头。 */
@Component
public class DownloadHeaderInterceptor implements RouterInterceptor {

    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
        chain.doIntercept(ctx, mainHandler);
    }

    @Override
    public Object postResult(Context ctx, Object result) throws Exception {
        if (result instanceof DownloadedFile file && file.getName() != null) {
            String encodedName = percentEncode(file.getName());
            ctx.headerAdd("Access-Control-Expose-Headers", "Content-Disposition,download-filename");
            ctx.headerSet("Content-Disposition", (file.isAttachment() ? "attachment; " : "")
                    + "filename=" + encodedName + ";filename*=utf-8''" + encodedName);
            ctx.headerSet("download-filename", encodedName);

            // Solon 的文件渲染器会重写 Content-Disposition。移除渲染文件名，保留上面的 Bell 头。
            return new DownloadedFile(file.getContentType(), file.getContentSize(), file::getContent, null)
                    .asAttachment(file.isAttachment())
                    .cacheControl(file.getMaxAgeSeconds())
                    .eTag(file.getETag())
                    .lastModified(file.getLastModified());
        }
        return result;
    }

    static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
