package com.jimuqu.common.mybatis.core;

import cn.xbatis.page.IPager;
import cn.xbatis.page.PageUtil;
import cn.xbatis.page.PagerField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import cn.hutool.v7.core.collection.ListUtil;

import java.util.List;



/**
 * 分页对象
 *
 * @author chengliang
 * @since 2025/05/15
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class Page<T> implements IPager<T> {

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
    /**
     * 数据列表
     */
    private List<T> items = ListUtil.zero();
    /**
     * 数据总条数
     */
    private Integer total = 0;

    public Page(List<T> items) {
        this.setItems(items);
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

    public static <T> Page<T> of(int total) {
        return new Page<T>().setTotal(total);
    }

    public static <T> Page<T> of(int number, int size) {
        return new Page<T>(number, size);
    }

    public static <T> Page<T> of(List<T> items) {
        return new Page<T>(items);
    }

    public static <T> Page<T> of(List<T> items, Integer total) {
        return new Page<T>(items).setTotal(total);
    }

    public Integer getOffset() {
        return PageUtil.getOffset(this.currentPage, this.pageSize);
    }

    @Override
    public <V> void set(PagerField<V> field, V value) {
        if (PagerField.TOTAL == field) {
            //设置总条数
            this.setTotal((Integer) value);
            return;
        }
        if (PagerField.RESULTS == field) {
            //设置List结果
            this.setItems((List<T>) value);
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
        throw new RuntimeException("not support field: " + field);
    }



}
