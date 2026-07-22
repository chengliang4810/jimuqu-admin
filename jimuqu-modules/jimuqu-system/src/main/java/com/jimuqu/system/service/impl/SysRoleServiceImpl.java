package com.jimuqu.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.mybatis.enums.DataScopeType;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.SysRoleMenu;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.query.SysMenuQuery;
import com.jimuqu.system.domain.query.SysRoleQuery;
import com.jimuqu.system.domain.vo.SysMenuVo;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysMenuMapper;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysRoleMenuMapper;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色信息 Service。
 *
 * @author chengliang4810
 */
@Component
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final ISysDataScopeService dataScopeService;
    private final OnlineUserSessionCleaner onlineUserSessionCleaner;

    @Override
    public SysRoleVo queryById(Long id) {
        return roleMapper.selectRoleById(id);
    }

    @Override
    public Page<SysRoleVo> queryPageList(SysRoleQuery query, PageQuery pageQuery) {
        return pageQuery.applyOrder(buildQueryChain(query)).returnType(SysRoleVo.class).paging(pageQuery.build());
    }

    @Override
    public List<SysRoleVo> queryList(SysRoleQuery query) {
        return buildQueryChain(query).returnType(SysRoleVo.class).list();
    }

    private QueryChain<SysRole> buildQueryChain(SysRoleQuery query) {
        QueryChain<SysRole> queryChain = QueryChain.of(roleMapper)
                .forSearch(true)
                .where(query)
                .eq(SysRole::getDelFlag, "0")
                .orderBy(SysRole::getRoleSort, SysRole::getCreateTime);
        applyRoleDataScope(queryChain);
        return queryChain;
    }

    private void applyRoleDataScope(QueryChain<SysRole> queryChain) {
        DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
        if (rule.allAccess()) {
            return;
        }
        boolean hasDepartments = !rule.departmentIds().isEmpty();
        boolean hasSelf = rule.selfAccess() && rule.userId() != null;
        if (hasDepartments && hasSelf) {
            queryChain.andNested(scope -> scope.in(SysRole::getCreateDept, rule.departmentIds())
                    .or().eq(SysRole::getCreateBy, rule.userId()));
        } else if (hasDepartments) {
            queryChain.in(SysRole::getCreateDept, rule.departmentIds());
        } else if (hasSelf) {
            queryChain.eq(SysRole::getCreateBy, rule.userId());
        } else {
            queryChain.andNested(scope -> scope.eq(SysRole::getId, 0L).and().ne(SysRole::getId, 0L));
        }
    }

    @Override
    @Transaction
    public Boolean insertByBo(SysRoleBo bo) {
        SysRole role = MapstructUtil.convert(bo, SysRole.class);
        if (role == null || roleMapper.save(role) <= 0) {
            return false;
        }
        bo.setId(role.getId());
        replaceRoleMenus(role.getId(), bo.getMenuIds());
        return true;
    }

    @Override
    @Transaction
    public Boolean updateByBo(SysRoleBo bo) {
        SysRole role = MapstructUtil.convert(bo, SysRole.class);
        if (role != null && UserConstants.ROLE_DISABLE.equals(role.getStatus())
                && countUserRoleByRoleId(role.getId()) > 0) {
            throw new ServiceException("角色已分配，不能禁用!");
        }
        boolean updated = role != null && roleMapper.update(role) > 0;
        if (updated) {
            cleanOnlineUserByRole(role.getId());
        }
        return updated;
    }

    @Override
    @Transaction
    public Integer deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        for (Long roleId : ids) {
            checkRoleAllowed(new SysRoleBo(roleId));
            checkRoleDataScope(roleId);
            if (countUserRoleByRoleId(roleId) > 0) {
                SysRole role = roleMapper.getById(roleId);
                throw new ServiceException(role.getRoleName() + "已分配，不能删除!");
            }
        }
        roleMenuMapper.delete(where -> where.in(SysRoleMenu::getRoleId, ids));
        roleDeptMapper.delete(where -> where.in(SysRoleDept::getRoleId, ids));
        return roleMapper.deleteByIds(ids);
    }

    @Override
    public List<SysRoleVo> selectRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<SysRoleVo> selectRolesAuthByUserId(Long userId) {
        Set<Long> assignedRoleIds = roleMapper.selectRolesByUserId(userId).stream()
                .map(SysRoleVo::getId).collect(java.util.stream.Collectors.toSet());
        List<SysRoleVo> roles = selectRoleAll();
        roles.forEach(role -> role.setFlag(assignedRoleIds.contains(role.getId())));
        return roles;
    }

    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        Set<String> permissions = new HashSet<>();
        for (SysRoleVo role : roleMapper.selectRolesByUserId(userId)) {
            if (role != null && UserConstants.ROLE_NORMAL.equals(role.getStatus())
                    && StringUtil.isNotBlank(role.getRoleKey())) {
                permissions.addAll(StringUtil.splitList(role.getRoleKey().trim()));
            }
        }
        return permissions;
    }

    @Override
    public List<SysRoleVo> selectRoleAll() {
        return selectRoleByIds(null);
    }

    @Override
    public List<SysRoleVo> selectRoleByIds(Collection<Long> roleIds) {
        QueryChain<SysRole> queryChain = QueryChain.of(roleMapper)
                .eq(SysRole::getStatus, UserConstants.ROLE_NORMAL)
                .eq(SysRole::getDelFlag, "0")
                .in(CollUtil.isNotEmpty(roleIds), SysRole::getId, roleIds)
                .orderBy(SysRole::getRoleSort, SysRole::getCreateTime);
        applyRoleDataScope(queryChain);
        return queryChain
                .returnType(SysRoleVo.class)
                .list();
    }

    @Override
    public List<Long> selectRoleListByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId).stream().map(SysRoleVo::getId).toList();
    }

    @Override
    public boolean checkRoleNameUnique(SysRoleBo role) {
        return !roleMapper.exists(where -> where
                .eq(SysRole::getRoleName, role.getRoleName())
                .ne(ObjectUtil.isNotNull(role.getId()), SysRole::getId, role.getId()));
    }

    @Override
    public boolean checkRoleKeyUnique(SysRoleBo role) {
        return !roleMapper.exists(where -> where
                .eq(SysRole::getRoleKey, role.getRoleKey())
                .ne(ObjectUtil.isNotNull(role.getId()), SysRole::getId, role.getId()));
    }

    @Override
    public void checkRoleAllowed(SysRoleBo role) {
        if (role == null) {
            throw new ServiceException("角色信息不能为空");
        }
        if (UserConstants.SUPER_ADMIN_ID.equals(role.getId())) {
            throw new ServiceException("不允许操作超级管理员角色");
        }
        SysRole current = role.getId() == null ? null : roleMapper.getById(role.getId());
        if (current == null) {
            if (role.getId() != null) {
                throw new ServiceException("角色不存在");
            }
            if (GlobalConstants.SUPER_ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
                throw new ServiceException("不允许使用系统内置管理员角色标识符");
            }
            return;
        }
        if (!ObjectUtil.equals(current.getRoleKey(), role.getRoleKey())
                && (GlobalConstants.SUPER_ADMIN_ROLE_KEY.equals(current.getRoleKey())
                || GlobalConstants.SUPER_ADMIN_ROLE_KEY.equals(role.getRoleKey()))) {
            throw new ServiceException("不允许修改系统内置管理员角色标识符");
        }
    }

    @Override
    public void checkRoleDataScope(Long roleId) {
        if (roleId == null) {
            throw new ServiceException("角色ID不能为空");
        }
        SysRole role = roleMapper.getById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在或已被删除");
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
        if (!rule.permits(role.getCreateBy(), role.getCreateDept())) {
            throw new ServiceException("没有权限访问角色数据");
        }
    }

    @Override
    public long countUserRoleByRoleId(Long roleId) {
        return QueryChain.of(userRoleMapper).eq(SysUserRole::getRoleId, roleId).count();
    }

    @Override
    @Transaction
    public boolean updateRoleStatus(Long roleId, String status) {
        if (UserConstants.ROLE_DISABLE.equals(status) && countUserRoleByRoleId(roleId) > 0) {
            throw new ServiceException("角色已分配，不能禁用!");
        }
        boolean updated = roleMapper.update(new SysRole(roleId).setStatus(status)) > 0;
        if (updated) {
            cleanOnlineUserByRole(roleId);
        }
        return updated;
    }

    @Override
    @Transaction
    public int authDataScope(SysRoleBo bo) {
        checkDataScope(bo.getDataScope());
        SysRole current = roleMapper.getById(bo.getId());
        if (current == null) {
            throw new ServiceException("角色不存在");
        }
        int rows = roleMapper.update(new SysRole(bo.getId())
                .setDataScope(bo.getDataScope())
                .setDeptCheckStrictly(bo.getDeptCheckStrictly()));
        replaceRoleDepts(bo.getId(), bo.getDeptIds());
        cleanOnlineUserByRole(bo.getId());
        return rows > 0 ? rows : 1;
    }

    @Override
    @Transaction
    public int updateRolePermission(SysRoleBo bo) {
        checkDataScope(bo.getDataScope());
        SysRole current = roleMapper.getById(bo.getId());
        if (current == null) {
            throw new ServiceException("角色不存在");
        }
        int rows = roleMapper.update(new SysRole(bo.getId())
                .setDataScope(bo.getDataScope())
                .setMenuCheckStrictly(bo.getMenuCheckStrictly())
                .setDeptCheckStrictly(bo.getDeptCheckStrictly()));
        replaceRoleMenus(bo.getId(), bo.getMenuIds());
        replaceRoleDepts(bo.getId(), bo.getDeptIds());
        cleanOnlineUserByRole(bo.getId());
        return rows > 0 ? rows : 1;
    }

    private void checkDataScope(String dataScope) {
        if (DataScopeType.findCode(dataScope) == null) {
            throw new ServiceException("未知的数据权限范围: " + dataScope);
        }
    }

    @Override
    public void cleanOnlineUserByRole(Long roleId) {
        onlineUserSessionCleaner.cleanRoleAfterCommit(roleId);
    }

    @Override
    @Transaction
    public int deleteAuthUser(SysUserRole userRole) {
        if (userRole == null || userRole.getRoleId() == null || userRole.getUserId() == null) {
            return 0;
        }
        checkRoleDataScope(userRole.getRoleId());
        checkNotCurrentUser(List.of(userRole.getUserId()));
        checkUserDataScope(List.of(userRole.getUserId()));
        int rows = userRoleMapper.delete(where -> where
                .eq(SysUserRole::getRoleId, userRole.getRoleId())
                .eq(SysUserRole::getUserId, userRole.getUserId()));
        if (rows > 0) {
            onlineUserSessionCleaner.cleanUsersAfterCommit(List.of(userRole.getUserId()));
        }
        return rows;
    }

    @Override
    @Transaction
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        if (roleId == null || userIds == null || userIds.length == 0) {
            return 0;
        }
        checkRoleDataScope(roleId);
        List<Long> requested = Arrays.stream(userIds).distinct().toList();
        checkNotCurrentUser(requested);
        checkUserDataScope(requested);
        int rows = userRoleMapper.delete(where -> where
                .eq(SysUserRole::getRoleId, roleId)
                .in(SysUserRole::getUserId, requested));
        if (rows > 0) {
            onlineUserSessionCleaner.cleanUsersAfterCommit(requested);
        }
        return rows;
    }

    @Override
    @Transaction
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        if (roleId == null || userIds == null || userIds.length == 0) {
            return 0;
        }
        checkRoleDataScope(roleId);
        List<Long> requested = Arrays.stream(userIds).distinct().toList();
        checkNotCurrentUser(requested);
        checkUserDataScope(requested);
        Set<Long> existing = new HashSet<>(QueryChain.of(userRoleMapper)
                .select(SysUserRole::getUserId)
                .eq(SysUserRole::getRoleId, roleId)
                .in(SysUserRole::getUserId, requested)
                .returnType(Long.class)
                .list());
        List<SysUserRole> relations = requested.stream()
                .filter(userId -> !existing.contains(userId))
                .map(userId -> {
                    SysUserRole relation = new SysUserRole();
                    relation.setRoleId(roleId);
                    relation.setUserId(userId);
                    return relation;
                })
                .toList();
        int rows = relations.isEmpty() ? 0 : userRoleMapper.saveBatch(relations);
        if (rows > 0) {
            onlineUserSessionCleaner.cleanUsersAfterCommit(requested);
        }
        return rows;
    }

    private void checkNotCurrentUser(Collection<Long> userIds) {
        if (userIds.contains(LoginHelper.getUserId())) {
            throw new ServiceException("不允许修改当前用户角色");
        }
    }

    private void checkUserDataScope(Collection<Long> userIds) {
        List<SysUser> users = QueryChain.of(userMapper).in(SysUser::getId, userIds).list();
        if (users.size() != userIds.size()) {
            throw new ServiceException("用户不存在或已被删除");
        }
        DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
        if (users.stream().anyMatch(user -> !permitsUser(rule, user))) {
            throw new ServiceException("没有权限访问部分用户数据");
        }
    }

    static boolean permitsUser(DataScopeRule rule, SysUser user) {
        return SysUserServiceImpl.permitsUser(rule, user);
    }

    private void replaceRoleMenus(Long roleId, Long[] menuIds) {
        List<Long> requested = validateMenuIds(menuIds);
        roleMenuMapper.delete(where -> where.eq(SysRoleMenu::getRoleId, roleId));
        if (requested.isEmpty()) {
            return;
        }
        List<SysRoleMenu> relations = requested.stream().map(menuId -> {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            return relation;
        }).toList();
        roleMenuMapper.saveBatch(relations);
    }

    private void replaceRoleDepts(Long roleId, Long[] deptIds) {
        List<Long> requested = validateDeptIds(deptIds);
        roleDeptMapper.delete(where -> where.eq(SysRoleDept::getRoleId, roleId));
        if (requested.isEmpty()) {
            return;
        }
        List<SysRoleDept> relations = requested.stream().map(deptId -> {
            SysRoleDept relation = new SysRoleDept();
            relation.setRoleId(roleId);
            relation.setDeptId(deptId);
            return relation;
        }).toList();
        roleDeptMapper.saveBatch(relations);
    }

    private List<Long> validateMenuIds(Long[] menuIds) {
        List<Long> requested = validateDistinctIds(menuIds, "菜单");
        if (requested.isEmpty()) {
            return requested;
        }
        Set<Long> accessibleIds;
        if (LoginHelper.isSuperAdmin()) {
            accessibleIds = new HashSet<>(QueryChain.of(menuMapper)
                    .select(SysMenu::getId)
                    .in(SysMenu::getId, requested)
                    .returnType(Long.class)
                    .list());
        } else {
            accessibleIds = menuMapper.selectMenuListByUserId(LoginHelper.getUserId(), new SysMenuQuery()).stream()
                    .map(SysMenuVo::getId)
                    .collect(java.util.stream.Collectors.toSet());
        }
        if (!accessibleIds.containsAll(requested)) {
            throw new ServiceException("菜单不存在或无权访问");
        }
        return requested;
    }

    private List<Long> validateDeptIds(Long[] deptIds) {
        List<Long> requested = validateDistinctIds(deptIds, "部门");
        if (requested.isEmpty()) {
            return requested;
        }
        List<SysDept> depts = QueryChain.of(deptMapper)
                .in(SysDept::getId, requested)
                .list();
        if (depts.size() != requested.size()) {
            throw new ServiceException("部门不存在或无权访问");
        }
        if (!LoginHelper.isSuperAdmin()) {
            DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
            if (!rule.allAccess() && depts.stream().anyMatch(dept -> !rule.departmentIds().contains(dept.getId()))) {
                throw new ServiceException("部门不存在或无权访问");
            }
        }
        return requested;
    }

    private List<Long> validateDistinctIds(Long[] ids, String relationName) {
        if (ids == null || ids.length == 0) {
            return List.of();
        }
        List<Long> requested = Arrays.asList(ids);
        if (requested.stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(requested).size() != requested.size()) {
            throw new ServiceException(relationName + "ID不能为空或重复");
        }
        return requested;
    }
}
