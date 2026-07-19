package com.jimuqu.system.service;

import com.jimuqu.common.core.domain.dto.RoleDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户权限处理
 *
 * @author Lion Li,chengliang4810
 */
public interface ISysPermissionService {

    /**
     * 获取角色数据权限
     *
     * @param userId  用户id
     * @return 角色权限信息
     */
    Set<String> getRolePermission(Long userId);

    /**
     * 获取菜单数据权限
     *
     * @param userId  用户id
     * @return 菜单权限信息
     */
    Set<String> getMenuPermission(Long userId);

    /**
     * 按权限标识汇总具备数据权限的角色集合。
     *
     * @param roles 角色列表
     * @return key 为权限标识，value 为拥有该权限的角色ID列表
     */
    Map<String, List<Long>> getDataScopeRoleMap(List<RoleDTO> roles);

}
