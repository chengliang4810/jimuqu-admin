package com.jimuqu.auth.service;

import com.jimuqu.common.mybatis.model.DataScopeWriteRule;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysLoginDataScopeUpdateTest {

    @Test
    void loginInfoUpdateAlwaysUsesExplicitAllScope() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysLoginService service = service(userMapper);
        when(userMapper.updateWithDataScope(any(SysUser.class), any(DataScopeWriteRule.class))).thenReturn(1);

        service.recordLoginInfo(9L, "127.0.0.1");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        ArgumentCaptor<DataScopeWriteRule> ruleCaptor = ArgumentCaptor.forClass(DataScopeWriteRule.class);
        verify(userMapper).updateWithDataScope(userCaptor.capture(), ruleCaptor.capture());
        assertTrue(ruleCaptor.getValue().allAccess());
        assertEquals(9L, userCaptor.getValue().getId());
        assertEquals("127.0.0.1", userCaptor.getValue().getLoginIp());
    }

    private static SysLoginService service(SysUserMapper userMapper) {
        return new SysLoginService(null, null, userMapper, null, null, null);
    }
}
