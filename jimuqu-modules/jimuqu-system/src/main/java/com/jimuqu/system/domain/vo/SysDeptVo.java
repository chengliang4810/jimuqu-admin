package com.jimuqu.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.Ignores;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.system.domain.SysDept;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 部门视图对象
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@ExcelIgnoreUnannotated
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysDept.class)
@AutoMapper(target = SysDept.class)
@Ignores({"parentName", "leaderName", "children"})
public class SysDeptVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @JsonProperty("deptId")
    @ONodeAttr(name = "deptId")
    @ExcelProperty(value = "部门id")
    private Long id;
    /**
     * 父部门id
     */
    private Long parentId;

    /**
     * 父部门名称
     */
    private String parentName;

    /**
     * 祖级列表
     */
    private String ancestors;
    /**
     * 部门名称
     */
    @ExcelProperty(value = "部门名称")
    private String deptName;
    /**
     * 部门类别编码
     */
    @ExcelProperty(value = "部门类别编码")
    private String deptCategory;
    /**
     * 显示顺序
     */
    private Integer orderNum;
    /**
     * 负责人 Id
     */
    private Long leader;

    /**
     * 负责人
     */
    // @RelationOneToOne(valueField = "userName", selfField = "leader", joinSelfColumn = "leader", targetField = "userId", joinTargetColumn = "user_id", targetTable = "sys_user")
    @ExcelProperty(value = "负责人")
    private String leaderName;

    /**
     * 联系电话
     */
    @ExcelProperty(value = "联系电话")
    private String phone;
    /**
     * 邮箱
     */
    @ExcelProperty(value = "邮箱")
    private String email;
    /**
     * 部门状态（0正常 1停用）
     */
    @ExcelProperty(value = "部门状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;
    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 子部门
     */
    private List<SysDeptVo> children = new ArrayList<>();

}
