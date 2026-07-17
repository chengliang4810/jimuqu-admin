package com.jimuqu.common.core.encrypt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级请求/响应加密标记。
 *
 * @author chengliang
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEncrypt {

    /** 响应是否加密，默认仅强制请求加密。 */
    boolean response() default false;
}
