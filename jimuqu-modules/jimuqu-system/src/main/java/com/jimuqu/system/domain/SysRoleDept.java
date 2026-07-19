package com.jimuqu.system.domain;

import cn.xbatis.db.annotations.Table;
import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.PrimaryKey;

/**
 * 角色和部门关联 sys_role_dept
 *
 * @author Lion Li,chengliang4810
 */

@Data
@Table("sys_role_dept")
public class SysRoleDept {

    /**
     * 角色ID
     */
    @PrimaryKey
    @AutoColumn(comment = "角色ID", notNull = true)
    private Long roleId;

    /**
     * 部门ID
     */
    @PrimaryKey
    @AutoColumn(comment = "部门ID", notNull = true)
    private Long deptId;

}
