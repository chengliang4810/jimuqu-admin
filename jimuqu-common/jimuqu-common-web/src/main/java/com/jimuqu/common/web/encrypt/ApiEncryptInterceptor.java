package com.jimuqu.common.web.encrypt;

import cn.hutool.v7.core.text.StrUtil;
import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.domain.ApiEncryptPayload;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.JsonUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

/**
 * {@link ApiEncrypt} 请求解密拦截器。
 *
 * @author chengliang
 */
@Component(index = -100)
public class ApiEncryptInterceptor implements RouterInterceptor {

    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
        ApiEncrypt apiEncrypt = ApiEncryptSupport.findAnnotation(ctx.action());
        if (apiEncrypt == null || !apiEncrypt.request() || !ApiEncryptSupport.isJsonRequest(ctx)) {
            chain.doIntercept(ctx, mainHandler);
            return;
        }

        String body = ctx.bodyNew();
        if (StrUtil.isBlank(body)) {
            chain.doIntercept(ctx, mainHandler);
            return;
        }

        ApiEncryptPayload payload = parsePayload(body, apiEncrypt.required());
        if (payload != null) {
            String privateKey = ApiEncryptSupport.resolvePrivateKey(apiEncrypt);
            ctx.bodyNew(ApiCryptoUtil.decrypt(payload, privateKey));
        }

        chain.doIntercept(ctx, mainHandler);
    }

    private ApiEncryptPayload parsePayload(String body, boolean required) {
        try {
            ApiEncryptPayload payload = JsonUtil.toObject(body, ApiEncryptPayload.class);
            if (payload == null || StrUtil.hasBlank(payload.getEncryptKey(), payload.getIv(), payload.getData())) {
                if (required) {
                    throw new ServiceException("接口加密参数不完整");
                }
                return null;
            }
            return payload;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            if (required) {
                throw new ServiceException("接口加密参数格式错误: " + e.getMessage());
            }
            return null;
        }
    }
}
