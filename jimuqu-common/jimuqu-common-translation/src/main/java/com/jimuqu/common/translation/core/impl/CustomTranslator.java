package com.jimuqu.common.translation.core.impl;

import cn.hutool.v7.core.util.ObjUtil;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

/**
 * 自定义翻译
 */
@Slf4j
@Component(value = "customTranslator", typed = true)
public class CustomTranslator implements TranslationInterface {

    @Override
    public String translate(Object value, Trans trans) {
        if (ObjUtil.isNull(value)) {
            return trans.defaultValue();
        }

        // 自定义翻译逻辑将在此实现
        // 目前返回默认值，避免返回null导致TranslationService设置属性失败
        log.warn("自定义翻译器尚未实现，字段值: {}, 使用默认值: {}", value, trans.defaultValue());
        return trans.defaultValue();
    }
}
