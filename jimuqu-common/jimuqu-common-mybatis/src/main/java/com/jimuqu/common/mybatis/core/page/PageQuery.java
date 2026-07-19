package com.jimuqu.common.mybatis.core.page;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.utils.sql.SqlUtil;
import com.jimuqu.common.mybatis.core.Page;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import db.sql.api.impl.cmd.Methods;
import db.sql.api.impl.cmd.basic.OrderByDirection;
import db.sql.api.impl.cmd.struct.query.OrderBy;
import db.sql.api.impl.cmd.struct.query.OrderByValue;
import lombok.Data;
import cn.hutool.v7.core.util.ObjUtil;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 分页查询实体类
 *
 * @author Lion Li,chengliang4810
 */

@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数
     */
    private Integer pageNum;
    /**
     * 分页大小
     */
    private Integer pageSize;
    /**
     * 排序列
     */
    private String orderByColumn;
    /**
     * 排序的方向desc或者asc
     */
    private String isAsc;
    /**
     * 当前记录起始索引 默认值
     */
    public static final int DEFAULT_PAGE_NUM = 1;
    /**
     * 每页显示记录数 默认值 默认查全部
     */
    public static final int DEFAULT_PAGE_SIZE = Integer.MAX_VALUE;

    public <T> Page<T> build() {
        Integer pageNum = ObjUtil.defaultIfNull(getPageNum(), DEFAULT_PAGE_NUM);
        Integer pageSize = ObjUtil.defaultIfNull(getPageSize(), DEFAULT_PAGE_SIZE);
        if (pageNum <= 0) {
            pageNum = DEFAULT_PAGE_NUM;
        }
        return Page.of(pageNum, pageSize);
    }

    /**
     * 构建排序
     * <p>
     * 支持的用法如下:
     * {isAsc:"asc",orderByColumn:"id"} order by id asc
     * {isAsc:"asc",orderByColumn:"id,createTime"} order by id asc,create_time asc
     * {isAsc:"desc",orderByColumn:"id,createTime"} order by id desc,create_time desc
     * {isAsc:"asc,desc",orderByColumn:"id,createTime"} order by id asc,create_time desc
     */
    public OrderBy[] buildOrderBy() {
        if (StringUtil.isBlank(orderByColumn) || StringUtil.isBlank(isAsc)) {
            return new OrderBy[]{};
        }

        String orderBy = SqlUtil.escapeOrderBySql(orderByColumn);
        orderBy = StringUtil.toUnderScoreCase(orderBy);

        // 兼容前端排序类型
        String orderDirection = StringUtil.replace(isAsc, "ascending", "asc");
        orderDirection = StringUtil.replace(orderDirection, "descending", "desc");

        String[] orderByArr = orderBy.split(StringUtil.SEPARATOR);
        String[] isAscArr = orderDirection.split(StringUtil.SEPARATOR);
        if (isAscArr.length != 1 && isAscArr.length != orderByArr.length) {
            throw new ServiceException("排序参数有误" );
        }
        OrderBy[] orderBys = new OrderBy[orderByArr.length];
        // 每个字段各自排序
        for (int i = 0; i < orderByArr.length; i++) {
            String orderByStr = orderByArr[i];
            String isAscStr = isAscArr.length == 1 ? isAscArr[0] : isAscArr[i];
            if ("asc".equals(isAscStr)) {
                orderBys[i] = new OrderBy().orderBy(OrderByDirection.ASC,  Methods.column(orderByStr));
            } else if ("desc".equals(isAscStr)) {
                orderBys[i] =  new OrderBy().orderBy(OrderByDirection.DESC, Methods.column(orderByStr));
            } else {
                throw new ServiceException("排序参数有误" );
            }
        }
        return orderBys;
    }

    /**
     * 将分页排序放到查询默认排序之前，与上游 Page orders 的优先级一致。
     */
    public <T> QueryChain<T> applyOrder(QueryChain<T> queryChain) {
        OrderBy[] requested = buildOrderBy();
        if (requested.length == 0) {
            return queryChain;
        }

        return prependOrder(queryChain, requested);
    }

    /**
     * 动态排序缺失时应用指定默认排序；存在时不再追加默认排序。
     */
    public <T> QueryChain<T> applyOrder(QueryChain<T> queryChain,
                                        Consumer<QueryChain<T>> defaultOrder) {
        OrderBy[] requested = buildOrderBy();
        if (requested.length == 0) {
            defaultOrder.accept(queryChain);
            return queryChain;
        }
        return prependOrder(queryChain, requested);
    }

    private <T> QueryChain<T> prependOrder(QueryChain<T> queryChain, OrderBy[] requested) {
        List<OrderByValue> values = queryChain.$orderBy().getOrderByField();
        List<OrderByValue> defaults = new ArrayList<>(values);
        values.clear();
        for (OrderBy orderBy : requested) {
            values.addAll(orderBy.getOrderByField());
        }
        values.addAll(defaults);
        return queryChain;
    }


}
