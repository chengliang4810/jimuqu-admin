package com.jimuqu.system.service;

import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.bo.SysMenuBo;
import com.jimuqu.system.domain.query.SysMenuQuery;
import com.jimuqu.system.domain.vo.RouterVo;
import com.jimuqu.system.domain.vo.SysMenuVo;
import cn.hutool.v7.core.tree.MapTree;

import java.util.List;
import java.util.Set;

/**
 * 菜单权限Service接口
 *
 * @author chengliang4810
 * @since 2025-06-06
 */
public interface SysMenuService {

    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenuVo> queryList(Long userId);

    /**
     * 查询菜单权限列表
     *
     * @param query 查询条件对象
     * @param userId 用户ID
     * @return {@link List }<{@link SysMenuVo }> 菜单权限列表
     */
    List<SysMenuVo> queryList(SysMenuQuery query, Long userId);

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    Set<String> queryMenuPermsByUserId(Long userId);

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    Set<String> queryMenuPermsByRoleId(Long roleId);

    /**
     * 根据用户ID查询菜单树信息
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> queryMenuTreeByUserId(Long userId);

    /**
     * 根据用户ID查询菜单信息
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> queryMenuByUserId(Long userId);

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    List<Long> queryMenuListByRoleId(Long roleId);

    /**
     * 构建前端路由所需要的菜单
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    List<RouterVo> buildMenus(List<SysMenu> menus);

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    List<MapTree<Long>> buildMenuTreeSelect(List<SysMenuVo> menus);

    /**
     * 根据主键查询菜单权限
     *
     * @param id 菜单权限主键
     * @return {@link SysMenuVo } 菜单权限视图对象
     */
   SysMenuVo queryById(Long id);

    /**
     * 是否存在菜单子节点
     *
     * @param menuIdList 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    boolean hasChildByMenuId(List<Long> menuIdList);

    /**
     * 查询菜单是否存在角色
     *
     * @param menuId 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    boolean checkMenuExistRole(Long menuId);

    /**
     * 新增菜单权限
     *
     * @param bo 菜单权限业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysMenuBo bo);

    /**
     * 更新菜单权限
     *
     * @param bo 菜单权限业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysMenuBo bo);

    /**
     * 删除代码生成模板信息
     *
     * @param menuIdList 菜单ID
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteById(List<Long> menuIdList);

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    boolean checkMenuNameUnique(SysMenuBo menu);
}
