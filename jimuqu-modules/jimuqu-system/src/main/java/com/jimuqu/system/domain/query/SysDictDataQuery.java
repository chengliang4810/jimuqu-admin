package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysDictData;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.*;

/**
 * 字典数据查询条件对象
 * @author chengliang4810
 * @since 2025-05-27
 */
@Data
@FieldNameConstants
@ConditionTarget(SysDictData.class)
public class SysDictDataQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典ID
     */
    @Condition(value = EQ)
    private Long id;
    @Condition(value = EQ, property = "id")
    private Long dictCode;
    /**
     * 父级ID
     */
    @Condition(value = EQ)
    private Long parentId;
    /**
     * 字典排序
     */
    @Condition(value = EQ)
    private Long dictSort;
    /**
     * 字典标签
     */
    @Condition(value = EQ)
    private String dictLabel;
    /**
     * 字典键值
     */
    @Condition(value = EQ)
    private String dictValue;
    /**
     * 字典类型
     */
    @Condition(value = EQ)
    private String dictTypeKey;
    @Condition(value = EQ, property = "dictTypeKey")
    private String dictType;
    /**
     * 样式属性（其他样式扩展）
     */
    @Condition(value = EQ)
    private String cssClass;
    /**
     * 表格回显样式
     */
    @Condition(value = EQ)
    private String listClass;
    /**
     * 是否默认（Y是 N否）
     */
    @Condition(value = EQ)
    private String isDefault;
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
