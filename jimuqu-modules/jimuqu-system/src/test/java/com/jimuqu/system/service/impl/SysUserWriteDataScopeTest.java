package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeWriteRule;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SysUserWriteDataScopeTest {

    @Test
    void internalUpdateWithoutLoginUserBypassesDataScope() {
        ISysDataScopeService dataScopeService = mock(ISysDataScopeService.class);
        SysUserServiceImpl service = service(mock(SysUserMapper.class), dataScopeService);

        DataScopeWriteRule writeRule = service.resolveWriteDataScope(null);

        assertTrue(writeRule.allAccess());
        verifyNoInteractions(dataScopeService);
    }

    @Test
    void loggedInUpdateResolvesCurrentUsersWriteRule() {
        ISysDataScopeService dataScopeService = mock(ISysDataScopeService.class);
        SysUserServiceImpl service = service(mock(SysUserMapper.class), dataScopeService);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        DataScopeWriteRule expected = DataScopeWriteRule.of(List.of(
                new DataScopeRule(false, Set.of(103L), false, 9L)));
        when(dataScopeService.resolveUserWriteDataScope(9L)).thenReturn(expected);

        DataScopeWriteRule actual = service.resolveWriteDataScope(loginUser);

        assertSame(expected, actual);
        verify(dataScopeService).resolveUserWriteDataScope(9L);
    }

    @Test
    void updateAddsRoleIntersectionAndInnerUnionToTypedWhere() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserServiceImpl service = service(userMapper, mock(ISysDataScopeService.class));
        SysUser update = new SysUser().setId(42L).setStatus("1");
        DataScopeWriteRule writeRule = DataScopeWriteRule.of(List.of(
                new DataScopeRule(false, Set.of(103L), true, 9L),
                new DataScopeRule(false, Set.of(103L, 104L), false, 9L)));
        doReturn(1).when(userMapper).updateWithDataScope(same(update), same(writeRule));

        int rows = service.updateUserWithDataScope(update, writeRule);

        verify(userMapper).updateWithDataScope(same(update), same(writeRule));
        cn.xbatis.core.sql.executor.Where where = cn.xbatis.core.sql.executor.Where.create();
        where.setDbType(db.sql.api.DbType.MYSQL);
        where.eq(SysUser::getId, update.getId());
        SysUserMapper.applyWriteDataScope(where, writeRule);
        String script = where.getWhereScript();
        List<Object> params = where.getWhereScriptParams();
        assertEquals(1, rows);
        assertTrue(script.contains(" AND "), script);
        assertTrue(script.contains(" OR "), script);
        assertTrue(params.containsAll(List.of(42L, 103L, 104L, 9L)), params.toString());
    }

    @Test
    void profileUpdateAlwaysUsesExplicitAllScope() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ISysDataScopeService dataScopeService = mock(ISysDataScopeService.class);
        SysUserServiceImpl service = service(userMapper, dataScopeService);
        when(userMapper.updateWithDataScope(any(SysUser.class), any(DataScopeWriteRule.class)))
                .thenReturn(1);

        int rows = service.updateUserProfile(new SysUserBo().setId(42L).setNickName("本人"));

        ArgumentCaptor<DataScopeWriteRule> ruleCaptor =
                ArgumentCaptor.forClass(DataScopeWriteRule.class);
        verify(userMapper).updateWithDataScope(any(SysUser.class), ruleCaptor.capture());
        assertEquals(1, rows);
        assertTrue(ruleCaptor.getValue().allAccess());
        verifyNoInteractions(dataScopeService);
    }

    @Test
    void ownPasswordUpdateAlwaysUsesExplicitAllScope() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ISysDataScopeService dataScopeService = mock(ISysDataScopeService.class);
        SysUserServiceImpl service = service(userMapper, dataScopeService);
        when(userMapper.updateWithDataScope(any(SysUser.class), any(DataScopeWriteRule.class)))
                .thenReturn(1);

        boolean updated = service.resetOwnUserPwd(42L, "hashed-password");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        ArgumentCaptor<DataScopeWriteRule> ruleCaptor =
                ArgumentCaptor.forClass(DataScopeWriteRule.class);
        verify(userMapper).updateWithDataScope(userCaptor.capture(), ruleCaptor.capture());
        assertTrue(updated);
        assertEquals(42L, userCaptor.getValue().getId());
        assertEquals("hashed-password", userCaptor.getValue().getPassword());
        assertTrue(ruleCaptor.getValue().allAccess());
        verifyNoInteractions(dataScopeService);
    }

    private static SysUserServiceImpl service(
            SysUserMapper userMapper, ISysDataScopeService dataScopeService) {
        return new SysUserServiceImpl(
                userMapper, null, null, null, null,
                null, null, null, dataScopeService, null);
    }
}
