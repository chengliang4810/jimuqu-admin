package com.jimuqu.system.domain.query;

import cn.xbatis.db.annotations.Condition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysDeptQueryParityTest {

    @Test
    void departmentCategoryUsesLikeMatching() throws NoSuchFieldException {
        Condition condition = SysDeptQuery.class.getDeclaredField("deptCategory").getAnnotation(Condition.class);

        assertEquals(Condition.Type.LIKE, condition.value());
    }
}
