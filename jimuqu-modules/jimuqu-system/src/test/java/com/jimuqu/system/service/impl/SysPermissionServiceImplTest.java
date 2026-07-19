package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.system.mapper.SysMenuMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysPermissionServiceImplTest {

    @Test
    void mapsSplitPermissionsToEveryContributingRole() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysPermissionServiceImpl service = new SysPermissionServiceImpl(null, null, menuMapper);
        when(menuMapper.selectMenuPermsByRoleId(1L))
                .thenReturn(List.of("system:user:list,system:user:query", "system:user:list"));
        when(menuMapper.selectMenuPermsByRoleId(2L))
                .thenReturn(List.of("system:user:list"));

        Map<String, List<Long>> result = service.getDataScopeRoleMap(List.of(role(1L), role(2L)));

        assertEquals(List.of(1L, 2L), result.get("system:user:list"));
        assertEquals(List.of(1L), result.get("system:user:query"));
    }

    private static RoleDTO role(Long roleId) {
        RoleDTO role = new RoleDTO();
        role.setRoleId(roleId);
        return role;
    }
}
