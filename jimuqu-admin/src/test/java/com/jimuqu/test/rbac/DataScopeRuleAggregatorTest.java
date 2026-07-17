package com.jimuqu.test.rbac;

import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeRuleAggregator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeRuleAggregatorTest {

    @Test
    void aggregatesCustomDeptChildAndSelfAsUnion() {
        List<RoleDTO> roles = Arrays.asList(
                role(2L, "2"), role(3L, "3"), role(4L, "4"), role(5L, "5"));
        Map<Long, Collection<Long>> custom = Map.of(2L, Arrays.asList(201L, 202L));

        DataScopeRule rule = DataScopeRuleAggregator.aggregate(
                9L, 103L, roles,
                roleId -> custom.getOrDefault(roleId, Collections.emptyList()),
                deptId -> Arrays.asList(deptId, 104L, 105L));

        assertFalse(rule.allAccess());
        assertTrue(rule.selfAccess());
        assertEquals(Arrays.asList(201L, 202L, 103L, 104L, 105L),
                rule.departmentIds().stream().toList());
        assertTrue(rule.permits(99L, 104L));
        assertTrue(rule.permits(9L, 999L));
        assertFalse(rule.permits(99L, 999L));
    }

    @Test
    void allScopeWinsWithoutSkippingOtherRoleValidation() {
        DataScopeRule rule = DataScopeRuleAggregator.aggregate(
                9L, 103L, Arrays.asList(role(1L, "1"), role(2L, "2")),
                roleId -> Collections.singletonList(201L),
                deptId -> Collections.singletonList(deptId));

        assertTrue(rule.allAccess());
        assertTrue(rule.permits(99L, 999L));
        assertTrue(rule.departmentIds().contains(201L));
    }

    @Test
    void missingRolesReturnsDenyRule() {
        DataScopeRule rule = DataScopeRuleAggregator.aggregate(
                9L, 103L, Collections.emptyList(),
                roleId -> Collections.emptyList(), deptId -> Collections.emptyList());

        assertTrue(rule.denyAll());
        assertFalse(rule.permits(9L, 103L));
    }

    @Test
    void unknownScopeFailsClosedAtResolverBoundary() {
        assertThrows(IllegalStateException.class, () -> DataScopeRuleAggregator.aggregate(
                9L, 103L, Collections.singletonList(role(8L, "9")),
                roleId -> Collections.emptyList(), deptId -> Collections.emptyList()));
    }

    @Test
    void resolverFailurePropagatesForServiceToConvertToDeny() {
        assertThrows(IllegalStateException.class, () -> DataScopeRuleAggregator.aggregate(
                9L, 103L, Collections.singletonList(role(2L, "2")),
                roleId -> {
                    throw new IllegalStateException("database unavailable");
                },
                deptId -> Collections.emptyList()));
    }

    @Test
    void missingDepartmentRejectsDeptScope() {
        assertThrows(IllegalStateException.class, () -> DataScopeRuleAggregator.aggregate(
                9L, null, Collections.singletonList(role(3L, "3")),
                roleId -> Collections.emptyList(), deptId -> Collections.emptyList()));
    }

    private RoleDTO role(Long roleId, String dataScope) {
        RoleDTO role = new RoleDTO();
        role.setRoleId(roleId);
        role.setDataScope(dataScope);
        return role;
    }
}
