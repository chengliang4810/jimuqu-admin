package com.jimuqu.common.web.encrypt;

import cn.hutool.core.util.StrUtil;
import com.jimuqu.common.core.constant.HttpStatus;
import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.exception.ServiceException;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

/** 按 Bell 6.X encrypt-key 契约统一解密 POST/PUT 请求。 */
@Component(index = -100)
public class ApiEncryptInterceptor implements RouterInterceptor {

    @Init
    public void validateKeys() {
        if (ApiEncryptSupport.enabled()) {
            ApiCryptoUtil.validateRsaKeyPair(ApiEncryptSupport.publicKey(), ApiEncryptSupport.privateKey());
        }
    }

    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
        if (!ApiEncryptSupport.enabled()) {
            chain.doIntercept(ctx, mainHandler);
            return;
        }

        ApiEncrypt apiEncrypt = ApiEncryptSupport.findAnnotation(ctx.action());
        String method = ctx.method();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            String encryptedKey = ctx.header(ApiEncryptSupport.headerFlag());
            if (StrUtil.isNotBlank(encryptedKey)) {
                String body = ctx.bodyNew();
                ctx.bodyNew(ApiCryptoUtil.decryptRequest(body, encryptedKey, ApiEncryptSupport.privateKey()));
            } else if (apiEncrypt != null) {
                // 先消费请求体，避免 SmartHTTP 在输出 JSON 错误前重置连接。
                ctx.body();
                throw new ServiceException("没有访问权限，请联系管理员授权", HttpStatus.FORBIDDEN);
            }
        }

        chain.doIntercept(ctx, mainHandler);
    }
}
