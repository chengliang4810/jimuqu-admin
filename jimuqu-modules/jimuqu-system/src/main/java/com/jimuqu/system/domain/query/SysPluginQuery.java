package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysPlugin;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

/**
 * 在线插件查询条件对象。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Data
@FieldNameConstants
@ConditionTarget(SysPlugin.class)
public class SysPluginQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 插件编码。
     */
    @Condition(value = LIKE)
    private String pluginKey;

    /**
     * 插件名称。
     */
    @Condition(value = LIKE)
    private String pluginName;

    /**
     * 插件类型。
     */
    @Condition(value = EQ)
    private String pluginType;

    /**
     * 状态（0启用 1停用）。
     */
    @Condition(value = EQ)
    private Integer status;

    @Override
    public void beforeBuildCondition() {
    }
}
