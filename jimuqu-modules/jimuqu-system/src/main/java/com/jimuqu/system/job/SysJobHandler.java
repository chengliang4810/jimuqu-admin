package com.jimuqu.system.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务处理器白名单标记。
 * <p>
 * 只有标注该注解的方法允许被在线任务调用。
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SysJobHandler {

    /**
     * 处理器唯一标识，保存到任务表 handlerKey 字段。
     */
    String value();

    /**
     * 处理器名称。
     */
    String name() default "";
}
