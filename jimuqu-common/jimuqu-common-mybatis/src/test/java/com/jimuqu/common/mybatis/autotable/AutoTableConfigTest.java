package com.jimuqu.common.mybatis.autotable;

import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.TableIndexes;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AutoTableConfigTest {

    @Test
    void repeatedTableIndexesAreReadOnlyFromTheirContainer() {
        AutoTableAnnotationFinder finder = new AutoTableConfig().autoTableAnnotationFinder();

        TableIndexes container = finder.find(RepeatedIndexEntity.class, TableIndexes.class);

        assertEquals(2, container.value().length);
        assertNull(finder.find(RepeatedIndexEntity.class, TableIndex.class));
    }

    @TableIndex(name = "first_index", fields = "first")
    @TableIndex(name = "second_index", fields = "second")
    private static class RepeatedIndexEntity {
        private Long first;
        private Long second;
    }
}
