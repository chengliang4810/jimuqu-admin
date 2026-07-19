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
 * 岗位信息
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_post")
public class SysPost extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 岗位ID
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "岗位ID")
    private Long postId;
    /**
     * 部门id
     */
    @AutoColumn(comment = "部门id")
    @Index(name = "sys_post_dept_id")
    private Long deptId;
    /**
     * 岗位编码
     */
    @AutoColumn(comment = "岗位编码", length = 64)
    private String postCode;
    /**
     * 岗位类别编码
     */
    @AutoColumn(comment = "岗位类别编码", length = 100)
    private String postCategory;
    /**
     * 岗位名称
     */
    @AutoColumn(comment = "岗位名称", length = 50)
    private String postName;
    /**
     * 显示顺序
     */
    @AutoColumn(comment = "显示顺序", defaultValue = "0")
    private Integer postSort;
    /**
     * 状态（0正常 1停用）
     */
    @AutoColumn(comment = "状态（0正常 1停用）", length = 1, defaultValue = "0")
    private String status;
    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @LogicDelete(beforeValue = "0", afterValue = "1")
    @AutoColumn(comment = "删除标志（0代表存在 1代表删除）", length = 1, defaultValue = "0")
    private String delFlag;
    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;

}
