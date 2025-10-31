package com.jimuqu.common.translation.core;

/**
 * 可翻译枚举接口
 */
public interface TranslatableEnum<T> {

    /**
     * 获取枚举的实际值
     *
     * @return 枚举值
     */
    T getValue();

    /**
     * 获取枚举的显示标签
     *
     * @return 显示标签
     */
    String getLabel();

}
