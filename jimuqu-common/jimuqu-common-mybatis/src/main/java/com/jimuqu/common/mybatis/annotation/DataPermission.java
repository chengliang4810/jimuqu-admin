package com.jimuqu.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>
 * 用于在方法或类上标记数据权限控制
 * 配合 @DataColumn 注解使用，定义数据权限的列映射关系
 *
 * @author chengliang4810
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 数据列映射关系
     * 用于定义数据权限的键值对映射
     */
    DataColumn[] value() default {};

}