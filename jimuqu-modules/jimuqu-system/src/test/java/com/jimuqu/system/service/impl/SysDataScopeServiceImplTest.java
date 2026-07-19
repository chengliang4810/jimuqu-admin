package com.jimuqu.system.service.impl;

import com.jimuqu.common.mybatis.model.DataScopeAccess;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeWriteRule;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysMenuMapper;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SysDataScopeServiceImplTest {

    private SysRoleMapper roleMapper;
    private SysMenuMapper menuMapper;
    private SysDataScopeServiceImpl service;

    @BeforeEach
    void setUp() {
        roleMapper = mock(SysRoleMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser user = new SysUser();
        user.setId(9L);
        user.setDeptId(103L);
        when(userMapper.getById(9L)).thenReturn(user);
        service = spy(new SysDataScopeServiceImpl(
                roleMapper,
                mock(SysDeptMapper.class),
                mock(SysRoleDeptMapper.class),
                userMapper,
                menuMapper));
    }

    @Test
    void roleWithoutCurrentPermissionCannotWidenScope() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(
                role(2L, "broad", "1"),
                role(3L, "narrow", "5")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenReturn(List.of("system:role:list"));
        when(menuMapper.selectMenuPermsByRoleId(3L)).thenReturn(List.of("system:user:list"));

        DataScopeRule rule = service.resolveUserDataScope(
                9L, DataScopeAccess.ofPermissions("system:user:list"));

        assertFalse(rule.allAccess());
        assertTrue(rule.selfAccess());
        assertTrue(rule.permits(9L, 999L));
        assertFalse(rule.permits(10L, 103L));
    }

    @Test
    void matchingRolesAreCombinedAsUnion() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(
                role(2L, "dept", "3"),
                role(3L, "self", "5")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenReturn(List.of("system:user:list"));
        when(menuMapper.selectMenuPermsByRoleId(3L)).thenReturn(List.of("system:user:list"));

        DataScopeRule rule = service.resolveUserDataScope(
                9L, DataScopeAccess.ofPermissions("system:user:list"));

        assertEquals(List.of(103L), rule.departmentIds().stream().toList());
        assertTrue(rule.selfAccess());
        assertTrue(rule.permits(10L, 103L));
        assertTrue(rule.permits(9L, 999L));
    }

    @Test
    void noMatchingRoleFailsClosed() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(role(2L, "broad", "1")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenReturn(List.of("system:role:list"));

        DataScopeRule rule = service.resolveUserDataScope(
                9L, DataScopeAccess.ofPermissions("system:user:list"));

        assertTrue(rule.denyAll());
    }

    @Test
    void permissionLookupFailureFailsClosed() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(role(2L, "broad", "1")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenThrow(new IllegalStateException("database unavailable"));

        DataScopeRule rule = service.resolveUserDataScope(
                9L, DataScopeAccess.ofPermissions("system:user:list"));

        assertTrue(rule.denyAll());
    }

    @Test
    void combinedDepartmentAndSelfScopeSurvivesPermissionFiltering() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(role(2L, "combined", "6")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenReturn(List.of("system:user:list"));
        doReturn(List.of(103L, 104L)).when(service).getDeptAndChild(103L);

        DataScopeRule rule = service.resolveUserDataScope(
                9L, DataScopeAccess.ofPermissions("system:user:list"));

        assertEquals(List.of(103L, 104L), rule.departmentIds().stream().toList());
        assertTrue(rule.selfAccess());
        assertTrue(rule.permits(10L, 104L));
        assertTrue(rule.permits(9L, 999L));
    }

    @Test
    void matchingRolesUseIntersectionForWrite() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(
                role(2L, "dept", "3"),
                role(3L, "self", "5")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenReturn(List.of("system:user:edit"));
        when(menuMapper.selectMenuPermsByRoleId(3L)).thenReturn(List.of("system:user:edit"));
        DataScopeAccess access = DataScopeAccess.ofPermissions("system:user:edit");
        DataScopeWriteRule writeRule = service.resolveUserWriteDataScope(9L, access);

        assertEquals(2, writeRule.roleRules().size());
        assertFalse(writeRule.permits(10L, 103L));
        assertFalse(writeRule.permits(9L, 999L));
        assertTrue(writeRule.permits(9L, 103L));
    }

    @Test
    void noMatchingRoleFailsClosedForWrite() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(role(2L, "dept", "3")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenReturn(List.of("system:user:list"));

        DataScopeWriteRule writeRule = service.resolveUserWriteDataScope(
                9L, DataScopeAccess.ofPermissions("system:user:edit"));

        assertTrue(writeRule.denyAll());
        assertFalse(writeRule.permits(9L, 103L));
    }

    @Test
    void allRoleKeepsUpstreamWriteShortCircuitAfterPermissionFiltering() {
        when(roleMapper.selectRolesByUserId(9L)).thenReturn(List.of(
                role(2L, "self", "5"),
                role(3L, "all", "1")));
        when(menuMapper.selectMenuPermsByRoleId(2L)).thenReturn(List.of("system:user:edit"));
        when(menuMapper.selectMenuPermsByRoleId(3L)).thenReturn(List.of("system:user:edit"));

        DataScopeWriteRule writeRule = service.resolveUserWriteDataScope(
                9L, DataScopeAccess.ofPermissions("system:user:edit"));

        assertTrue(writeRule.allAccess());
        assertTrue(writeRule.permits(10L, 999L));
    }

    private SysRoleVo role(Long id, String roleKey, String dataScope) {
        SysRoleVo role = new SysRoleVo();
        role.setId(id);
        role.setRoleKey(roleKey);
        role.setDataScope(dataScope);
        role.setStatus("0");
        return role;
    }
}
