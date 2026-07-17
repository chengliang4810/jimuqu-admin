package com.jimuqu.common.web.encrypt;

import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.Action;

/** Bell 6.X API 加密配置与注解解析。 */
public class ApiEncryptSupport {

    private ApiEncryptSupport() {
    }

    public static ApiEncrypt findAnnotation(Action action) {
        return action == null ? null : action.method().getAnnotation(ApiEncrypt.class);
    }

    public static boolean enabled() {
        return Solon.cfg().getBool("api-decrypt.enabled", true);
    }

    public static String headerFlag() {
        return Solon.cfg().get("api-decrypt.headerFlag", "encrypt-key");
    }

    public static String publicKey() {
        return Solon.cfg().get("api-decrypt.publicKey");
    }

    public static String privateKey() {
        return Solon.cfg().get("api-decrypt.privateKey");
    }
}
