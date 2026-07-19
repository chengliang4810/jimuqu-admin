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
        var config = Solon.cfg();
        return config != null && config.getBool("api-decrypt.enabled", true);
    }

    public static String headerFlag() {
        var config = Solon.cfg();
        return config == null ? "encrypt-key" : config.get("api-decrypt.headerFlag", "encrypt-key");
    }

    public static String publicKey() {
        var config = Solon.cfg();
        return config == null ? null : config.get("api-decrypt.publicKey");
    }

    public static String privateKey() {
        var config = Solon.cfg();
        return config == null ? null : config.get("api-decrypt.privateKey");
    }
}
