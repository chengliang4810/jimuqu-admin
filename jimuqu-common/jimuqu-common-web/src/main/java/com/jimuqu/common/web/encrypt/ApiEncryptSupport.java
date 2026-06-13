package com.jimuqu.common.web.encrypt;

import cn.hutool.v7.core.text.StrUtil;
import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.domain.RsaKeyPair;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Context;

/**
 * 接口加密注解解析工具。
 *
 * @author chengliang
 */
public class ApiEncryptSupport {

    private static final String JSON = "json";
    private static final String PUBLIC_KEY_CONFIG = "api.encrypt.public-key";
    private static final String PRIVATE_KEY_CONFIG = "api.encrypt.private-key";
    private static final RsaKeyPair DEFAULT_KEY_PAIR = ApiCryptoUtil.generateRsaKeyPair();

    private ApiEncryptSupport() {
    }

    public static ApiEncrypt findAnnotation(Action action) {
        if (action == null) {
            return null;
        }
        ApiEncrypt apiEncrypt = action.method().getAnnotation(ApiEncrypt.class);
        if (apiEncrypt != null) {
            return apiEncrypt;
        }
        if (action.controller() != null) {
            apiEncrypt = action.controller().annotationGet(ApiEncrypt.class);
            if (apiEncrypt != null) {
                return apiEncrypt;
            }
        }
        return action.method().getDeclaringClz().getAnnotation(ApiEncrypt.class);
    }

    public static boolean isJsonRequest(Context ctx) {
        if (ctx == null || ctx.isMultipart() || ctx.isFormUrlencoded()) {
            return false;
        }
        String contentType = ctx.contentType();
        return StrUtil.isNotBlank(contentType) && contentType.toLowerCase().contains(JSON);
    }

    public static String resolvePublicKey(ApiEncrypt apiEncrypt) {
        if (apiEncrypt != null && StrUtil.isNotBlank(apiEncrypt.publicKey())) {
            return apiEncrypt.publicKey();
        }
        return StrUtil.defaultIfBlank(Solon.cfg().get(PUBLIC_KEY_CONFIG), DEFAULT_KEY_PAIR.getPublicKey());
    }

    public static String resolvePrivateKey(ApiEncrypt apiEncrypt) {
        if (apiEncrypt != null && StrUtil.isNotBlank(apiEncrypt.privateKey())) {
            return apiEncrypt.privateKey();
        }
        return StrUtil.defaultIfBlank(Solon.cfg().get(PRIVATE_KEY_CONFIG), DEFAULT_KEY_PAIR.getPrivateKey());
    }

    public static RsaKeyPair currentKeyPair() {
        return new RsaKeyPair(resolvePublicKey(null), resolvePrivateKey(null));
    }
}
