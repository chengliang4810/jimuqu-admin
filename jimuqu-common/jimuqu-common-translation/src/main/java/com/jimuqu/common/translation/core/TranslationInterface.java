package com.jimuqu.common.translation.core;

import com.jimuqu.common.translation.annotation.Trans;

/**
 * 翻译接口
 */
public interface TranslationInterface {

    /**
     * 翻译
     *
     * @param value a value
     * @param trans a trans
     * @return {@link String}
     */
    String translate(Object value, Trans trans);
}
