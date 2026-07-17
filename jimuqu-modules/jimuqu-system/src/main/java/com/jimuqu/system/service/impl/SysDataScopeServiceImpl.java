package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeRuleAggregator;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Override
    public DataScopeRule resolveUserDataScope(Long userId) {
        if (userId == null) {
            return DataScopeRule.deny(null);
        }
        if (UserConstants.SUPER_ADMIN_ID.equals(userId)) {
            return DataScopeRule.all(userId);
        }
        try {
            SysUser user = userMapper.getById(userId);
            if (user == null || user.getDeptId() == null) {
                return DataScopeRule.deny(userId);
            }
            List<RoleDTO> roles = roleMapper.selectRolesByUserId(userId).stream()
                    .filter(role -> UserConstants.ROLE_NORMAL.equals(role.getStatus()))
                    .map(this::toRoleDto)
                    .toList();
            return DataScopeRuleAggregator.aggregate(
                    userId, user.getDeptId(), roles, this::getRoleCustom, this::getDeptAndChild);
        } catch (RuntimeException ex) {
            log.error("计算用户数据权限失败，已拒绝全部数据，userId={}", userId, ex);
            return DataScopeRule.deny(userId);
        }
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
