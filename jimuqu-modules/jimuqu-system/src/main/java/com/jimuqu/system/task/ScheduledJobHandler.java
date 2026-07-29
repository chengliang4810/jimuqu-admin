package com.jimuqu.system.task;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 允许在线定时任务调用的 Solon Bean 方法白名单。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledJobHandler {

    /**
     * 前后端持久化使用的稳定处理器标识。
     *
     * @return 处理器标识
     */
    String key();

    /**
     * 处理器的界面说明。
     *
     * @return 处理器说明
     */
    String description();
}
