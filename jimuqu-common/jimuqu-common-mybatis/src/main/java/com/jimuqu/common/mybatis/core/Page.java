package com.jimuqu.common.mybatis.core;

import cn.xbatis.page.IPager;
import cn.xbatis.page.PageUtil;
import cn.xbatis.page.PagerField;
import com.jimuqu.common.core.domain.PageResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;



/**
 * 分页对象
 *
 * @author chengliang
 * @since 2025/05/15
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class Page<T> extends PageResult<T> implements IPager<T> {

    /**
     * 页码
     */
    private transient Integer currentPage = 1;
    /**
     * 每页条数
     */
    private transient Integer pageSize = 20;
    /**
     * 是否执行count查询
     */
    private transient Boolean executeCount = true;
    public Page(List<T> rows) {
        this.setRows(rows);
    }

    public Page(int pageSize) {
        this(1, pageSize);
    }

    public Page(int currentPage, int pageSize) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public static <T> Page<T> of() {
        return new Page<T>(1, 20);
    }

    public static <T> Page<T> of(long total) {
        Page<T> page = new Page<>();
        page.setTotal(total);
        return page;
    }

    public static <T> Page<T> of(int number, int size) {
        return new Page<T>(number, size);
    }

    public static <T> Page<T> of(List<T> rows) {
        return new Page<T>(rows);
    }

    public static <T> Page<T> of(List<T> rows, long total) {
        Page<T> page = new Page<>(rows);
        page.setTotal(total);
        return page;
    }

    public Integer getOffset() {
        return PageUtil.getOffset(this.currentPage, this.pageSize);
    }

    @Override
    public <V> void set(PagerField<V> field, V value) {
        if (PagerField.TOTAL == field) {
            //设置总条数
            this.setTotal(((Number) value).longValue());
            return;
        }
        if (PagerField.RESULTS == field) {
            //设置List结果
            this.setRows((List<T>) value);
            return;
        }
        throw new RuntimeException("not support field: " + field);
    }

    @Override
    public <V> V get(PagerField<V> field) {
        if (PagerField.IS_EXECUTE_COUNT == field) {
            //返回是否执行count查询 ,isExecuteCount改成你自己的方法或字段
            return (V) this.getExecuteCount();
        }
        if (PagerField.NUMBER == field) {
            //返回页码
            return (V) this.getCurrentPage();
        }
        if (PagerField.SIZE == field) {
            //返回每页条数 ,getSize改成你自己的方法或字段
            return (V) this.getPageSize();
        }
        if (PagerField.RESULTS == field) {
            //返回每页条数 ,getSize改成你自己的方法或字段
            return (V) this.getRows();
        }
        throw new RuntimeException("not support field: " + field);
    }



}
