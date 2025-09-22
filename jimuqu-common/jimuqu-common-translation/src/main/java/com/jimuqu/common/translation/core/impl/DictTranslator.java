package com.jimuqu.common.translation.core.impl;

import cn.hutool.v7.core.util.ObjUtil;
import com.jimuqu.common.core.service.DictService;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典翻译
 */
@Slf4j
@Component(value = "dictTranslator", typed = true)
public class DictTranslator implements TranslationInterface {

    /**
     * 字典缓存
     */
    private final Map<String, String> dictCache = new ConcurrentHashMap<>();

    @Inject
    private DictService dictService;

    @Override
    public String translate(Object value, Trans trans) {
        if (ObjUtil.isNull(value) || ObjUtil.isNull(dictService)) {
            return trans.defaultValue();
        }
        String dictType = trans.value();
        String dictValue = value.toString();

        // 优先从缓存获取
        String cacheKey = dictType + ":" + dictValue;
        if (dictCache.containsKey(cacheKey)) {
            return dictCache.get(cacheKey);
        }

        // 缓存未命中，调用服务查询
        try {
            String dictLabel = dictService.getDictLabel(dictType, dictValue);
            if (ObjUtil.isNotEmpty(dictLabel)) {
                dictCache.put(cacheKey, dictLabel);
                return dictLabel;
            }
        } catch (Exception e) {
            log.error("字典翻译异常", e);
        }

        return trans.defaultValue();
    }
}
