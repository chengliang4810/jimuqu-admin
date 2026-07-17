package com.jimuqu.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysRole;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * 角色信息业务对象 sys_role
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysRole.class, reverseConvertGenerate = false)
public class SysRoleBo extends BoBaseEntity {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空", groups = { UpdateGroup.class })
    @JsonProperty("roleId")
    @ONodeAttr(name = "roleId")
    private Long id;
    /**
     * 角色名称
     */
    @Length(min = 0, max = 30, message = "角色名称长度不能超过{max}个字符")
    @NotBlank(message = "角色名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String roleName;
    /**
     * 角色权限字符串
     */
    @Length(min = 0, max = 100, message = "权限字符长度不能超过{max}个字符")
    @NotBlank(message = "角色权限字符串不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String roleKey;
    /**
     * 显示顺序
     */
    @NotNull(message = "显示顺序不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Integer roleSort;
    /**
     * 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）
     */
    private String dataScope;
    /**
     * 菜单树选择项是否关联显示
     */
    private Boolean menuCheckStrictly;
    /**
     * 部门树选择项是否关联显示
     */
    private Boolean deptCheckStrictly;
    /**
     * 角色状态（0正常 1停用）
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

    /**
     * 菜单组
     */
    private Long[] menuIds;

    /**
     * 部门组（数据权限）
     */
    private Long[] deptIds;

    public SysRoleBo(Long id) {
        this.id = id;
    }

    public boolean isSuperAdmin() {
        return UserConstants.SUPER_ADMIN_ID.equals(this.id);
    }

}
