package com.jimuqu.system.domain;

import cn.xbatis.db.annotations.Table;
import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.PrimaryKey;

/**
 * 角色和菜单关联 sys_role_menu
 *
 * @author Lion Li,chengliang4810
 */

@Data
@Table("sys_role_menu")
public class SysRoleMenu {

    /**
     * 角色ID
     */
    @PrimaryKey
    @AutoColumn(comment = "角色ID", notNull = true)
    private Long roleId;

    /**
     * 菜单ID
     */
    @PrimaryKey
    @AutoColumn(comment = "菜单ID", notNull = true)
    private Long menuId;

}
