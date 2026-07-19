package com.jimuqu.system.domain;

import cn.xbatis.db.annotations.LogicDelete;
import org.dromara.autotable.annotation.AutoColumn;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogicalDeleteMetadataTest {

    @Test
    void systemEntitiesUseTheUpstreamZeroToOneContract() throws NoSuchFieldException {
        for (Class<?> type : List.of(SysUser.class, SysRole.class, SysDept.class, SysPost.class, SysClient.class)) {
            Field field = type.getDeclaredField("delFlag");
            LogicDelete logicDelete = field.getAnnotation(LogicDelete.class);
            AutoColumn autoColumn = field.getAnnotation(AutoColumn.class);
            assertNotNull(logicDelete, type.getSimpleName() + ".delFlag 必须启用 Xbatis 逻辑删除");
            assertEquals("0", logicDelete.beforeValue());
            assertEquals("1", logicDelete.afterValue());
            assertNotNull(autoColumn, type.getSimpleName() + ".delFlag 必须由 AutoTable 维护");
            assertEquals("0", autoColumn.defaultValue());
            assertEquals(1, autoColumn.length());
        }
    }
}
