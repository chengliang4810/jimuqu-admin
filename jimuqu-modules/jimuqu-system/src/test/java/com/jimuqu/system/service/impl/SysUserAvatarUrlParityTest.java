package com.jimuqu.system.service.impl;

import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.SysConfigMapper;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysPostMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.mapper.SysUserPostMapper;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import com.jimuqu.system.service.SysFileService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserAvatarUrlParityTest {

    @Test
    void avatarUsesUnifiedOssAccessUrl() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysFileService fileService = mock(SysFileService.class);
        SysUserVo user = new SysUserVo().setId(7L).setAvatar(42L);
        when(userMapper.getVoById(7L)).thenReturn(user);
        when(roleMapper.selectRolesByUserId(7L)).thenReturn(List.of());
        when(fileService.selectUrlByIds("42")).thenReturn("https://signed.example/avatar.png");
        SysUserServiceImpl service = new SysUserServiceImpl(
                userMapper, mock(SysDeptMapper.class), roleMapper,
                mock(SysUserPostMapper.class), mock(SysUserRoleMapper.class),
                mock(SysPostMapper.class), fileService, mock(SysConfigMapper.class),
                mock(ISysDataScopeService.class), mock(SysUserImportRowTransactionExecutor.class));

        assertEquals("https://signed.example/avatar.png", service.queryById(7L).getAvatarUrl());
        verify(fileService).selectUrlByIds("42");
    }
}
