package com.jimuqu.common.web.serialization;

import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;
import org.noear.solon.serialization.prop.JsonProps;

/** 在 Solon 装配业务配置前注册统一的 JSON 输入、输出实现。 */
public final class ForyJsonPlugin implements Plugin {
    @Override
    public void start(AppContext context) {
        ForyJsonStringSerializer serializer = new ForyJsonStringSerializer(JsonProps.create(context));
        context.wrapAndPut(ForyJsonStringSerializer.class, serializer);
        context.app().serializers().register("@json", serializer);
        ForyJsonEntityConverter converter = new ForyJsonEntityConverter(serializer);
        context.wrapAndPut(ForyJsonEntityConverter.class, converter);
        context.app().chains().addEntityConverter(converter);
    }
}
