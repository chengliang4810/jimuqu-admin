package com.jimuqu.system.service.impl;

import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.mapper.SysUserPostMapper;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SysUserRelationUpdateParityTest {

    @Test
    void omittedRelationsPreserveExistingAssignments() throws Exception {
        SysUserPostMapper userPostMapper = mock(SysUserPostMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserServiceImpl service = new SysUserServiceImpl(
                null, null, null, userPostMapper, userRoleMapper,
                null, null, null, null, null);
        Method posts = SysUserServiceImpl.class.getDeclaredMethod(
                "insertUserPost", SysUserBo.class, boolean.class);
        Method roles = SysUserServiceImpl.class.getDeclaredMethod(
                "insertUserRole", Long.class, List.class, boolean.class);
        posts.setAccessible(true);
        roles.setAccessible(true);

        posts.invoke(service, new SysUserBo(42L), true);
        roles.invoke(service, 42L, null, true);
        verifyNoInteractions(userPostMapper, userRoleMapper);
    }
}
