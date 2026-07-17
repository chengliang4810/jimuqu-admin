package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysDictData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典数据视图对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
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
    private Long id;
    /**
     * 父级ID
     */
    private Long parentId;
    /**
     * 字典排序
     */
    private Long dictSort;
    /**
     * 字典标签
     */
    private String dictLabel;
    /**
     * 字典键值
     */
    private String dictValue;
    /**
     * 字典类型
     */
    @ONodeAttr(name = "dictType")
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
    private String isDefault;
    /**
     * 备注
     */
    private String remark;

}
