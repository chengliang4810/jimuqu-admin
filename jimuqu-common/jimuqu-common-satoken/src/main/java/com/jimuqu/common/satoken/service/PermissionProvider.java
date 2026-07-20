package com.jimuqu.common.satoken.service;

import java.util.Set;

/**
 * 非当前登录用户权限查询接口。
 */
public interface PermissionProvider {

    /**
     * 查询用户角色权限。
     *
     * @param userId 用户ID
     * @return 角色权限
     */
    Set<String> getRolePermission(Long userId);

    /**
     * 查询用户菜单权限。
     *
     * @param userId 用户ID
     * @return 菜单权限
     */
    Set<String> getMenuPermission(Long userId);
}
