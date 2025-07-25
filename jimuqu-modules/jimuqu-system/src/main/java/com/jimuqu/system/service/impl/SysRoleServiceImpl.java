package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.query.SysRoleQuery;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * 角色信息Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    /**
     * 查询角色信息
     */
    @Override
    public SysRoleVo queryById(Long id) {
        return sysRoleMapper.getVoById(id);
    }

    /**
     * 查询角色信息分页列表
     */
    @Override
    public Page<SysRoleVo> queryPageList(SysRoleQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysRoleVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询角色信息列表
     */
    @Override
    public List<SysRoleVo> queryList(SysRoleQuery query) {
        QueryChain<SysRole> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysRoleVo.class).list();
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysRole> buildQueryChain(SysRoleQuery query) {
        return QueryChain.of(sysRoleMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 新增角色信息
     */
    @Override
    public Boolean insertByBo(SysRoleBo bo) {
        SysRole sysRole = MapstructUtil.convert(bo, SysRole.class);
        boolean flag = sysRoleMapper.save(sysRole) > 0;
        bo.setId(sysRole.getId());
        return flag;
    }

    /**
     * 修改角色信息
     */
    @Override
    public Boolean updateByBo(SysRoleBo bo) {
        SysRole sysRole = MapstructUtil.convert(bo, SysRole.class);
        return sysRoleMapper.update(sysRole) > 0;
    }

    /**
     * 批量删除角色信息
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysRoleMapper.deleteByIds(ids);
    }

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRolesByUserId(Long userId) {
        return List.of();
    }

    /**
     * 根据用户ID查询角色权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        return Set.of();
    }

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    @Override
    public List<SysRoleVo> selectRoleAll() {
        return List.of();
    }

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId) {
        return List.of();
    }

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRoleBo role) {
        return !sysRoleMapper.exists(where -> where
                .eq(SysRole::getRoleName, role.getRoleName())
                .ne(ObjUtil.isNotNull(role.getId()), SysRole::getId, role.getId())
        );
    }


    /**
     * 校验角色权限是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleKeyUnique(SysRoleBo role) {
        return !sysRoleMapper.exists(where -> where
                .eq(SysRole::getRoleKey, role.getRoleKey())
                .ne(ObjUtil.isNotNull(role.getId()), SysRole::getId, role.getId())
        );
    }

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRoleBo role) {

    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    @Override
    public void checkRoleDataScope(Long roleId) {

    }

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public long countUserRoleByRoleId(Long roleId) {
        return 0;
    }

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态
     * @return 结果
     */
    @Override
    public boolean updateRoleStatus(Long roleId, String status) {
        return false;
    }

    /**
     * 修改数据权限信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    @Override
    public int authDataScope(SysRoleBo bo) {
        return 0;
    }

    /**
     * 根据角色Id清除在线用户
     *
     * @param roleId 角色Id
     */
    @Override
    public void cleanOnlineUserByRole(Long roleId) {

    }

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    public int deleteAuthUser(SysUserRole userRole) {
        return 0;
    }

    /**
     * 批量取消授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        return 0;
    }

    /**
     * 批量选择授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    @Override
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        return 0;
    }
}
