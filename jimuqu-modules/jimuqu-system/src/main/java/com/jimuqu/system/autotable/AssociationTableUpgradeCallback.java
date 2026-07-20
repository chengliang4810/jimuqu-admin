package com.jimuqu.system.autotable;

import cn.xbatis.db.annotations.Table;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.SysRoleMenu;
import com.jimuqu.system.domain.SysUserPost;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMenuMapper;
import com.jimuqu.system.mapper.SysUserPostMapper;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.callback.RunBeforeCallback;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * 兼容早期版本未声明复合主键的关联表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssociationTableUpgradeCallback implements RunBeforeCallback {

    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    @Transaction
    public void before(Class<?> tableClass) {
        if (tableClass == SysRoleDept.class) {
            normalize(SysRoleDept.class, roleDeptMapper,
                    List.of("role_id", "dept_id"),
                    row -> row.getRoleId() != null && row.getDeptId() != null);
        } else if (tableClass == SysRoleMenu.class) {
            normalize(SysRoleMenu.class, roleMenuMapper,
                    List.of("role_id", "menu_id"),
                    row -> row.getRoleId() != null && row.getMenuId() != null);
        } else if (tableClass == SysUserPost.class) {
            normalize(SysUserPost.class, userPostMapper,
                    List.of("user_id", "post_id"),
                    row -> row.getUserId() != null && row.getPostId() != null);
        } else if (tableClass == SysUserRole.class) {
            normalize(SysUserRole.class, userRoleMapper,
                    List.of("user_id", "role_id"),
                    row -> row.getUserId() != null && row.getRoleId() != null);
        }
    }

    private <T> void normalize(Class<T> entityType, BaseMapperPlus<T, T> mapper,
                               List<String> keyColumns, Predicate<T> validRow) {
        if (!requiresUpgrade(entityType, keyColumns)) {
            return;
        }
        List<T> rows = mapper.listAll();
        List<T> normalizedRows = normalizeRows(rows, validRow);
        if (normalizedRows.size() == rows.size()) {
            return;
        }
        mapper.deleteAll();
        if (!normalizedRows.isEmpty()) {
            mapper.saveBatch(normalizedRows);
        }
        log.info("AutoTable 升级前已规范化关联表 {}：{} -> {}", tableName(entityType), rows.size(), normalizedRows.size());
    }

    static <T> List<T> normalizeRows(List<T> rows, Predicate<T> validRow) {
        return rows.stream().filter(Objects::nonNull).filter(validRow).distinct().toList();
    }

    private boolean requiresUpgrade(Class<?> entityType, List<String> keyColumns) {
        String tableName = tableName(entityType);
        return DataSourceManager.useConnection(connection -> {
            try {
                DatabaseMetaData metadata = connection.getMetaData();
                String catalog = connection.getCatalog();
                return tableExists(metadata, catalog, tableName)
                        && (!hasExpectedPrimaryKey(metadata, catalog, tableName, keyColumns)
                        || !hasNotNullColumns(metadata, catalog, tableName, keyColumns));
            } catch (SQLException e) {
                throw new IllegalStateException("检查关联表结构失败：" + tableName, e);
            }
        });
    }

    private boolean tableExists(DatabaseMetaData metadata, String catalog, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private boolean hasExpectedPrimaryKey(DatabaseMetaData metadata, String catalog, String tableName,
                                          List<String> keyColumns) throws SQLException {
        TreeMap<Short, String> primaryKey = new TreeMap<>();
        try (ResultSet columns = metadata.getPrimaryKeys(catalog, null, tableName)) {
            while (columns.next()) {
                primaryKey.put(columns.getShort("KEY_SEQ"), normalizeColumnName(columns.getString("COLUMN_NAME")));
            }
        }
        return List.copyOf(primaryKey.values()).equals(keyColumns);
    }

    private boolean hasNotNullColumns(DatabaseMetaData metadata, String catalog, String tableName,
                                      List<String> keyColumns) throws SQLException {
        Set<String> expectedColumns = new HashSet<>(keyColumns);
        Set<String> notNullColumns = new HashSet<>();
        try (ResultSet columns = metadata.getColumns(catalog, null, tableName, null)) {
            while (columns.next()) {
                String columnName = normalizeColumnName(columns.getString("COLUMN_NAME"));
                if (expectedColumns.contains(columnName)
                        && columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls) {
                    notNullColumns.add(columnName);
                }
            }
        }
        return notNullColumns.equals(expectedColumns);
    }

    private String normalizeColumnName(String columnName) {
        return columnName == null ? "" : columnName.toLowerCase(Locale.ROOT);
    }

    private String tableName(Class<?> entityType) {
        return entityType.getAnnotation(Table.class).value();
    }
}
