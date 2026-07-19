package com.jimuqu.system.domain;

import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.PrimaryKey;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssociationSchemaContractTest {

    @Test
    void rbacAssociationColumnsFormNonNullCompositePrimaryKeys() throws Exception {
        assertCompositeKey(SysRoleDept.class, "roleId", "deptId");
        assertCompositeKey(SysRoleMenu.class, "roleId", "menuId");
        assertCompositeKey(SysUserPost.class, "userId", "postId");
        assertCompositeKey(SysUserRole.class, "userId", "roleId");
    }

    private void assertCompositeKey(Class<?> type, String... fieldNames) throws NoSuchFieldException {
        for (String fieldName : fieldNames) {
            Field field = type.getDeclaredField(fieldName);
            assertNotNull(field.getAnnotation(PrimaryKey.class),
                    type.getSimpleName() + "." + fieldName + " must be a primary key column");
            AutoColumn column = field.getAnnotation(AutoColumn.class);
            assertNotNull(column, type.getSimpleName() + "." + fieldName);
            assertTrue(column.notNull(),
                    type.getSimpleName() + "." + fieldName + " must reject null values");
        }
    }
}
