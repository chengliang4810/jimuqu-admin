package com.jimuqu.common.web.sensitive;

import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.encrypt.utils.ApiCryptoUtil;
import com.jimuqu.common.core.service.SensitiveService;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.utils.SensitiveUtil;
import com.jimuqu.common.web.encrypt.ApiEncryptSupport;
import org.noear.snack4.ONode;
import org.noear.snack4.annotation.ONodeAttr;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Render;
import org.noear.solon.serialization.snack4.Snack4StringSerializer;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * JSON 响应脱敏和接口响应加密渲染器。
 *
 * @author chengliang
 */
public class SensitiveJsonRender implements Render {

    private final Snack4StringSerializer serializer;
    private final SensitiveService sensitiveService;

    public SensitiveJsonRender(Snack4StringSerializer serializer) {
        this(serializer, (roleKey, perms) -> true);
    }

    public SensitiveJsonRender(Snack4StringSerializer serializer, SensitiveService sensitiveService) {
        this.serializer = serializer;
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
        Object body = ONode.deserialize(serializer.serialize(obj), Object.class);
        maskSensitiveFields(obj, body, new IdentityHashMap<>());
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
        ctx.headerAdd("Access-Control-Expose-Headers", ApiEncryptSupport.headerFlag());
        ctx.headerSet(ApiEncryptSupport.headerFlag(), ApiCryptoUtil.encryptByRsa(
                java.util.Base64.getEncoder().encodeToString(aesKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                ApiEncryptSupport.publicKey()));
        return ApiCryptoUtil.encryptByAes(json, aesKey);
    }

    private void maskSensitiveFields(Object source, Object target, IdentityHashMap<Object, Boolean> visited) {
        if (source == null || target == null || isSimpleValue(source.getClass()) || visited.containsKey(source)) {
            return;
        }
        visited.put(source, Boolean.TRUE);
        try {
            if (source instanceof Map<?, ?> sourceMap && target instanceof Map<?, ?> targetMap) {
                for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                    Object key = String.valueOf(entry.getKey());
                    maskSensitiveFields(entry.getValue(), targetMap.get(key), visited);
                }
                return;
            }
            if (source instanceof Iterable<?> iterable && target instanceof List<?> targetList) {
                int index = 0;
                for (Object item : iterable) {
                    if (index >= targetList.size()) {
                        break;
                    }
                    maskSensitiveFields(item, targetList.get(index++), visited);
                }
                return;
            }
            if (source.getClass().isArray() && target instanceof List<?> targetList) {
                int length = Math.min(Array.getLength(source), targetList.size());
                for (int index = 0; index < length; index++) {
                    maskSensitiveFields(Array.get(source, index), targetList.get(index), visited);
                }
                return;
            }
            if (target instanceof Map<?, ?> targetMap) {
                maskBeanFields(source, targetMap, visited);
            }
        } finally {
            visited.remove(source);
        }
    }

    @SuppressWarnings("unchecked")
    private void maskBeanFields(Object source, Map<?, ?> target, IdentityHashMap<Object, Boolean> visited) {
        Map<Object, Object> mutableTarget = (Map<Object, Object>) target;
        Set<Field> handledFields = applyGetterValues(source, mutableTarget, visited);
        Class<?> type = source.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (handledFields.contains(field) || shouldSkip(field) || !field.trySetAccessible()) {
                    continue;
                }
                String name = serializedName(field);
                if (!mutableTarget.containsKey(name)) {
                    continue;
                }
                try {
                    Object value = field.get(source);
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    if (shouldDesensitize(sensitive) && value instanceof CharSequence text) {
                        mutableTarget.put(name, SensitiveUtil.desensitize(text.toString(), sensitive));
                    } else {
                        maskSensitiveFields(value, mutableTarget.get(name), visited);
                    }
                } catch (IllegalAccessException ignored) {
                    // trySetAccessible 已成功，极端安全策略下读不到字段时保持原序列化值。
                }
            }
            type = type.getSuperclass();
        }
    }

    private Set<Field> applyGetterValues(Object source, Map<Object, Object> target,
                                         IdentityHashMap<Object, Boolean> visited) {
        Set<Field> handledFields = new HashSet<>();
        try {
            for (PropertyDescriptor property : Introspector
                    .getBeanInfo(source.getClass(), Object.class).getPropertyDescriptors()) {
                Method getter = property.getReadMethod();
                Field field = findField(source.getClass(), property.getName());
                if (getter == null || shouldSkip(getter) || field != null && shouldSkip(field)) {
                    continue;
                }
                String name = serializedName(field, getter, property.getName());
                if (field != null && !target.containsKey(name)) {
                    continue;
                }
                if (!getter.trySetAccessible()) {
                    continue;
                }
                Object getterValue = getter.invoke(source);
                if (field == null) {
                    // 仅保留 Snack4 已识别或显式标注的计算属性，避免把
                    // Page#getOffset 等内部辅助 getter 意外扩展为对外字段。
                    if (getterValue != null
                            && (target.containsKey(name) || getter.getAnnotation(ONodeAttr.class) != null)) {
                        Object propertyValue = serializedTree(getterValue);
                        maskSensitiveFields(getterValue, propertyValue, visited);
                        target.put(name, propertyValue);
                    }
                    continue;
                }
                if (!field.trySetAccessible()) {
                    continue;
                }
                handledFields.add(field);
                Sensitive sensitive = field.getAnnotation(Sensitive.class);
                if (shouldDesensitize(sensitive) && getterValue instanceof CharSequence text) {
                    target.put(name, SensitiveUtil.desensitize(text.toString(), sensitive));
                    continue;
                }
                Object fieldValue = field.get(source);
                if (!Objects.equals(getterValue, fieldValue)) {
                    Object propertyValue = serializedTree(getterValue);
                    maskSensitiveFields(getterValue, propertyValue, visited);
                    target.put(name, propertyValue);
                } else {
                    maskSensitiveFields(fieldValue, target.get(name), visited);
                }
            }
        } catch (IntrospectionException | ReflectiveOperationException | IOException ignored) {
            // 无法读取 getter 时回退到下方的字段遍历。
            handledFields.clear();
        }
        return handledFields;
    }

    private Object serializedTree(Object value) throws IOException {
        return ONode.deserialize(serializer.serialize(value), Object.class);
    }

    private boolean shouldDesensitize(Sensitive sensitive) {
        return sensitive != null && sensitiveService.isSensitive(sensitive.roleKey(), sensitive.perms());
    }

    private Field findField(Class<?> beanType, String name) {
        Class<?> type = beanType;
        while (type != null && type != Object.class) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        ONodeAttr nodeAttr = field.getAnnotation(ONodeAttr.class);
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || field.isSynthetic()
                || nodeAttr != null && (nodeAttr.ignore() || !nodeAttr.encode());
    }

    private boolean shouldSkip(Method getter) {
        ONodeAttr nodeAttr = getter.getAnnotation(ONodeAttr.class);
        return Modifier.isStatic(getter.getModifiers())
                || getter.isBridge()
                || getter.isSynthetic()
                || nodeAttr != null && (nodeAttr.ignore() || !nodeAttr.encode());
    }

    private String serializedName(Field field) {
        ONodeAttr nodeAttr = field.getAnnotation(ONodeAttr.class);
        if (nodeAttr != null && !nodeAttr.name().isBlank()) {
            return nodeAttr.name();
        }
        return field.getName();
    }

    private String serializedName(Field field, Method getter, String defaultName) {
        ONodeAttr getterAttr = getter.getAnnotation(ONodeAttr.class);
        if (getterAttr != null && !getterAttr.name().isBlank()) {
            return getterAttr.name();
        }
        return field == null ? defaultName : serializedName(field);
    }

    private boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || Date.class.isAssignableFrom(type)
                || TemporalAccessor.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type);
    }
}
