package com.jimuqu.test.rbac;

import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeRuleAggregator;
import com.jimuqu.common.mybatis.model.DataScopeWriteRule;
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
    void allScopeShortCircuitsFollowingRoles() {
        DataScopeRule rule = DataScopeRuleAggregator.aggregate(
                9L, 103L, Arrays.asList(role(1L, "1"), role(2L, "9")),
                roleId -> {
                    throw new IllegalStateException("ALL 后不应查询自定义部门");
                },
                deptId -> Collections.singletonList(deptId));

        assertTrue(rule.allAccess());
        assertTrue(rule.permits(99L, 999L));
        assertTrue(rule.departmentIds().isEmpty());
    }

    @Test
    void invalidRoleBeforeAllStillFails() {
        assertThrows(IllegalStateException.class, () -> DataScopeRuleAggregator.aggregate(
                9L, 103L, Arrays.asList(role(2L, "9"), role(1L, "1")),
                roleId -> Collections.emptyList(), deptId -> Collections.singletonList(deptId)));
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

    @Test
    void departmentChildOrSelfScopeIncludesBothBranches() {
        DataScopeRule rule = DataScopeRuleAggregator.aggregate(
                9L, 103L, Collections.singletonList(role(6L, "6")),
                roleId -> Collections.emptyList(), deptId -> List.of(103L, 104L));

        assertEquals(List.of(103L, 104L), rule.departmentIds().stream().toList());
        assertTrue(rule.selfAccess());
        assertTrue(rule.permits(20L, 104L));
        assertTrue(rule.permits(9L, 999L));
        assertFalse(rule.permits(20L, 999L));
    }

    @Test
    void departmentAndSelfAreUnionForReadButIntersectionForWrite() {
        List<RoleDTO> roles = List.of(role(3L, "3"), role(5L, "5"));
        DataScopeRule readRule = DataScopeRuleAggregator.aggregate(
                9L, 103L, roles,
                roleId -> List.of(), deptId -> List.of(deptId));
        DataScopeWriteRule writeRule = DataScopeRuleAggregator.aggregateWrite(
                9L, 103L, roles,
                roleId -> List.of(), deptId -> List.of(deptId));

        assertTrue(readRule.permits(20L, 103L));
        assertTrue(readRule.permits(9L, 999L));
        assertEquals(2, writeRule.roleRules().size());
        assertFalse(writeRule.permits(20L, 103L));
        assertFalse(writeRule.permits(9L, 999L));
        assertTrue(writeRule.permits(9L, 103L));
    }

    @Test
    void customDepartmentRolesIntersectForWrite() {
        List<RoleDTO> roles = List.of(role(2L, "2"), role(3L, "2"));
        Map<Long, Collection<Long>> custom = Map.of(
                2L, List.of(201L, 202L),
                3L, List.of(202L, 203L));

        DataScopeRule readRule = DataScopeRuleAggregator.aggregate(
                9L, 103L, roles,
                custom::get, deptId -> List.of(deptId));
        DataScopeWriteRule writeRule = DataScopeRuleAggregator.aggregateWrite(
                9L, 103L, roles,
                custom::get, deptId -> List.of(deptId));

        assertEquals(List.of(201L, 202L, 203L), readRule.departmentIds().stream().toList());
        assertTrue(writeRule.permits(20L, 202L));
        assertFalse(writeRule.permits(20L, 201L));
    }

    @Test
    void writeWithoutParticipatingRolesFailsClosed() {
        DataScopeWriteRule writeRule = DataScopeRuleAggregator.aggregateWrite(
                9L, 103L, List.of(),
                roleId -> List.of(), deptId -> List.of(deptId));

        assertTrue(writeRule.denyAll());
        assertFalse(writeRule.permits(9L, 103L));
    }

    @Test
    void allRoleKeepsUpstreamWriteShortCircuit() {
        DataScopeWriteRule writeRule = DataScopeRuleAggregator.aggregateWrite(
                9L, 103L, List.of(role(5L, "5"), role(1L, "1")),
                roleId -> List.of(), deptId -> List.of(deptId));

        assertTrue(writeRule.allAccess());
        assertTrue(writeRule.permits(20L, 999L));
    }

    private RoleDTO role(Long roleId, String dataScope) {
        RoleDTO role = new RoleDTO();
        role.setRoleId(roleId);
        role.setDataScope(dataScope);
        return role;
    }
}
