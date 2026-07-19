package com.jimuqu.common.translation.service;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.xbatis.page.IPager;
import cn.xbatis.page.PagerField;
import com.jimuqu.common.core.domain.PageResult;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.common.translation.enums.TransType;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 翻译服务
 */
@Component
@Slf4j
public class TranslationService {

    @Inject
    private Map<String, TranslationInterface> transMap = new HashMap<>();

    /**
     * 翻译
     *
     * @param object
     */
    public void translate(Object object) {
        translate(object, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private void translate(Object object, Set<Object> visited) {
        if (ObjUtil.isNull(object)) {
            return;
        }

        Class<?> objectType = object.getClass();
        if (!visited.add(object)) {
            return;
        }

        if (object instanceof Collection<?> collection) {
            for (Object item : collection) {
                translate(item, visited);
            }
            return;
        }

        if (object instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                translate(value, visited);
            }
            return;
        }

        if (object instanceof IPager<?> iPager) {
            for (Object value : iPager.get(PagerField.RESULTS)) {
                translate(value, visited);
            }
            return;
        }

        if (object instanceof PageResult<?> pageResult) {
            translate(pageResult.getRows(), visited);
            return;
        }

        if (objectType.isArray()) {
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                translate(Array.get(object, i), visited);
            }
            return;
        }

        if (isSimpleType(objectType)) {
            return;
        }

        // 开始处理对象字段
        Field[] fields = ReflectUtil.getDeclaredFields(objectType);
        for (Field field : fields) {
            // 1. 处理带有 @Trans 注解的字段
            if (field.isAnnotationPresent(Trans.class)) {
                try {
                    field.setAccessible(true);
                    Trans trans = field.getAnnotation(Trans.class);

                    // 获取源字段的值进行翻译
                    String sourceField = ObjUtil.isEmpty(trans.field()) ? field.getName() : trans.field();
                    Object sourceValue = BeanUtil.getProperty(object, sourceField);

                    if (ObjUtil.isNotNull(sourceValue)) {
                        String translatedValue = doTranslate(sourceValue, trans);
                        // 将翻译结果设置到当前字段
                        if (ObjUtil.isNotEmpty(translatedValue)) {
                            field.set(object, translatedValue);
                        } else if (ObjUtil.isNotEmpty(trans.defaultValue())) {
                            field.set(object, trans.defaultValue());
                        }
                    }
                } catch (Exception e) {
                    log.warn("翻译字段失败, objectType: {}, field: {}", objectType.getName(), field.getName(), e);
                }
            }

            // 2. 按字段实际值递归，确保 List、Map、PageResult 等容器字段可被处理。
            try {
                field.setAccessible(true);
                translate(field.get(object), visited);
            } catch (Exception e) {
                log.warn("遍历待翻译字段失败, objectType: {}, field: {}", objectType.getName(), field.getName(), e);
            }
        }
    }

    /**
     * 判断字段类型是否需要递归处理
     * 跳过：基本类型、Java核心类、数组、枚举、注解、代理类等
     */
    private boolean isSimpleType(Class<?> type) {
        if (type.isPrimitive() || type.isEnum() || type.isAnnotation()) {
            return true;
        }
        String packageName = type.getPackageName();
        return packageName.startsWith("java.lang")
                || packageName.startsWith("java.time")
                || packageName.startsWith("java.math")
                || packageName.startsWith("java.net")
                || packageName.startsWith("java.nio")
                || packageName.equals("java.util")
                || packageName.startsWith("java.util.concurrent")
                || packageName.startsWith("javax.")
                || packageName.startsWith("jakarta.")
                || java.lang.reflect.Proxy.isProxyClass(type);
    }

    private String doTranslate(Object value, Trans trans) {
        TransType type = trans.type();
        TranslationInterface translator = transMap.get(type.name().toLowerCase() + "Translator");
        if (translator != null) {
            return translator.translate(value, trans);
        }
        // 如果没有找到对应的翻译器，可以返回默认值或原始值
        return trans.defaultValue();
    }
}
