package com.jimuqu.common.translation.core.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jimuqu.common.core.service.DictService;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.core.TranslationInterface;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 字典翻译
 */
@Slf4j
@Component(value = "dictTranslator", typed = true)
public class DictTranslator implements TranslationInterface {

    @Inject
    private DictService dictService;

    @Override
    public String translate(Object value, Trans trans) {
        if (ObjectUtil.isNull(value) || ObjectUtil.isNull(dictService)) {
            return trans.defaultValue();
        }
        String dictType = trans.value();
        String dictValue = value.toString();

        try {
            String dictLabel = dictService.getDictLabel(dictType, dictValue);
            if (ObjectUtil.isNotEmpty(dictLabel)) {
                return dictLabel;
            }
        } catch (Exception e) {
            log.error("字典翻译异常", e);
        }

        return trans.defaultValue();
    }
}
