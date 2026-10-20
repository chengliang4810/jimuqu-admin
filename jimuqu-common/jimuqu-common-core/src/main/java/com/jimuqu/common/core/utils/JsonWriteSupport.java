package com.jimuqu.common.core.utils;

import org.apache.fory.json.ForyJsonException;
import org.apache.fory.json.annotation.JsonIgnore;
import org.apache.fory.json.annotation.JsonInclude;
import org.apache.fory.json.annotation.JsonProperty;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 将响应投影为 JSON 值，保留浏览器数字精度、字段范围和业务 getter 语义。
 * 字段元数据按类缓存；输出期间不修改源对象。
 */
public final class JsonWriteSupport {
    private JsonWriteSupport() {
    }

    private static final ClassValue<List<Property>> PROPERTIES = new ClassValue<>() {
        @Override
        protected List<Property> computeValue(Class<?> type) {
            if (type == Object.class) {
                return List.of();
            }
            Map<String, Method> getters = new LinkedHashMap<>();
            try {
                for (var property : Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors()) {
                    Method getter = property.getReadMethod();
                    if (getter != null && getter.trySetAccessible()) {
                        getters.put(property.getName(), getter);
                    }
                }
            } catch (IntrospectionException e) {
                throw new ForyJsonException("Cannot inspect JSON properties: " + type.getName(), e);
            }
            Map<String, Property> properties = new LinkedHashMap<>();
            for (Class<?> current = type; current != null && current != Object.class;
                 current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    Method getter = getters.remove(field.getName());
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()
                            || ignored(field) || !field.trySetAccessible()) {
                        continue;
                    }
                    if (type.isRecord()) {
                        try {
                            getter = type.getDeclaredMethod(field.getName());
                            getter.trySetAccessible();
                        } catch (NoSuchMethodException e) {
                            throw new ForyJsonException("Cannot inspect record: " + type.getName(), e);
                        }
                    }
                    String name = name(field.getAnnotation(JsonProperty.class), field.getName());
                    if (getter != null && getter.getAnnotation(JsonProperty.class) != null) {
                        name = name(getter.getAnnotation(JsonProperty.class), name);
                    }
                    properties.putIfAbsent(name, new Property(name, field, getter, inclusion(type, field, getter)));
                }
            }
            // 未绑定字段的辅助 getter 不属于接口；显式声明的计算属性才输出。
            getters.forEach((name, getter) -> {
                JsonProperty annotation = getter.getAnnotation(JsonProperty.class);
                if (annotation != null) {
                    String jsonName = name(annotation, name);
                    properties.putIfAbsent(jsonName, new Property(jsonName, null, getter, inclusion(type, null, getter)));
                }
            });
            return List.copyOf(properties.values());
        }
    };

    public static Object toTree(Object value, boolean writeNulls,
                                BiFunction<Field, Object, Object> fieldTransform) {
        return toTree(value, writeNulls, fieldTransform, null);
    }

    public static Object toTree(Object value, boolean writeNulls,
                                BiFunction<Field, Object, Object> fieldTransform,
                                Function<Object, Object> dateEncoder) {
        return toTree(value, writeNulls, fieldTransform, dateEncoder, new IdentityHashMap<>(), 0);
    }

    private static Object toTree(Object value, boolean writeNulls,
                                 BiFunction<Field, Object, Object> fieldTransform,
                                 Function<Object, Object> dateEncoder,
                                 IdentityHashMap<Object, Boolean> path, int depth) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return JsonNumberCodec.encode(number);
        }
        if (value instanceof CharSequence text) {
            return text.toString();
        }
        if (value instanceof Character || value instanceof Boolean
                || value instanceof Enum<?> || value instanceof java.util.UUID) {
            return value;
        }
        if (dateEncoder != null && (value instanceof Date || value instanceof java.time.temporal.TemporalAccessor)) {
            return dateEncoder.apply(value);
        }
        if (value instanceof java.time.LocalDateTime dateTime) {
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (value instanceof java.time.LocalDate date) {
            return date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (value instanceof java.time.Instant instant) {
            return instant.toEpochMilli();
        }
        if (value instanceof Date date) {
            return date.getTime();
        }
        if (depth >= 64 || path.put(value, Boolean.TRUE) != null) {
            throw new ForyJsonException("JSON nesting is too deep or contains a circular reference");
        }
        try {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>(map.size());
                map.forEach((key, item) -> {
                    if (item != null || writeNulls) {
                        result.put(String.valueOf(key), toTree(item, writeNulls, fieldTransform, dateEncoder, path, depth + 1));
                    }
                });
                return result;
            }
            if (value instanceof Iterable<?> iterable) {
                List<Object> result = new ArrayList<>();
                for (Object item : iterable) {
                    result.add(toTree(item, writeNulls, fieldTransform, dateEncoder, path, depth + 1));
                }
                return result;
            }
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                List<Object> result = new ArrayList<>(length);
                for (int index = 0; index < length; index++) {
                    result.add(toTree(Array.get(value, index), writeNulls, fieldTransform, dateEncoder, path, depth + 1));
                }
                return result;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            for (Property property : PROPERTIES.get(value.getClass())) {
                Object item = property.read(value);
                if (fieldTransform != null && property.field() != null) {
                    item = fieldTransform.apply(property.field(), item);
                }
                if (included(item, property.include(), writeNulls)) {
                    result.put(property.name(), toTree(item, writeNulls, fieldTransform, dateEncoder, path, depth + 1));
                }
            }
            return result;
        } finally {
            path.remove(value);
        }
    }

    private static boolean ignored(Field field) {
        JsonIgnore annotation = field.getAnnotation(JsonIgnore.class);
        return annotation != null && annotation.ignoreWrite();
    }

    private static String name(JsonProperty annotation, String defaultName) {
        return annotation == null || annotation.value().isBlank() ? defaultName : annotation.value();
    }

    private static JsonProperty.Include inclusion(Class<?> type, Field field, Method getter) {
        JsonProperty property = getter == null ? null : getter.getAnnotation(JsonProperty.class);
        if (property == null || property.include() == JsonProperty.Include.DEFAULT) {
            property = field == null ? null : field.getAnnotation(JsonProperty.class);
        }
        if (property != null && property.include() != JsonProperty.Include.DEFAULT) {
            return property.include();
        }
        JsonInclude annotation = type.getAnnotation(JsonInclude.class);
        return annotation == null ? JsonProperty.Include.DEFAULT : annotation.value();
    }

    private static boolean included(Object value, JsonProperty.Include include, boolean writeNulls) {
        return switch (include) {
            case ALWAYS -> true;
            case NON_NULL -> value != null;
            case NON_EMPTY -> value != null
                    && !(value instanceof CharSequence text && text.isEmpty())
                    && !(value instanceof java.util.Collection<?> collection && collection.isEmpty())
                    && !(value instanceof Map<?, ?> map && map.isEmpty())
                    && !(value.getClass().isArray() && Array.getLength(value) == 0);
            case NON_DEFAULT -> value != null && !Boolean.FALSE.equals(value)
                    && !(value instanceof Number number && isZero(number))
                    && !(value instanceof Character character && character == '\0');
            case DEFAULT -> value != null || writeNulls;
        };
    }

    private static boolean isZero(Number number) {
        if (number instanceof java.math.BigDecimal decimal) {
            return decimal.signum() == 0;
        }
        if (number instanceof java.math.BigInteger integer) {
            return integer.signum() == 0;
        }
        return number.doubleValue() == 0;
    }

    private record Property(String name, Field field, Method getter, JsonProperty.Include include) {
        Object read(Object owner) {
            try {
                return getter == null ? field.get(owner) : getter.invoke(owner);
            } catch (ReflectiveOperationException e) {
                throw new ForyJsonException("Cannot read JSON property: " + name, e);
            }
        }
    }
}
