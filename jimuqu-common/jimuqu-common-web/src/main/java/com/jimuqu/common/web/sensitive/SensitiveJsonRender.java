package com.jimuqu.common.web.sensitive;

import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.sensitive.utils.SensitiveUtil;
import com.jimuqu.common.web.encrypt.ApiEncryptSupport;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Render;
import org.noear.solon.serialization.snack4.Snack4StringSerializer;

/**
 * JSON 响应脱敏和接口响应加密渲染器。
 *
 * @author chengliang
 */
public class SensitiveJsonRender implements Render {

    private final Snack4StringSerializer serializer;

    public SensitiveJsonRender(Snack4StringSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public String name() {
        return "sensitive-json";
    }

    @Override
    public String[] mappings() {
        return new String[]{"@json"};
    }

    @Override
    public boolean matched(Context ctx, String mime) {
        return serializer.matched(ctx, mime);
    }

    @Override
    public String renderAndReturn(Object obj, Context ctx) throws Throwable {
        Object body = SensitiveUtil.desensitizeObject(obj);
        String json = serializer.serialize(body);
        return encryptIfNecessary(json, ctx);
    }

    @Override
    public void render(Object obj, Context ctx) throws Throwable {
        if (ctx.contentTypeNew() == null) {
            ctx.contentType(serializer.mimeType());
        }
        ctx.output(renderAndReturn(obj, ctx));
    }

    private String encryptIfNecessary(String json, Context ctx) throws Exception {
        ApiEncrypt apiEncrypt = ApiEncryptSupport.findAnnotation(ctx.action());
        if (!ApiEncryptSupport.enabled() || apiEncrypt == null || !apiEncrypt.response()) {
            return json;
        }
        String aesKey = ApiCryptoUtil.randomAesKey();
        ctx.headerSet(ApiEncryptSupport.headerFlag(), ApiCryptoUtil.encryptByRsa(
                java.util.Base64.getEncoder().encodeToString(aesKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                ApiEncryptSupport.publicKey()));
        return ApiCryptoUtil.encryptByAes(json, aesKey);
    }
}
