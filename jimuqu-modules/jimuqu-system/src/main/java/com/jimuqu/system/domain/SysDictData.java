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
 * 字典数据
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_dict_data")
public class SysDictData extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典ID
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "字典ID")
    private Long id;
    /**
     * 父级ID
     */
    @AutoColumn(comment = "父级ID", defaultValue = "0")
    private Long parentId;
    /**
     * 字典排序
     */
    @AutoColumn(comment = "字典排序", defaultValue = "0")
    private Integer dictSort;
    /**
     * 字典标签
     */
    @AutoColumn(comment = "字典标签", length = 100)
    private String dictLabel;
    /**
     * 字典键值
     */
    @AutoColumn(comment = "字典键值", length = 100)
    private String dictValue;
    /**
     * 字典类型
     */
    @AutoColumn(comment = "字典类型", length = 100, defaultValue = "L")
    @Index(name = "sys_dict_data_type")
    private String dictTypeKey;
    /**
     * 样式属性（其他样式扩展）
     */
    @AutoColumn(comment = "样式属性（其他样式扩展）", length = 100)
    private String cssClass;
    /**
     * 表格回显样式
     */
    @AutoColumn(comment = "表格回显样式", length = 100)
    private String listClass;
    /**
     * 是否默认（Y是 N否）
     */
    @AutoColumn(comment = "是否默认（Y是 N否）", length = 1, defaultValue = "N")
    private String isDefault;
    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;

}
