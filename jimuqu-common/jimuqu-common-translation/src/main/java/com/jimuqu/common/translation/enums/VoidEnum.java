package com.jimuqu.common.translation.enums;

import com.jimuqu.common.translation.core.*;

/**
 * 空枚举，用于注解的默认值
 */
public enum VoidEnum implements TranslatableEnum<Object> {
    ;

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public String getLabel() {
        return null;
    }
}
