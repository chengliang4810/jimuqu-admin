package com.jimuqu.common.web.sensitive;

import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.service.SensitiveService;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.utils.SensitiveUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.web.encrypt.ApiEncryptSupport;
import com.jimuqu.common.web.serialization.ForyJsonStringSerializer;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Render;

import java.util.Objects;

/** JSON 响应脱敏和接口响应加密渲染器。 */
public class SensitiveJsonRender implements Render {
    private final ForyJsonStringSerializer serializer;
    private final SensitiveService sensitiveService;

    public SensitiveJsonRender(ForyJsonStringSerializer serializer) {
        this(serializer, (roleKey, perms) -> true);
    }

    public SensitiveJsonRender(ForyJsonStringSerializer serializer, SensitiveService sensitiveService) {
        this.serializer = Objects.requireNonNull(serializer);
        this.sensitiveService = Objects.requireNonNull(sensitiveService);
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
        Object body = serializer.toTree(obj, (field, value) -> {
            Sensitive sensitive = field.getAnnotation(Sensitive.class);
            if (sensitive != null && value instanceof CharSequence text
                    && sensitiveService.isSensitive(sensitive.roleKey(), sensitive.perms())) {
                return SensitiveUtil.desensitize(text.toString(), sensitive);
            }
            return value;
        });
        return encryptIfNecessary(JsonUtil.writeTree(body), ctx);
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
        ctx.headerAdd("Access-Control-Expose-Headers", ApiEncryptSupport.headerFlag());
        ctx.headerSet(ApiEncryptSupport.headerFlag(), ApiCryptoUtil.encryptByRsa(
                java.util.Base64.getEncoder().encodeToString(aesKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                ApiEncryptSupport.publicKey()));
        return ApiCryptoUtil.encryptByAes(json, aesKey);
    }

}
