package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysDictType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 字典类型视图对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@ExcelIgnoreUnannotated
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysDictType.class)
@AutoMapper(target = SysDictType.class)
public class SysDictTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     */
    @ExcelProperty(value = "字典主键")
    private Long dictId;
    /**
     * 字典名称
     */
    @ExcelProperty(value = "字典名称")
    private String dictName;
    /**
     * 字典key
     */
    @ONodeAttr(name = "dictType")
    @ExcelProperty(value = "字典类型")
    private String dictKey;
    /**
     * 字典类型 L 列表 T 树
     */
    @ONodeAttr(ignore = true)
    private String dictType;
    /**
     * 系统内置（Y是 N否）
     */
    private String isBuiltIn;
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

}
