package com.jimuqu.common.web.serialization;

import org.noear.solon.core.handle.Context;
import org.noear.solon.core.util.LazyReference;
import org.noear.solon.core.wrap.MethodWrap;
import org.noear.solon.core.wrap.ParamWrap;
import org.noear.solon.serialization.AbstractStringEntityConverter;

import java.util.Collection;
import java.util.Map;

/** 保留 Solon 的路径、查询参数优先级和 JSON body 泛型绑定规则。 */
public final class ForyJsonEntityConverter extends AbstractStringEntityConverter<ForyJsonStringSerializer> {
    public ForyJsonEntityConverter(ForyJsonStringSerializer serializer) {
        super(serializer);
    }

    @Override
    public String[] mappings() {
        return new String[]{"@json"};
    }

    @Override
    protected Object changeBody(Context ctx, MethodWrap method) throws Exception {
        return serializer.deserializeFromBody(ctx);
    }

    @Override
    protected Object changeValue(Context ctx, ParamWrap param, int index, Class<?> type,
                                 LazyReference bodyRef) throws Throwable {
        if (param.spec().isRequiredPath() || param.spec().isRequiredCookie() || param.spec().isRequiredHeader()
                || !param.spec().isRequiredBody() && ctx.paramMap().containsKey(param.spec().getName())) {
            return super.changeValue(ctx, param, index, type, bodyRef);
        }
        Object body = bodyRef.get();
        if (body == null) {
            return super.changeValue(ctx, param, index, type, bodyRef);
        }
        if (param.spec().isRequiredBody()) {
            return serializer.deserialize(ctx.bodyNew(), param.getGenericType());
        }
        if (body instanceof Map<?, ?> map) {
            if (map.containsKey(param.spec().getName())) {
                return serializer.deserialize(com.jimuqu.common.core.utils.JsonUtil.writeTree(
                        map.get(param.spec().getName())), param.getGenericType());
            }
            if (type.isPrimitive() || type.getName().startsWith("java.lang.")) {
                return super.changeValue(ctx, param, index, type, bodyRef);
            }
            if (Collection.class.isAssignableFrom(type) || type.isArray()) {
                return null;
            }
            return serializer.deserialize(ctx.bodyNew(), param.getGenericType());
        }
        if (body instanceof Collection<?> && !Collection.class.isAssignableFrom(type) && !type.isArray()) {
            return null;
        }
        return serializer.deserialize(ctx.bodyNew(), param.getGenericType());
    }
}
