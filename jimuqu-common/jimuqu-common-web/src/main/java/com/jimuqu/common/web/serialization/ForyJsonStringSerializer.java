package com.jimuqu.common.web.serialization;

import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.JsonWriteSupport;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.serialization.EntitySerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

/** Apache Fory JSON 的 Solon 文本序列化适配。 */
public final class ForyJsonStringSerializer implements EntitySerializer<String> {
    @Override
    public String name() {
        return "fory-json";
    }

    @Override
    public String mimeType() {
        return "application/json;charset=UTF-8";
    }

    @Override
    public Class<String> dataType() {
        return String.class;
    }

    @Override
    public boolean matched(Context ctx, String mime) {
        if (mime == null) {
            return false;
        }
        String mediaType = mime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return "application/json".equals(mediaType) || "text/json".equals(mediaType)
                || mediaType.startsWith("application/") && mediaType.endsWith("+json");
    }

    @Override
    public String serialize(Object value) {
        return JsonUtil.writeTree(JsonWriteSupport.toTree(value, true, null));
    }

    @Override
    public Object deserialize(String data, Type type) {
        return JsonUtil.toObject(data, type);
    }

    @Override
    public void serializeToBody(Context ctx, Object value) throws IOException {
        if (ctx.contentTypeNew() == null) {
            ctx.contentType(mimeType());
        }
        ctx.output(serialize(value instanceof ModelAndView view ? view.model() : value));
    }

    @Override
    public Object deserializeFromBody(Context ctx, Type type) throws IOException {
        return deserialize(ctx.bodyNew(), type);
    }
}
