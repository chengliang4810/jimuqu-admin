package com.jimuqu.common.mybatis.model;

import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.mybatis.enums.DataScopeType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * 将多个角色的数据范围合并为一条 typed 规则。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataScopeRuleAggregator {

    public static DataScopeRule aggregate(
            Long userId,
            Long userDeptId,
            Collection<RoleDTO> roles,
            Function<Long, ? extends Collection<Long>> customDeptResolver,
            Function<Long, ? extends Collection<Long>> childDeptResolver) {
        if (roles == null || roles.isEmpty()) {
            return DataScopeRule.deny(userId);
        }

        boolean allAccess = false;
        boolean selfAccess = false;
        Set<Long> departmentIds = new LinkedHashSet<>();
        for (RoleDTO role : roles) {
            if (role == null || role.getRoleId() == null) {
                throw new IllegalStateException("角色数据不完整");
            }
            DataScopeType scope = DataScopeType.findCode(role.getDataScope());
            if (scope == null) {
                throw new IllegalStateException("未知的数据权限范围: " + role.getDataScope());
            }
            switch (scope) {
                case ALL -> allAccess = true;
                case CUSTOM -> departmentIds.addAll(requireResult(
                        customDeptResolver.apply(role.getRoleId()), "自定义部门权限"));
                case DEPT -> departmentIds.add(requireDeptId(userDeptId));
                case DEPT_AND_CHILD -> departmentIds.addAll(requireResult(
                        childDeptResolver.apply(requireDeptId(userDeptId)), "部门及以下权限"));
                case SELF -> selfAccess = true;
            }
        }
        return new DataScopeRule(allAccess, departmentIds, selfAccess, userId);
    }

    private static Long requireDeptId(Long deptId) {
        if (deptId == null) {
            throw new IllegalStateException("用户部门不存在");
        }
        return deptId;
    }

    private static Collection<Long> requireResult(Collection<Long> value, String name) {
        if (value == null) {
            throw new IllegalStateException(name + "计算结果为空");
        }
        return value;
    }
}
