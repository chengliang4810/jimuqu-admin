package com.jimuqu.common.translation.core.impl;

import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslatableEnum;
import com.jimuqu.common.translation.core.TranslationInterface;
import com.jimuqu.common.translation.enums.EmptyEnum;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举翻译
 */
@Slf4j
@Component(value = "enumTranslator", typed = true)
public class EnumTranslator implements TranslationInterface {

    /**
     * 枚举缓存
     */
    private final Map<Class<? extends TranslatableEnum>, Map<String, String>> enumCache = new ConcurrentHashMap<>();

    @Override
    public String translate(Object value, Trans trans) {
        if (ObjectUtil.isNull(value)) {
            return trans.defaultValue();
        }
        try {
            Class<? extends TranslatableEnum> enumClass = getEnumClass(trans);
            if (enumClass != null) {
                return getEnumDict(enumClass).get(value.toString());
            }
        } catch (Exception e) {
            log.error("枚举翻译异常", e);
        }
        return trans.defaultValue();
    }

    private Class<? extends TranslatableEnum> getEnumClass(Trans trans) throws ClassNotFoundException {
        if (trans.enumClass() != EmptyEnum.class) {
            return trans.enumClass();
        }
        // 兼容通过类名字符串方式
        Class<?> loadedClass = ClassLoaderUtil.loadClass(trans.value());
        if (TranslatableEnum.class.isAssignableFrom(loadedClass)) {
            return (Class<? extends TranslatableEnum>) loadedClass;
        }
        return null;
    }

    private Map<String, String> getEnumDict(Class<? extends TranslatableEnum> enumClass) {
        return enumCache.computeIfAbsent(enumClass, k -> {
            Map<String, String> dict = new ConcurrentHashMap<>();
            try {
                if (k.isEnum()) {
                    Object[] enumConstants = k.getEnumConstants();
                    for (Object enumConstant : enumConstants) {
                        TranslatableEnum<?> translatableEnum = (TranslatableEnum<?>) enumConstant;
                        Object code = translatableEnum.getValue();
                        String label = translatableEnum.getLabel();
                        if (ObjectUtil.isNotNull(code) && StrUtil.isNotBlank(label)) {
                            dict.put(Convert.toStr(code), label);
                        }
                    }
                } else {
                    log.warn("类 {} 不是一个枚举，无法进行翻译。", k.getName());
                }
            } catch (Exception e) {
                log.error("加载并解析枚举类失败: {}", k.getName(), e);
            }
            return dict;
        });
    }
}
