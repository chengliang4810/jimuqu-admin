package com.jimuqu.common.translation.service;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.xbatis.page.IPager;
import cn.xbatis.page.PagerField;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.common.translation.enums.TransType;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 翻译服务
 */
@Component
public class TranslationService {

    @Inject
    private Map<String, TranslationInterface> transMap = new HashMap<>();

    /**
     * 翻译
     *
     * @param object
     */
    public void translate(Object object) {
        if (ObjUtil.isNull(object)) {
            return;
        }

        if (object instanceof Collection<?> collection) {
            for (Object item : collection) {
                translate(item);
            }
            return;
        }

        if (object instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                translate(value);
            }
            return;
        }

        if (object instanceof IPager<?> iPager) {
            for (Object value : iPager.get(PagerField.RESULTS)) {
                translate(value);
            }
            return;
        }

        // 开始处理对象字段
        Field[] fields = ReflectUtil.getDeclaredFields(object.getClass());
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
                    // 实际项目中建议添加日志记录
                    e.printStackTrace();
                }
            }

            // 2. 递归处理自定义对象类型的字段，跳过Java核心类、数组、枚举等
            if (shouldProcessFieldType(field.getType())) {
                 try {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    translate(value);
                } catch (IllegalAccessException e) {
                    // 实际项目中建议添加日志记录
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 判断字段类型是否需要递归处理
     * 跳过：基本类型、Java核心类、数组、枚举、注解、代理类等
     */
    private boolean shouldProcessFieldType(Class<?> fieldType) {
        // 跳过基本类型及其包装类
        if (fieldType.isPrimitive() || fieldType.getName().startsWith("java.lang.")) {
            return false;
        }

        // 跳过其他Java核心包
        String packageName = fieldType.getPackage() != null ? fieldType.getPackage().getName() : "";
        if (packageName.startsWith("java.") ||
            packageName.startsWith("javax.") ||
            packageName.startsWith("jakarta.")) {
            return false;
        }

        // 跳过数组类型
        if (fieldType.isArray()) {
            return false;
        }

        // 跳过枚举类型
        if (fieldType.isEnum()) {
            return false;
        }

        // 跳过注解类型
        if (fieldType.isAnnotation()) {
            return false;
        }

        // 跳过代理类
        if (java.lang.reflect.Proxy.isProxyClass(fieldType)) {
            return false;
        }

        return true;
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
