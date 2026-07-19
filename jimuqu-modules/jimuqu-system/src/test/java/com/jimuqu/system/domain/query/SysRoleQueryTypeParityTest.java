package com.jimuqu.system.domain.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysRoleQueryTypeParityTest {

    @Test
    void strictTreeFlagsUseBooleanLikeEntityAndUpstreamBo() throws Exception {
        assertEquals(Boolean.class,
                SysRoleQuery.class.getDeclaredField("menuCheckStrictly").getType());
        assertEquals(Boolean.class,
                SysRoleQuery.class.getDeclaredField("deptCheckStrictly").getType());
    }
}
