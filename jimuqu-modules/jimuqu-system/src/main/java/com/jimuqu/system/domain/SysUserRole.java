package com.jimuqu.system.domain;

import cn.xbatis.db.annotations.Table;
import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.PrimaryKey;

/**
 * 用户和角色关联 sys_user_role
 *
 * @author Lion Li,chengliang4810
 */

@Data
@Table("sys_user_role")
public class SysUserRole {

    /**
     * 用户ID
     */
    @PrimaryKey
    @AutoColumn(comment = "用户ID", notNull = true)
    private Long userId;

    /**
     * 角色ID
     */
    @PrimaryKey
    @AutoColumn(comment = "角色ID", notNull = true)
    @Index(name = "sys_user_role_rid")
    private Long roleId;

}
