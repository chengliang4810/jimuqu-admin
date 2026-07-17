package com.jimuqu.common.mybatis.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Xbatis 查询可直接消费的数据范围规则。
 *
 * @param allAccess     是否允许全部数据
 * @param departmentIds 允许访问的部门ID并集
 * @param selfAccess    是否允许本人数据
 * @param userId        当前用户ID
 */
public record DataScopeRule(boolean allAccess, Set<Long> departmentIds,
                            boolean selfAccess, Long userId) {

    public DataScopeRule {
        departmentIds = departmentIds == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(departmentIds));
    }

    public static DataScopeRule deny(Long userId) {
        return new DataScopeRule(false, Collections.emptySet(), false, userId);
    }

    public static DataScopeRule all(Long userId) {
        return new DataScopeRule(true, Collections.emptySet(), false, userId);
    }

    public boolean denyAll() {
        return !allAccess && departmentIds.isEmpty() && !selfAccess;
    }

    public boolean permits(Long recordUserId, Long recordDeptId) {
        if (allAccess) {
            return true;
        }
        if (recordDeptId != null && departmentIds.contains(recordDeptId)) {
            return true;
        }
        return selfAccess && userId != null && userId.equals(recordUserId);
    }
}
