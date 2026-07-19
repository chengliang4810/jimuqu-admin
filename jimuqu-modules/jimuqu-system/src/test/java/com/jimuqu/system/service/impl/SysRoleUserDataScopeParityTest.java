package com.jimuqu.system.service.impl;

import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.system.domain.SysUser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysRoleUserDataScopeParityTest {

    @Test
    void selfScopeUsesCreatorIdLikeUpstreamDataPermissionMapping() {
        DataScopeRule selfScope = new DataScopeRule(false, Set.of(), true, 9L);

        SysUser self = user(9L, 100L, 7L);
        SysUser createdBySelf = user(10L, 9L, 7L);

        assertFalse(SysRoleServiceImpl.permitsUser(selfScope, self));
        assertTrue(SysRoleServiceImpl.permitsUser(selfScope, createdBySelf));
        assertFalse(SysUserServiceImpl.permitsUser(selfScope, self));
        assertTrue(SysUserServiceImpl.permitsUser(selfScope, createdBySelf));
    }

    private static SysUser user(Long id, Long createBy, Long deptId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setCreateBy(createBy);
        user.setDeptId(deptId);
        return user;
    }
}
