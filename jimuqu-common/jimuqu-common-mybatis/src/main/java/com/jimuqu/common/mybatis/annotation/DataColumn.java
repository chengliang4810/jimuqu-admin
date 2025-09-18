package com.jimuqu.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据列注解
 * <p>
 * 用于定义数据权限的列映射关系
 * 配合 @DataPermission 注解使用
 *
 * @author chengliang4810
 * @version 1.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataColumn {

    /**
     * SpEL表达式中的占位符关键字
     * 默认为 "deptName"
     */
    String key() default "deptName";

    /**
     * 占位符对应的数据库字段
     * 默认为 "dept_id"
     */
    String value() default "dept_id";

}