package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysDictType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典类型视图对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
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
    private Long dictId;
    /**
     * 字典key
     */
    private String dictKey;
    /**
     * 字典名称
     */
    private String dictName;
    /**
     * 字典类型 L 列表 T 树
     */
    private String dictType;
    /**
     * 备注
     */
    private String remark;

}
