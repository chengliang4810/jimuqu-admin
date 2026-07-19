package com.jimuqu.common.excel.annotation;

import com.jimuqu.common.excel.core.ExcelOptionsProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 动态下拉选项。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelDynamicOptions {

    /**
     * 下拉选项提供器。
     */
    Class<? extends ExcelOptionsProvider> providerClass();
}
