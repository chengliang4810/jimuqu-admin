package com.jimuqu.common.translation.core;

import com.jimuqu.common.translation.annotation.Trans;

import java.util.List;

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

    /**
     * 批量翻译。未实现批量查询的翻译器继续复用单值契约。
     *
     * @param values 待翻译值
     * @param trans  翻译注解
     * @return 与输入顺序一致的翻译结果
     */
    default List<String> translateBatch(List<?> values, Trans trans) {
        return values.stream().map(value -> translate(value, trans)).toList();
    }
}
