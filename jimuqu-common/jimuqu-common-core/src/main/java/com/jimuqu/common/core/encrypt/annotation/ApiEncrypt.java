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
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEncrypt {

    /**
     * 是否解密请求 JSON。
     */
    boolean request() default true;

    /**
     * 是否加密响应 JSON。
     */
    boolean response() default true;

    /**
     * 请求体不是合法加密载荷时是否抛出异常。
     */
    boolean required() default true;

    /**
     * 响应加密公钥。为空时读取配置 api.encrypt.public-key。
     */
    String publicKey() default "";

    /**
     * 请求解密私钥。为空时读取配置 api.encrypt.private-key。
     */
    String privateKey() default "";
}
