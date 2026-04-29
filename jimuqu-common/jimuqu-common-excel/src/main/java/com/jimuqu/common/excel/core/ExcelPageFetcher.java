package com.jimuqu.common.excel.core;

import java.util.List;

/**
 * Excel分页数据拉取函数。
 *
 * @param <T> 导出行类型
 */
@FunctionalInterface
public interface ExcelPageFetcher<T> {

    /**
     * 按页拉取导出数据。
     *
     * @param page     页码，从1开始
     * @param pageSize 每页条数
     * @return 当前页数据，空列表表示没有更多数据
     */
    List<T> fetch(int page, int pageSize);
}
