package com.jimuqu.common.translation.annotation;

import com.jimuqu.common.translation.core.TranslatableEnum;
import com.jimuqu.common.translation.enums.TransType;
import com.jimuqu.common.translation.enums.VoidEnum;

/**
 * 翻译注解
 */
public @interface Trans {
    /**
     * 值, 不同的翻译类型值不同
     * 当 type = DICT 时, value 为字典类型
     * 当 type = ENUM 且 enumClass 为 VoidEnum.class 时, value 为枚举类全名
     */
    String value() default "";

    /**
     * 翻译类型
     */
    TransType type() default TransType.DEFAULT;

    /**
     * 当 type = ENUM 时, 指定枚举类
     */
    Class<? extends TranslatableEnum> enumClass() default VoidEnum.class;

    /**
     * 翻译的字段，默认为自身
     */
    String field() default "";

    /**
     * 默认值，当无法获取翻译时返回此值
     */
    String defaultValue() default "";
}
