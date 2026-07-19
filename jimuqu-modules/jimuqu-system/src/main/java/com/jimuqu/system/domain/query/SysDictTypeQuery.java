package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysDictType;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.*;

/**
 * 字典类型查询条件对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@FieldNameConstants
@ConditionTarget(SysDictType.class)
public class SysDictTypeQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     */
    @Condition(value = EQ)
    private Long dictId;
    /**
     * 字典key
     */
    @Condition(value = EQ)
    private String dictKey;
    /**
     * 字典名称
     */
    @Condition(value = LIKE)
    private String dictName;
    /**
     * 字典类型 L 列表 T 树
     */
    @Condition(value = LIKE, property = "dictKey")
    private String dictType;
    /**
     * 备注
     */
    @Condition(value = EQ)
    private String remark;

    /**
     * 条件构建前执行
     */
    @Override
    public void beforeBuildCondition() {
        
    }

}
