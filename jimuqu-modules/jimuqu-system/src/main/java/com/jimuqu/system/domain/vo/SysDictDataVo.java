package com.jimuqu.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.system.domain.SysDictData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 字典数据视图对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@ExcelIgnoreUnannotated
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysDictData.class)
@AutoMapper(target = SysDictData.class)
public class SysDictDataVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典ID
     */
    @ONodeAttr(name = "dictCode")
    @ExcelProperty(value = "字典编码")
    private Long id;
    /**
     * 父级ID
     */
    private Long parentId;
    /**
     * 字典排序
     */
    @ExcelProperty(value = "字典排序")
    private Integer dictSort;
    /**
     * 字典标签
     */
    @ExcelProperty(value = "字典标签")
    private String dictLabel;
    /**
     * 字典键值
     */
    @ExcelProperty(value = "字典键值")
    private String dictValue;
    /**
     * 字典类型
     */
    @ONodeAttr(name = "dictType")
    @ExcelProperty(value = "字典类型")
    private String dictTypeKey;
    /**
     * 样式属性（其他样式扩展）
     */
    private String cssClass;
    /**
     * 表格回显样式
     */
    private String listClass;
    /**
     * 是否默认（Y是 N否）
     */
    @ExcelProperty(value = "是否默认", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    private String isDefault;
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
