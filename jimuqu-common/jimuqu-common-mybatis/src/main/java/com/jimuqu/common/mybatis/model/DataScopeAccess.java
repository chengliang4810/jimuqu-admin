package com.jimuqu.common.mybatis.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 当前操作参与数据权限计算的访问约束。
 */
public record DataScopeAccess(Set<String> permissions, Set<String> roleKeys) {

    private static final DataScopeAccess UNCONSTRAINED = new DataScopeAccess(Set.of(), Set.of());

    public DataScopeAccess {
        permissions = normalize(permissions);
        roleKeys = normalize(roleKeys);
    }

    public static DataScopeAccess unconstrained() {
        return UNCONSTRAINED;
    }

    public static DataScopeAccess ofPermissions(String... permissions) {
        return new DataScopeAccess(toSet(permissions), Set.of());
    }

    public static DataScopeAccess of(Set<String> permissions, Set<String> roleKeys) {
        return new DataScopeAccess(permissions, roleKeys);
    }

    public boolean constrained() {
        return !permissions.isEmpty() || !roleKeys.isEmpty();
    }

    private static Set<String> toSet(String[] values) {
        return values == null ? Set.of() : normalize(new LinkedHashSet<>(Arrays.asList(values)));
    }

    private static Set<String> normalize(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }
        return Set.copyOf(normalized);
    }
}
