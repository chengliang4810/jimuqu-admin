package com.jimuqu.system.autotable;

import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMenuMapper;
import com.jimuqu.system.mapper.SysUserPostMapper;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssociationTableUpgradeCallbackTest {

    private boolean dataSourceSet;

    @AfterEach
    void cleanDataSource() {
        if (dataSourceSet) {
            DataSourceManager.cleanDataSource();
        }
    }

    @Test
    void skipsReadingRowsWhenCompositePrimaryKeyAndNotNullColumnsAreCorrect() throws Exception {
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        AssociationTableUpgradeCallback callback = callback(userRoleMapper);
        useMetadata(true);

        callback.before(SysUserRole.class);

        verifyNoInteractions(userRoleMapper);
    }

    @Test
    void normalizesRowsOnlyWhenAssociationTableStructureNeedsUpgrade() throws Exception {
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        AssociationTableUpgradeCallback callback = callback(userRoleMapper);
        useMetadata(false);
        SysUserRole valid = association(1L, 2L);
        when(userRoleMapper.listAll()).thenReturn(
                Arrays.asList(null, association(1L, null), valid, association(1L, 2L)));

        callback.before(SysUserRole.class);

        verify(userRoleMapper).listAll();
        verify(userRoleMapper).deleteAll();
        verify(userRoleMapper).saveBatch(List.of(valid));
    }

    @Test
    void normalizeRowsDropsNullInvalidAndDuplicateAssociationRows() {
        SysUserRole valid = association(1L, 2L);

        List<SysUserRole> normalized = AssociationTableUpgradeCallback.normalizeRows(
                Arrays.asList(null, association(1L, null), valid, association(1L, 2L)),
                row -> row.getUserId() != null && row.getRoleId() != null);

        assertEquals(List.of(valid), normalized);
    }

    private SysUserRole association(Long userId, Long roleId) {
        SysUserRole association = new SysUserRole();
        association.setUserId(userId);
        association.setRoleId(roleId);
        return association;
    }

    private AssociationTableUpgradeCallback callback(SysUserRoleMapper userRoleMapper) {
        return new AssociationTableUpgradeCallback(
                mock(SysRoleDeptMapper.class),
                mock(SysRoleMenuMapper.class),
                mock(SysUserPostMapper.class),
                userRoleMapper);
    }

    private void useMetadata(boolean allColumnsNotNull) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("jimuqu_test");
        when(connection.getMetaData()).thenReturn(metadata);

        ResultSet tables = mock(ResultSet.class);
        when(tables.next()).thenReturn(true, false);
        when(metadata.getTables(eq("jimuqu_test"), eq(null), eq("sys_user_role"), any(String[].class)))
                .thenReturn(tables);

        ResultSet primaryKeys = mock(ResultSet.class);
        when(primaryKeys.next()).thenReturn(true, true, false);
        when(primaryKeys.getShort("KEY_SEQ")).thenReturn((short) 1, (short) 2);
        when(primaryKeys.getString("COLUMN_NAME")).thenReturn("user_id", "role_id");
        when(metadata.getPrimaryKeys("jimuqu_test", null, "sys_user_role")).thenReturn(primaryKeys);

        ResultSet columns = mock(ResultSet.class);
        when(columns.next()).thenReturn(true, true, false);
        when(columns.getString("COLUMN_NAME")).thenReturn("user_id", "role_id");
        when(columns.getInt("NULLABLE")).thenReturn(
                DatabaseMetaData.columnNoNulls,
                allColumnsNotNull ? DatabaseMetaData.columnNoNulls : DatabaseMetaData.columnNullable);
        when(metadata.getColumns("jimuqu_test", null, "sys_user_role", null)).thenReturn(columns);

        DataSourceManager.setDataSource(dataSource);
        dataSourceSet = true;
    }
}
