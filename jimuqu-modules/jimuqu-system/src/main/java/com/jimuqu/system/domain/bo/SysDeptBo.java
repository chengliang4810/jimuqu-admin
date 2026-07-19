package com.jimuqu.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysDept;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * 部门业务对象 sys_dept
 *
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDept.class, reverseConvertGenerate = false)
public class SysDeptBo extends BoBaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { UpdateGroup.class })
    @JsonProperty("deptId")
    @ONodeAttr(name = "deptId")
    private Long id;
    /**
     * 父部门id
     */
    @NotNull(message = "父部门id不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Long parentId;
    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    @Length(min = 0, max = 30, message = "部门名称长度不能超过{max}个字符")
    private String deptName;
    /**
     * 部门类别编码
     */
    @Length(max = 100, message = "部门类别编码长度不能超过{max}个字符")
    private String deptCategory;
    /**
     * 显示顺序
     */
    @NotNull(message = "显示顺序不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Integer orderNum;
    /**
     * 负责人
     */
    private Long leader;
    /**
     * 联系电话
     */
    @Length(min = 0, max = 11, message = "联系电话长度不能超过{max}个字符")
    private String phone;
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Length(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;
    /**
     * 部门状态（0正常 1停用）
     */
    private String status;

}
