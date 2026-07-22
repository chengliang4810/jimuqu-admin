package com.jimuqu.common.core.sensitive.utils;

import cn.hutool.core.util.StrUtil;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.noear.snack4.annotation.ONodeAttr;

import java.lang.reflect.Array;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 脱敏工具。
 *
 * @author chengliang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SensitiveUtil {

    public static String desensitize(String value, Sensitive sensitive) {
        if (sensitive == null) {
            return value;
        }
        if (sensitive.type() == SensitiveType.CUSTOM) {
            return desensitize(value, sensitive.prefixKeep(), sensitive.suffixKeep(), sensitive.mask());
        }
        return desensitize(value, sensitive.type());
    }

    public static String desensitize(String value, SensitiveType type) {
        if (type == null) {
            return value;
        }
        return switch (type) {
            case MOBILE -> desensitize(value, 3, 4, "****");
            case EMAIL -> email(value);
            case ID_CARD -> desensitize(value, 4, 4, "**********");
            case BANK_CARD -> desensitize(value, 4, 4, "***********");
            case NAME -> name(value);
            case ADDRESS -> desensitize(value, 6, 0, "******");
            case CUSTOM -> desensitize(value, 0, 0, "****");
        };
    }

    public static String desensitize(String value, int prefixKeep, int suffixKeep, String mask) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int prefix = Math.max(prefixKeep, 0);
        int suffix = Math.max(suffixKeep, 0);
        if (value.length() <= prefix + suffix) {
            return value;
        }
        String safeMask = StrUtil.isBlank(mask) ? "****" : mask;
        return value.substring(0, prefix) + safeMask + value.substring(value.length() - suffix);
    }

    /**
     * 按字段上的 {@link Sensitive} 注解生成脱敏后的可序列化对象。
     */
    public static Object desensitizeObject(Object value) {
        return desensitizeObject(value, new IdentityHashMap<>());
    }

    private static Object desensitizeObject(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isSimpleValue(value.getClass())) {
            return value;
        }
        if (visited.containsKey(value)) {
            return null;
        }
        visited.put(value, Boolean.TRUE);
        try {
            if (value instanceof Collection<?> collection) {
                List<Object> list = new ArrayList<>(collection.size());
                for (Object item : collection) {
                    list.add(desensitizeObject(item, visited));
                }
                return list;
            }
            if (value instanceof Map<?, ?> map) {
                Map<Object, Object> result = new LinkedHashMap<>(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    result.put(entry.getKey(), desensitizeObject(entry.getValue(), visited));
                }
                return result;
            }
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                List<Object> list = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    list.add(desensitizeObject(Array.get(value, i), visited));
                }
                return list;
            }
            return beanToMap(value, visited);
        } finally {
            visited.remove(value);
        }
    }

    private static Map<String, Object> beanToMap(Object bean, IdentityHashMap<Object, Boolean> visited) {
        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> type = bean.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (shouldSkip(field)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(bean);
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    if (sensitive != null && fieldValue instanceof CharSequence text) {
                        fieldValue = desensitize(text.toString(), sensitive);
                    } else {
                        fieldValue = desensitizeObject(fieldValue, visited);
                    }
                    result.put(serializedName(field), fieldValue);
                } catch (IllegalAccessException ignored) {
                    // 已设置 accessible，极端安全策略下读不到字段时跳过该字段。
                }
            }
            type = type.getSuperclass();
        }
        return result;
    }

    private static boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || field.isSynthetic()
                || hasAnnotation(field, "com.fasterxml.jackson.annotation.JsonIgnore")
                || hasIgnoredNodeAttr(field);
    }

    private static String serializedName(Field field) {
        ONodeAttr nodeAttr = field.getAnnotation(ONodeAttr.class);
        if (nodeAttr != null && !nodeAttr.name().isBlank()) {
            return nodeAttr.name();
        }
        String jsonProperty = annotationStringValue(
                field, "com.fasterxml.jackson.annotation.JsonProperty", "value");
        if (jsonProperty != null && !jsonProperty.isBlank()) {
            return jsonProperty;
        }
        return field.getName();
    }

    private static boolean hasIgnoredNodeAttr(Field field) {
        ONodeAttr nodeAttr = field.getAnnotation(ONodeAttr.class);
        return nodeAttr != null && nodeAttr.ignore();
    }

    private static boolean hasAnnotation(Field field, String annotationType) {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationType)) {
                return true;
            }
        }
        return false;
    }

    private static String annotationStringValue(Field field, String annotationType, String attribute) {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            if (!annotation.annotationType().getName().equals(annotationType)) {
                continue;
            }
            try {
                return String.valueOf(annotation.annotationType().getMethod(attribute).invoke(annotation));
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || Date.class.isAssignableFrom(type)
                || TemporalAccessor.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type);
    }

    private static String email(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return value;
        }
        return value.charAt(0) + "***" + value.substring(atIndex);
    }

    private static String name(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value;
        }
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*" + value.charAt(value.length() - 1);
    }
}
