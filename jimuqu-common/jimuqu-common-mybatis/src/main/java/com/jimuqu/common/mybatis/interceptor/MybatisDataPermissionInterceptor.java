package com.jimuqu.common.mybatis.interceptor;

import com.jimuqu.common.mybatis.annotation.DataPermission;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * Mybatis数据权限拦截器
 * <p>
 * 在SQL执行前拦截，修改SQL语句添加数据权限条件
 * 支持查询、分页查询等操作的数据权限控制
 *
 * @author chengliang4810
 * @version 1.0
 */
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class MybatisDataPermissionInterceptor implements Interceptor {

    /**
     * 数据权限处理器
     */
    private final DataPermissionHandler dataPermissionHandler;

    public MybatisDataPermissionInterceptor() {
        this.dataPermissionHandler = new DataPermissionHandler();
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取数据权限注解
        DataPermission dataPermission = DataPermissionHolder.get();
        if (dataPermission == null) {
            // 没有数据权限注解，直接执行
            return invocation.proceed();
        }

        try {
            log.debug("开始处理数据权限");

            // 获取SQL信息
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            Object parameter = invocation.getArgs()[1];
            BoundSql boundSql = ms.getBoundSql(parameter);
            String originalSql = boundSql.getSql();

            // 生成数据权限SQL条件
            String dataPermissionCondition = dataPermissionHandler.handle(dataPermission, null);
            if (dataPermissionCondition != null && !dataPermissionCondition.trim().isEmpty()) {
                // 构建新的SQL
                String newSql = buildDataPermissionSql(originalSql, dataPermissionCondition);

                // 修改SQL
                setFieldValue(boundSql, "sql", newSql);

                log.debug("数据权限SQL: {}", newSql);
            }

            return invocation.proceed();

        } catch (Exception e) {
            log.error("处理数据权限时发生错误", e);
            return invocation.proceed();
        }
    }

    /**
     * 构建带数据权限的SQL
     */
    private String buildDataPermissionSql(String originalSql, String condition) {
        String upperSql = originalSql.toUpperCase();

        // 查找WHERE关键字
        int whereIndex = upperSql.indexOf(" WHERE ");
        if (whereIndex != -1) {
            // 已有WHERE子句，添加AND条件
            return originalSql.substring(0, whereIndex + 7) + " (" + condition + ") AND " + originalSql.substring(whereIndex + 7);
        } else {
            // 没有WHERE子句，添加WHERE条件
            // 查找GROUP BY、HAVING、ORDER BY等
            int groupByIndex = upperSql.indexOf(" GROUP BY ");
            int havingIndex = upperSql.indexOf(" HAVING ");
            int orderByIndex = upperSql.indexOf(" ORDER BY ");
            int limitIndex = upperSql.indexOf(" LIMIT ");

            int insertIndex = originalSql.length();
            if (groupByIndex != -1) {
                insertIndex = groupByIndex;
            } else if (havingIndex != -1) {
                insertIndex = havingIndex;
            } else if (orderByIndex != -1) {
                insertIndex = orderByIndex;
            } else if (limitIndex != -1) {
                insertIndex = limitIndex;
            }

            return originalSql.substring(0, insertIndex) + " WHERE " + condition + originalSql.substring(insertIndex);
        }
    }

    /**
     * 设置字段值（反射方式）
     */
    private void setFieldValue(Object object, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以配置一些属性
    }

}