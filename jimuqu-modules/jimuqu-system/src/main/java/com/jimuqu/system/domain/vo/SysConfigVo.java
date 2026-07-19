package com.jimuqu.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.system.domain.SysConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 参数配置视图对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@ExcelIgnoreUnannotated
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysConfig.class)
@AutoMapper(target = SysConfig.class)
public class SysConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数主键
     */
    @JsonProperty("configId")
    @ONodeAttr(name = "configId")
    @ExcelProperty(value = "参数主键")
    private Long id;
    /**
     * 参数名称
     */
    @ExcelProperty(value = "参数名称")
    private String configName;
    /**
     * 参数键名
     */
    @ExcelProperty(value = "参数键名")
    private String configKey;
    /**
     * 参数键值
     */
    @ExcelProperty(value = "参数键值")
    private String configValue;
    /**
     * 系统内置（Y是 N否）
     */
    @ExcelProperty(value = "系统内置", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    private String configType;
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
