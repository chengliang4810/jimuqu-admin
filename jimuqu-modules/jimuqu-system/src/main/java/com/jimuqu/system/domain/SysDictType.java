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
import org.dromara.autotable.annotation.enums.IndexTypeEnum;

import java.io.Serial;

/**
 * 字典类型
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_dict_type")
public class SysDictType extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "字典主键")
    private Long dictId;
    /**
     * 字典key
     */
    @AutoColumn(comment = "字典key", length = 100)
    @Index(name = "uk_sys_dict_type_dict_key", type = IndexTypeEnum.UNIQUE)
    private String dictKey;
    /**
     * 字典名称
     */
    @AutoColumn(comment = "字典名称", length = 100)
    private String dictName;
    /**
     * 字典类型 L 列表 T 树
     */
    @AutoColumn(comment = "字典类型 L 列表 T 树", length = 100, defaultValue = "L")
    private String dictType;
    /**
     * 系统内置（Y是 N否）
     */
    @AutoColumn(comment = "系统内置（Y是 N否）", length = 1, defaultValue = "N")
    private String isBuiltIn;
    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;

}
