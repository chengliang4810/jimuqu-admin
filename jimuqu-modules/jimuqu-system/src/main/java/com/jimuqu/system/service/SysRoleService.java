package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.domain.query.SysRoleQuery;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 角色信息Service接口
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
public interface SysRoleService {

    /**
     * 根据主键查询角色信息
     *
     * @param id 角色信息主键
     * @return {@link SysRoleVo } 角色信息视图对象
     */
    SysRoleVo queryById(Long id);

    /**
     * 查询角色信息分页列表
     *
     * @param query     查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysRoleVo }> 角色信息分页对象
     */
    Page<SysRoleVo> queryPageList(SysRoleQuery query, PageQuery pageQuery);

    /**
     * 查询角色信息列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysRoleVo }> 角色信息列表
     */
    List<SysRoleVo> queryList(SysRoleQuery query);

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRoleVo> selectRolesByUserId(Long userId);

    /** 根据用户ID查询全部可用角色，并标记已授权角色。 */
    List<SysRoleVo> selectRolesAuthByUserId(Long userId);

    /**
     * 根据用户ID查询角色权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    Set<String> selectRolePermissionByUserId(Long userId);

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    List<SysRoleVo> selectRoleAll();

    /**
     * 根据角色ID集合查询正常角色。
     */
    List<SysRoleVo> selectRoleByIds(Collection<Long> roleIds);

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    List<Long> selectRoleListByUserId(Long userId);

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    boolean checkRoleNameUnique(SysRoleBo role);

    /**
     * 校验角色权限是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    boolean checkRoleKeyUnique(SysRoleBo role);

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    void checkRoleAllowed(SysRoleBo role);

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    void checkRoleDataScope(Long roleId);

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    long countUserRoleByRoleId(Long roleId);

    /**
     * 新增角色信息
     *
     * @param bo 角色信息业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysRoleBo bo);

    /**
     * 更新角色信息
     *
     * @param bo 角色信息业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysRoleBo bo);

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态
     * @return 结果
     */
    boolean updateRoleStatus(Long roleId, String status);

    /**
     * 修改数据权限信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    int authDataScope(SysRoleBo bo);

    /**
     * 修改角色菜单权限和数据权限。
     */
    int updateRolePermission(SysRoleBo bo);

    /**
     * 批量删除角色信息
     *
     * @param ids 角色信息主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    int deleteAuthUser(SysUserRole userRole);

    /**
     * 批量取消授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    int deleteAuthUsers(Long roleId, Long[] userIds);

    /**
     * 批量选择授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    int insertAuthUsers(Long roleId, Long[] userIds);

    /**
     * 根据角色Id清除在线用户
     *
     * @param roleId 角色Id
     */
    void cleanOnlineUserByRole(Long roleId);
}
