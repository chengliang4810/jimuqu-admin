package com.jimuqu.common.mybatis.core.page;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.exception.ServiceException;
import db.sql.api.DbType;
import db.sql.api.SQLMode;
import db.sql.api.SqlBuilderContext;
import db.sql.api.impl.cmd.Methods;
import db.sql.api.impl.cmd.basic.OrderByDirection;
import db.sql.api.impl.cmd.struct.query.OrderBy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageQueryTest {

    @Test
    void buildsCamelCaseAndDirectionAliases() {
        PageQuery query = query("createTime,userName", "ascending,descending");

        String sql = render(query.buildOrderBy());

        assertTrue(sql.contains("create_time ASC"), sql);
        assertTrue(sql.contains("user_name DESC"), sql);
        assertTrue(sql.indexOf("create_time") < sql.indexOf("user_name"), sql);
        assertEquals("ascending,descending", query.getIsAsc());
    }

    @Test
    void appliesOneDirectionToMultipleColumns() {
        String sql = render(query("createTime,userName", "desc").buildOrderBy());

        assertTrue(sql.contains("create_time DESC"), sql);
        assertTrue(sql.contains("user_name DESC"), sql);
    }

    @Test
    void leavesDefaultOrderUntouchedWhenSortIsEmpty() {
        QueryChain<Object> chain = QueryChain.create();
        chain.$orderBy().orderBy(OrderByDirection.ASC, Methods.column("id"));

        query(null, null).applyOrder(chain);

        assertEquals(1, chain.$orderBy().getOrderByField().size());
        assertTrue(render(chain.$orderBy()).endsWith("id ASC"));
    }

    @Test
    void putsPageOrderBeforeQueryDefaultOrder() {
        QueryChain<Object> chain = QueryChain.create();
        chain.$orderBy().orderBy(OrderByDirection.ASC, Methods.column("id"));

        query("createTime", "desc").applyOrder(chain);
        String sql = render(chain.$orderBy());

        assertTrue(sql.indexOf("create_time DESC") < sql.indexOf("id ASC"), sql);
    }

    @Test
    void usesFallbackOnlyWhenPageOrderIsAbsent() {
        QueryChain<Object> empty = QueryChain.create();
        query(null, null).applyOrder(empty,
                chain -> chain.$orderBy().orderBy(OrderByDirection.DESC, Methods.column("id")));
        assertTrue(render(empty.$orderBy()).contains("id DESC"));

        QueryChain<Object> sorted = QueryChain.create();
        query("createTime", "asc").applyOrder(sorted,
                chain -> chain.$orderBy().orderBy(OrderByDirection.DESC, Methods.column("id")));
        String sql = render(sorted.$orderBy());
        assertTrue(sql.contains("create_time ASC"), sql);
        assertTrue(!sql.contains("id DESC"), sql);
    }

    @Test
    void rejectsInvalidDirectionAndSqlInjectionCharacters() {
        assertThrows(ServiceException.class, () -> query("id", "sideways").buildOrderBy());
        assertThrows(IllegalArgumentException.class,
                () -> query("id;drop table sys_user", "asc").buildOrderBy());
    }

    private static PageQuery query(String columns, String direction) {
        PageQuery query = new PageQuery();
        query.setOrderByColumn(columns);
        query.setIsAsc(direction);
        return query;
    }

    private static String render(OrderBy... orderBys) {
        OrderBy combined = new OrderBy();
        for (OrderBy orderBy : orderBys) {
            combined.getOrderByField().addAll(orderBy.getOrderByField());
        }
        return combined.sql(null, null,
                new SqlBuilderContext(DbType.MYSQL, SQLMode.PRINT), new StringBuilder()).toString();
    }
}
