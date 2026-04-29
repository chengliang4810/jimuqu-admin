package com.jimuqu.system.report.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 调度报表暴露注解。
 * <p>
 * 当前仅作为报表handler的稳定标记，后续调度模块可扫描该注解完成任务注册。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledReport {

    /**
     * 报表编码。
     */
    String value();

    /**
     * 报表名称。
     */
    String name() default "";
}
