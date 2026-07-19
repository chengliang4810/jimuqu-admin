package com.jimuqu.system.domain;

import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AutoTableIndexParityTest {

    @Test
    void fieldIndexesMatchUpstreamSystemSchema() throws Exception {
        assertFieldIndex(SysDept.class, "parentId", "sys_dept_parent_id", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysUser.class, "deptId", "sys_user_dept_id", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysUser.class, "userName", "sys_user_user_name", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysUser.class, "phonenumber", "sys_user_phone", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysPost.class, "deptId", "sys_post_dept_id", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysUserRole.class, "roleId", "sys_user_role_rid", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysOperLog.class, "businessType", "sys_oper_log_bt", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysOperLog.class, "userId", "sys_oper_log_uid", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysOperLog.class, "status", "sys_oper_log_s", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysOperLog.class, "operTime", "sys_oper_log_ot", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysDictData.class, "dictTypeKey", "sys_dict_data_type", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysLoginInfo.class, "status", "sys_login_info_s", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysLoginInfo.class, "loginTime", "sys_login_info_lt", IndexTypeEnum.NORMAL);
        assertFieldIndex(SysDictType.class, "dictKey", "uk_sys_dict_type_dict_key", IndexTypeEnum.UNIQUE);
    }

    @Test
    void inheritedAuditFieldsKeepUpstreamIndexes() {
        assertTableIndex(SysUser.class, "sys_user_create_by", "createBy");
        assertTableIndex(SysRole.class, "sys_role_create_dept", "createDept");
        assertTableIndex(SysRole.class, "sys_role_create_by", "createBy");
        assertTableIndex(SysMessage.class, "sys_message_category_time", "category", "createTime");
    }

    private static void assertFieldIndex(Class<?> type, String fieldName, String name,
                                         IndexTypeEnum indexType) throws Exception {
        Index index = type.getDeclaredField(fieldName).getAnnotation(Index.class);
        assertNotNull(index, type.getSimpleName() + "." + fieldName);
        assertEquals(name, index.name());
        assertEquals(indexType, index.type());
    }

    private static void assertTableIndex(Class<?> type, String name, String... fields) {
        Map<String, TableIndex> indexes = Arrays.stream(type.getAnnotationsByType(TableIndex.class))
                .collect(Collectors.toMap(TableIndex::name, index -> index));
        TableIndex index = indexes.get(name);
        assertNotNull(index, type.getSimpleName() + " 缺少 " + name);
        assertArrayEquals(fields, index.fields());
    }
}
