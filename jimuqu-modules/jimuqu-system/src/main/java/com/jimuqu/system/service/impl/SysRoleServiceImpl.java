package com.jimuqu.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.SysRoleMenu;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.query.SysRoleQuery;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysRoleMenuMapper;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import com.jimuqu.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public SysRoleVo queryById(Long id) {
        return roleMapper.selectRoleById(id);
    }

    @Override
    public Page<SysRoleVo> queryPageList(SysRoleQuery query, PageQuery pageQuery) {
        return buildQueryChain(query).returnType(SysRoleVo.class).paging(pageQuery.build());
    }

    @Override
    public List<SysRoleVo> queryList(SysRoleQuery query) {
        return buildQueryChain(query).returnType(SysRoleVo.class).list();
    }

    private QueryChain<SysRole> buildQueryChain(SysRoleQuery query) {
        return QueryChain.of(roleMapper)
                .forSearch(true)
                .where(query)
                .eq(SysRole::getDelFlag, "0")
                .orderBy(SysRole::getRoleSort, SysRole::getId);
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
    public Boolean updateByBo(SysRoleBo bo) {
        SysRole role = MapstructUtil.convert(bo, SysRole.class);
        return role != null && roleMapper.update(role) > 0;
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
                throw new ServiceException("角色已分配用户，不能删除");
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
        return QueryChain.of(roleMapper)
                .eq(SysRole::getStatus, UserConstants.ROLE_NORMAL)
                .eq(SysRole::getDelFlag, "0")
                .in(CollUtil.isNotEmpty(roleIds), SysRole::getId, roleIds)
                .orderBy(SysRole::getRoleSort, SysRole::getId)
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
                .ne(ObjUtil.isNotNull(role.getId()), SysRole::getId, role.getId()));
    }

    @Override
    public boolean checkRoleKeyUnique(SysRoleBo role) {
        return !roleMapper.exists(where -> where
                .eq(SysRole::getRoleKey, role.getRoleKey())
                .ne(ObjUtil.isNotNull(role.getId()), SysRole::getId, role.getId()));
    }

    @Override
    public void checkRoleAllowed(SysRoleBo role) {
        if (role == null) {
            throw new ServiceException("角色信息不能为空");
        }
        if (UserConstants.SUPER_ADMIN_ID.equals(role.getId())) {
            throw new ServiceException("不允许操作超级管理员角色");
        }
        if (GlobalConstants.SUPER_ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
            throw new ServiceException("不允许使用超级管理员角色标识");
        }
    }

    @Override
    public void checkRoleDataScope(Long roleId) {
        if (roleId == null) {
            throw new ServiceException("角色ID不能为空");
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || CollUtil.isEmpty(loginUser.getRoles())) {
            throw new ServiceException("没有权限访问角色数据");
        }
        boolean allowed = loginUser.getRoles().stream()
                .map(RoleDTO::getRoleId)
                .anyMatch(roleId::equals);
        if (!allowed) {
            throw new ServiceException("没有权限访问角色数据");
        }
    }

    @Override
    public long countUserRoleByRoleId(Long roleId) {
        return QueryChain.of(userRoleMapper).eq(SysUserRole::getRoleId, roleId).count();
    }

    @Override
    public boolean updateRoleStatus(Long roleId, String status) {
        if (UserConstants.ROLE_DISABLE.equals(status) && countUserRoleByRoleId(roleId) > 0) {
            throw new ServiceException("角色已分配，不能禁用");
        }
        return roleMapper.update(new SysRole(roleId).setStatus(status)) > 0;
    }

    @Override
    @Transaction
    public int authDataScope(SysRoleBo bo) {
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

    @Override
    public void cleanOnlineUserByRole(Long roleId) {
        List<Long> userIds = QueryChain.of(userRoleMapper)
                .select(SysUserRole::getUserId)
                .eq(SysUserRole::getRoleId, roleId)
                .returnType(Long.class)
                .list();
        for (Long userId : userIds) {
            for (UserType userType : UserType.values()) {
                try {
                    StpUtil.logout(userType.getUserType() + ":" + userId);
                } catch (RuntimeException ex) {
                    log.warn("清理角色在线用户失败，roleId={}, userId={}, type={}",
                            roleId, userId, userType.getUserType(), ex);
                }
            }
        }
    }

    @Override
    public int deleteAuthUser(SysUserRole userRole) {
        if (userRole == null || userRole.getRoleId() == null || userRole.getUserId() == null) {
            return 0;
        }
        return userRoleMapper.delete(where -> where
                .eq(SysUserRole::getRoleId, userRole.getRoleId())
                .eq(SysUserRole::getUserId, userRole.getUserId()));
    }

    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        if (roleId == null || userIds == null || userIds.length == 0) {
            return 0;
        }
        return userRoleMapper.delete(where -> where
                .eq(SysUserRole::getRoleId, roleId)
                .in(SysUserRole::getUserId, Arrays.asList(userIds)));
    }

    @Override
    @Transaction
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        if (roleId == null || userIds == null || userIds.length == 0) {
            return 0;
        }
        List<Long> requested = Arrays.stream(userIds).distinct().toList();
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
        return relations.isEmpty() ? 0 : userRoleMapper.saveBatch(relations);
    }

    private void replaceRoleMenus(Long roleId, Long[] menuIds) {
        roleMenuMapper.delete(where -> where.eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.length == 0) {
            return;
        }
        List<SysRoleMenu> relations = Arrays.stream(menuIds).distinct().map(menuId -> {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            return relation;
        }).toList();
        roleMenuMapper.saveBatch(relations);
    }

    private void replaceRoleDepts(Long roleId, Long[] deptIds) {
        roleDeptMapper.delete(where -> where.eq(SysRoleDept::getRoleId, roleId));
        if (deptIds == null || deptIds.length == 0) {
            return;
        }
        List<SysRoleDept> relations = Arrays.stream(deptIds).distinct().map(deptId -> {
            SysRoleDept relation = new SysRoleDept();
            relation.setRoleId(roleId);
            relation.setDeptId(deptId);
            return relation;
        }).toList();
        roleDeptMapper.saveBatch(relations);
    }
}
