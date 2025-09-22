package com.jimuqu.common.translation.service;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.util.ObjUtil;
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

        // 开始处理对象字段
        Field[] fields = ReflectUtil.getDeclaredFields(object.getClass());
        for (Field field : fields) {
            // 1. 处理带有 @Trans 注解的字段
            if (field.isAnnotationPresent(Trans.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (ObjUtil.isNotNull(value)) {
                        Trans trans = field.getAnnotation(Trans.class);
                        String translatedValue = doTranslate(value, trans);
                        // 如果翻译后的值不为空，则设置
                        if (ObjUtil.isNotEmpty(translatedValue)) {
                            // 获取要设置翻译值的字段名
                            String targetField = ObjUtil.isEmpty(trans.field()) ? field.getName() + "Name" : trans.field();
                            BeanUtil.setProperty(object, targetField, translatedValue);
                        }
                    }
                } catch (Exception e) {
                    // 实际项目中建议添加日志记录
                    e.printStackTrace();
                }
            }

            // 2. 递归处理对象类型的字段，无论是否有注解
            if (!field.getType().isPrimitive() && !field.getType().getName().startsWith("java")) {
                 try {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    translate(value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
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
