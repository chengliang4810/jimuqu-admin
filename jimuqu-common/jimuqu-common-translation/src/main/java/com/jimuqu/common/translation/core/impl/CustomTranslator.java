package com.jimuqu.common.translation.core.impl;

import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import org.noear.solon.annotation.Component;

/**
 * 自定义翻译
 */
@Component(value = "customTranslator", typed = true)
public class CustomTranslator implements TranslationInterface {
    @Override
    public String translate(Object value, Trans trans) {
        // 自定义翻译逻辑将在此实现
        return null;
    }
}
