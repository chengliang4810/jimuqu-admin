package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.system.mapper.SysMenuMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.service.ISysPermissionService;
import com.jimuqu.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements ISysPermissionService {

    private final SysRoleMapper roleMapper;
    private final SysMenuService menuService;
    private final SysMenuMapper menuMapper;

    @Override
    public Set<String> getRolePermission(Long userId) {
        if (UserConstants.SUPER_ADMIN_ID.equals(userId)) {
            return Set.of(GlobalConstants.SUPER_ADMIN_ROLE_KEY);
        }
        return roleMapper.selectRolesByUserId(userId).stream()
                .filter(role -> UserConstants.ROLE_NORMAL.equals(role.getStatus()))
                .map(role -> role.getRoleKey().trim())
                .filter(role -> !role.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getMenuPermission(Long userId) {
        if (UserConstants.SUPER_ADMIN_ID.equals(userId)) {
            return Set.of("*:*:*");
        }
        return menuService.queryMenuPermsByUserId(userId);
    }

    @Override
    public Map<String, List<Long>> getDataScopeRoleMap(List<RoleDTO> roles) {
        if (roles == null || roles.isEmpty()) {
            return Map.of();
        }
        Map<String, List<Long>> rolePermsMap = new LinkedHashMap<>();
        for (RoleDTO role : roles) {
            if (role == null || role.getRoleId() == null) {
                continue;
            }
            Set<String> rolePermissions = new LinkedHashSet<>();
            for (String value : menuMapper.selectMenuPermsByRoleId(role.getRoleId())) {
                if (StringUtil.isBlank(value)) {
                    continue;
                }
                rolePermissions.addAll(StringUtil.splitList(value.trim()));
            }
            for (String permission : rolePermissions) {
                rolePermsMap.computeIfAbsent(permission, key -> new ArrayList<>())
                        .add(role.getRoleId());
            }
        }
        return rolePermsMap;
    }
}
