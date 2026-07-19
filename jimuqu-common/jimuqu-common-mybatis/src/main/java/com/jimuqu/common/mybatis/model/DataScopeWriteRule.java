package com.jimuqu.common.mybatis.model;

import java.util.List;

/**
 * 写操作的数据范围规则。角色之间取交集，单个角色内部仍由 {@link DataScopeRule} 表达并集分支。
 *
 * @param allAccess ALL 角色是否已触发上游短路放行语义
 * @param roleRules 参与写权限计算的逐角色规则
 */
public record DataScopeWriteRule(boolean allAccess, List<DataScopeRule> roleRules) {

    public DataScopeWriteRule {
        roleRules = allAccess || roleRules == null ? List.of() : List.copyOf(roleRules);
    }

    public static DataScopeWriteRule deny() {
        return new DataScopeWriteRule(false, List.of());
    }

    public static DataScopeWriteRule all() {
        return new DataScopeWriteRule(true, List.of());
    }

    public static DataScopeWriteRule of(List<DataScopeRule> roleRules) {
        return new DataScopeWriteRule(false, roleRules);
    }

    public boolean denyAll() {
        return !allAccess && (roleRules.isEmpty() || roleRules.stream().anyMatch(DataScopeRule::denyAll));
    }

    public boolean permits(Long recordUserId, Long recordDeptId) {
        return allAccess || (!roleRules.isEmpty()
                && roleRules.stream().allMatch(rule -> rule.permits(recordUserId, recordDeptId)));
    }
}
