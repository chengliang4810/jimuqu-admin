package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.Ignores;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.system.domain.SysPost;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 岗位信息视图对象
 * @author chengliang4810
 * @since 2025-06-04
 */
@Data
@ExcelIgnoreUnannotated
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysPost.class)
@AutoMapper(target = SysPost.class)
@Ignores("deptName")
public class SysPostVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 岗位ID
     */
    @ExcelProperty(value = "岗位序号")
    private Long postId;
    /**
     * 部门id
     */
    @ExcelProperty(value = "部门id")
    private Long deptId;
    /**
     * 岗位编码
     */
    @ExcelProperty(value = "岗位编码")
    private String postCode;
    /**
     * 岗位名称
     */
    @ExcelProperty(value = "岗位名称")
    private String postName;
    /**
     * 岗位类别编码
     */
    @ExcelProperty(value = "类别编码")
    private String postCategory;
    /**
     * 显示顺序
     */
    @ExcelProperty(value = "岗位排序")
    private Integer postSort;
    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;
    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 部门名称
     */
    private String deptName;

}
