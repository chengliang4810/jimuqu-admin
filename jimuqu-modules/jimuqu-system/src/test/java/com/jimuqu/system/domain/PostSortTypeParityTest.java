package com.jimuqu.system.domain;

import com.jimuqu.system.domain.bo.SysPostBo;
import com.jimuqu.system.domain.query.SysPostQuery;
import com.jimuqu.system.domain.vo.SysPostVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostSortTypeParityTest {

    @Test
    void postSortUsesUpstreamIntegerTypeAcrossLayers() throws NoSuchFieldException {
        assertPostSortType(SysPost.class);
        assertPostSortType(SysPostBo.class);
        assertPostSortType(SysPostQuery.class);
        assertPostSortType(SysPostVo.class);
    }

    private static void assertPostSortType(Class<?> type) throws NoSuchFieldException {
        assertEquals(Integer.class, type.getDeclaredField("postSort").getType(), type.getName());
    }
}
