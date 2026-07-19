package com.jimuqu.common.mybatis.model;

import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.mybatis.enums.DataScopeType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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
                case ALL -> {
                    return DataScopeRule.all(userId);
                }
                case CUSTOM -> departmentIds.addAll(requireResult(
                        customDeptResolver.apply(role.getRoleId()), "自定义部门权限"));
                case DEPT -> departmentIds.add(requireDeptId(userDeptId));
                case DEPT_AND_CHILD -> departmentIds.addAll(requireResult(
                        childDeptResolver.apply(requireDeptId(userDeptId)), "部门及以下权限"));
                case SELF -> selfAccess = true;
                case DEPT_AND_CHILD_OR_SELF -> {
                    departmentIds.addAll(requireResult(
                            childDeptResolver.apply(requireDeptId(userDeptId)), "部门及以下权限"));
                    selfAccess = true;
                }
            }
        }
        return new DataScopeRule(false, departmentIds, selfAccess, userId);
    }

    /**
     * 聚合写操作规则：参与角色之间使用交集，ALL 角色沿用上游的短路放行语义。
     */
    public static DataScopeWriteRule aggregateWrite(
            Long userId,
            Long userDeptId,
            Collection<RoleDTO> roles,
            Function<Long, ? extends Collection<Long>> customDeptResolver,
            Function<Long, ? extends Collection<Long>> childDeptResolver) {
        if (roles == null || roles.isEmpty()) {
            return DataScopeWriteRule.deny();
        }

        List<DataScopeRule> roleRules = new ArrayList<>(roles.size());
        for (RoleDTO role : roles) {
            DataScopeRule roleRule = aggregate(
                    userId,
                    userDeptId,
                    Collections.singletonList(role),
                    customDeptResolver,
                    childDeptResolver);
            if (roleRule.allAccess()) {
                return DataScopeWriteRule.all();
            }
            roleRules.add(roleRule);
        }
        return DataScopeWriteRule.of(roleRules);
    }

    /**
     * 按写操作语义校验一条记录。
     */
    public static boolean permitsWrite(
            Long userId,
            Long userDeptId,
            Collection<RoleDTO> roles,
            Function<Long, ? extends Collection<Long>> customDeptResolver,
            Function<Long, ? extends Collection<Long>> childDeptResolver,
            Long recordUserId,
            Long recordDeptId) {
        return aggregateWrite(userId, userDeptId, roles, customDeptResolver, childDeptResolver)
                .permits(recordUserId, recordDeptId);
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
