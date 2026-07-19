package com.jimuqu.common.excel.core;

import java.util.Set;

/**
 * Excel 动态下拉选项提供器。
 */
public interface ExcelOptionsProvider {

    /**
     * 获取当前可选项。
     */
    Set<String> getOptions();
}
