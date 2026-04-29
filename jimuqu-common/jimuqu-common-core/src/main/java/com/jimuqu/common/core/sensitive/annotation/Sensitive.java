package com.jimuqu.common.core.sensitive.annotation;

import com.jimuqu.common.core.sensitive.enums.SensitiveType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON 响应字段脱敏标记。
 *
 * @author chengliang
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    /**
     * 脱敏类型。
     */
    SensitiveType type() default SensitiveType.CUSTOM;

    /**
     * 自定义脱敏时前置保留字符数。
     */
    int prefixKeep() default 0;

    /**
     * 自定义脱敏时后置保留字符数。
     */
    int suffixKeep() default 0;

    /**
     * 中间替换字符串。
     */
    String mask() default "****";
}
