package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.*;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;

import java.io.Serial;

/**
 * 角色信息
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_role")
public class SysRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "角色ID")
    private Long id;
    /**
     * 角色名称
     */
    @AutoColumn(comment = "角色名称", length = 30)
    private String roleName;
    /**
     * 角色权限字符串
     */
    @AutoColumn(comment = "角色权限字符串", length = 100)
    private String roleKey;
    /**
     * 显示顺序
     */
    @AutoColumn(comment = "显示顺序")
    private Integer roleSort;
    /**
     * 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）
     */
    @AutoColumn(comment = "数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）", length = 1, defaultValue = "1")
    private String dataScope;
    /**
     * 菜单树选择项是否关联显示（ 0：父子不互相关联显示 1：父子互相关联显示）
     */
    @AutoColumn(comment = "菜单树选择项是否关联显示", defaultValue = "1")
    private Boolean menuCheckStrictly;
    /**
     * 部门树选择项是否关联显示（0：父子不互相关联显示 1：父子互相关联显示 ）
     */
    @AutoColumn(comment = "部门树选择项是否关联显示", defaultValue = "1")
    private Boolean deptCheckStrictly;
    /**
     * 角色状态（0正常 1停用）
     */
    @AutoColumn(comment = "角色状态（0正常 1停用）", length = 1, defaultValue = "0")
    private String status;
    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @AutoColumn(comment = "删除标志（0代表存在 1代表删除）", length = 1, defaultValue = "0")
    private String delFlag;
    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;

    public SysRole(Long roleId) {
        this.id = roleId;
    }

}
