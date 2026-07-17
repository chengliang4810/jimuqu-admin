package com.jimuqu.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应数据。
 *
 * @param <T> 行数据类型
 * @author chengliang4810
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据。
     */
    private List<T> rows = Collections.emptyList();

    /**
     * 数据总数。
     */
    private Integer total = 0;
}
