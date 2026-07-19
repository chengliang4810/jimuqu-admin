package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.model.DataScopeAccess;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeRuleAggregator;
import com.jimuqu.common.mybatis.model.DataScopeWriteRule;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysMenuMapper;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 基于 Xbatis typed API 的数据范围聚合服务。
 */
@Slf4j
@Component(value = "sysDataScopeService", typed = true)
@RequiredArgsConstructor
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public DataScopeRule resolveUserDataScope(Long userId) {
        if (userId == null) {
            return DataScopeRule.deny(null);
        }
        if (UserConstants.SUPER_ADMIN_ID.equals(userId)) {
            return DataScopeRule.all(userId);
        }
        try {
            return resolveUserDataScope(userId, DataScopeAccessResolver.current());
        } catch (RuntimeException ex) {
            return denyOnFailure(userId, ex);
        }
    }

    @Override
    public DataScopeRule resolveUserDataScope(Long userId, DataScopeAccess access) {
        if (userId == null) {
            return DataScopeRule.deny(null);
        }
        if (UserConstants.SUPER_ADMIN_ID.equals(userId)) {
            return DataScopeRule.all(userId);
        }
        try {
            Objects.requireNonNull(access, "数据权限访问约束不能为空");
            SysUser user = userMapper.getById(userId);
            if (user == null) {
                return DataScopeRule.deny(userId);
            }
            List<RoleDTO> roles = loadScopeRoles(userId, access);
            return DataScopeRuleAggregator.aggregate(
                    userId, user.getDeptId(), roles, this::getRoleCustom, this::getDeptAndChild);
        } catch (RuntimeException ex) {
            return denyOnFailure(userId, ex);
        }
    }

    @Override
    public DataScopeWriteRule resolveUserWriteDataScope(Long userId) {
        if (userId == null) {
            return DataScopeWriteRule.deny();
        }
        if (UserConstants.SUPER_ADMIN_ID.equals(userId)) {
            return DataScopeWriteRule.all();
        }
        try {
            return resolveUserWriteDataScope(userId, DataScopeAccessResolver.current());
        } catch (RuntimeException ex) {
            return denyWriteOnFailure(userId, ex);
        }
    }

    @Override
    public DataScopeWriteRule resolveUserWriteDataScope(Long userId, DataScopeAccess access) {
        if (userId == null) {
            return DataScopeWriteRule.deny();
        }
        if (UserConstants.SUPER_ADMIN_ID.equals(userId)) {
            return DataScopeWriteRule.all();
        }
        try {
            Objects.requireNonNull(access, "数据权限访问约束不能为空");
            SysUser user = userMapper.getById(userId);
            if (user == null) {
                return DataScopeWriteRule.deny();
            }
            List<RoleDTO> roles = loadScopeRoles(userId, access);
            return DataScopeRuleAggregator.aggregateWrite(
                    userId,
                    user.getDeptId(),
                    roles,
                    this::getRoleCustom,
                    this::getDeptAndChild);
        } catch (RuntimeException ex) {
            return denyWriteOnFailure(userId, ex);
        }
    }

    private List<RoleDTO> loadScopeRoles(Long userId, DataScopeAccess access) {
        List<RoleDTO> roles = roleMapper.selectRolesByUserId(userId).stream()
                .filter(role -> UserConstants.ROLE_NORMAL.equals(role.getStatus()))
                .map(this::toRoleDto)
                .toList();
        return selectScopeRoles(roles, access);
    }

    private List<RoleDTO> selectScopeRoles(List<RoleDTO> roles, DataScopeAccess access) {
        if (!access.constrained()) {
            return roles;
        }
        return roles.stream().filter(role -> roleMatchesAccess(role, access)).toList();
    }

    private boolean roleMatchesAccess(RoleDTO role, DataScopeAccess access) {
        Set<String> roleKeys = Set.copyOf(StringUtil.splitList(role.getRoleKey()));
        if (roleKeys.stream().anyMatch(access.roleKeys()::contains)) {
            return true;
        }
        List<String> values = menuMapper.selectMenuPermsByRoleId(role.getRoleId());
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.stream()
                .filter(StringUtil::isNotBlank)
                .flatMap(value -> StringUtil.splitList(value).stream())
                .anyMatch(access.permissions()::contains);
    }

    private DataScopeRule denyOnFailure(Long userId, RuntimeException ex) {
        log.error("计算用户数据权限失败，已拒绝全部数据，userId={}", userId, ex);
        return DataScopeRule.deny(userId);
    }

    private DataScopeWriteRule denyWriteOnFailure(Long userId, RuntimeException ex) {
        log.error("计算用户写数据权限失败，已拒绝操作，userId={}", userId, ex);
        return DataScopeWriteRule.deny();
    }

    private RoleDTO toRoleDto(SysRoleVo role) {
        RoleDTO dto = new RoleDTO();
        dto.setRoleId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setRoleKey(role.getRoleKey());
        dto.setDataScope(role.getDataScope());
        return dto;
    }

    @Override
    public List<Long> getRoleCustom(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return QueryChain.of(roleDeptMapper)
                .select(SysRoleDept::getDeptId)
                .eq(SysRoleDept::getRoleId, roleId)
                .returnType(Long.class)
                .list();
    }

    @Override
    public List<Long> getDeptAndChild(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        result.add(deptId);
        result.addAll(QueryChain.of(deptMapper)
                .select(SysDept::getId)
                .and(SysDept::getAncestors, condition -> condition.mysql().findInSet(deptId))
                .returnType(Long.class)
                .list());
        return result;
    }

    @Override
    public List<Long> getUserDataScope(Long userId) {
        DataScopeRule rule = resolveUserDataScope(userId);
        if (rule.allAccess()) {
            return QueryChain.of(deptMapper)
                    .select(SysDept::getId)
                    .returnType(Long.class)
                    .list();
        }
        return new ArrayList<>(rule.departmentIds());
    }

    @Override
    public boolean checkUserDataScope(Long userId, Long deptId) {
        if (userId == null || deptId == null) {
            return false;
        }
        DataScopeRule rule = resolveUserDataScope(userId);
        return rule.allAccess() || rule.departmentIds().contains(deptId);
    }

    @Override
    public boolean checkUserDataScope(Long userId, Long recordUserId, Long deptId) {
        if (userId == null) {
            return false;
        }
        return resolveUserDataScope(userId).permits(recordUserId, deptId);
    }
}
