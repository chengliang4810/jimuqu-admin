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
import org.dromara.autotable.annotation.Index;

import java.io.Serial;

/**
 * 部门
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_dept")
public class SysDept extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "主键")
    private Long id;
    /**
     * 父部门id
     */
    @AutoColumn(comment = "父部门id")
    @Index(name = "sys_dept_parent_id")
    private Long parentId;
    /**
     * 祖级列表
     */
    @AutoColumn(comment = "祖级列表", length = 500)
    private String ancestors;
    /**
     * 部门名称
     */
    @AutoColumn(comment = "部门名称", length = 30)
    private String deptName;
    /**
     * 部门类别编码
     */
    @AutoColumn(comment = "部门类别编码", length = 100)
    private String deptCategory;
    /**
     * 显示顺序
     */
    @AutoColumn(comment = "显示顺序")
    private Integer orderNum;
    /**
     * 负责人
     */
    @AutoColumn(comment = "负责人")
    private Long leader;
    /**
     * 联系电话
     */
    @AutoColumn(comment = "联系电话", length = 20)
    private String phone;
    /**
     * 邮箱
     */
    @AutoColumn(comment = "邮箱", length = 50)
    private String email;
    /**
     * 部门状态（0正常 1停用）
     */
    @AutoColumn(comment = "部门状态（0正常 1停用）", length = 1, defaultValue = "0")
    private String status;
    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @LogicDelete(beforeValue = "0", afterValue = "1")
    @AutoColumn(comment = "删除标志（0代表存在 1代表删除）", length = 1, defaultValue = "0")
    private String delFlag;

}
