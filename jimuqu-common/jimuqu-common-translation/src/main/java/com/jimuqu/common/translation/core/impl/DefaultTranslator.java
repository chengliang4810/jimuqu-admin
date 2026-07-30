package com.jimuqu.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认翻译器
 * <p>
 * 支持三种格式的内联字典:
 * 1. key=value,key=value
 * 2. key:value|key:value
 * 3. value,value,value (索引为key)
 * </p>
 */
@Slf4j
@Component(value = "defaultTranslator", typed = true)
public class DefaultTranslator implements TranslationInterface {

    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    @Override
    public String translate(Object value, Trans trans) {
        if (ObjectUtil.isNull(value)) {
            return trans.defaultValue();
        }
        String mappingString = trans.value();
        if (StrUtil.isBlank(mappingString)) {
            return trans.defaultValue();
        }

        try {
            Map<String, String> mapping = parseMapping(mappingString);
            String key = Convert.toStr(value);
            // 使用 getOrDefault 简化逻辑
            String translatedValue = mapping.get(key);
            return ObjectUtil.defaultIfNull(translatedValue, trans.defaultValue());
        } catch (Exception e) {
            log.error("默认翻译器解析或翻译异常, mappingString: {}", mappingString, e);
            return trans.defaultValue();
        }
    }

    private Map<String, String> parseMapping(String mappingString) {
        return cache.computeIfAbsent(mappingString, k -> {
            Map<String, String> map = new HashMap<>();
            // 格式3: 索引作为key (e.g., "低,中,高")
            if (!k.contains("=") && !k.contains(":")) {
                List<String> values = StrUtil.split(k, ",");
                for (int i = 0; i < values.size(); i++) {
                    map.put(String.valueOf(i), values.get(i).trim());
                }
            } else {
                // 格式1和2: key-value 对
                String entrySeparator = k.contains(",") ? "," : "|";
                String keyValueSeparator = k.contains("=") ? "=" : ":";
                List<String> pairs = StrUtil.split(k, entrySeparator);
                for (String pair : pairs) {
                    if (StrUtil.isNotBlank(pair)) {
                        List<String> kv = StrUtil.split(pair, keyValueSeparator, 2, true, true);
                        if (kv.size() == 2) {
                            map.put(kv.getFirst().trim(), kv.get(1).trim());
                        }
                    }
                }
            }
            return map;
        });
    }
}
