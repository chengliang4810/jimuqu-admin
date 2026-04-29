package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysJob;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

/**
 * 定时任务查询条件对象
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@FieldNameConstants
@ConditionTarget(SysJob.class)
public class SysJobQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务主键
     */
    @Condition(value = EQ)
    private Long id;

    /**
     * 任务名称
     */
    @Condition(value = LIKE)
    private String jobName;

    /**
     * 任务分组
     */
    @Condition(value = EQ)
    private String jobGroup;

    /**
     * 白名单处理器标识
     */
    @Condition(value = EQ)
    private String handlerKey;

    /**
     * 状态（0启用 1停用）
     */
    @Condition(value = EQ)
    private Integer status;

    @Override
    public void beforeBuildCondition() {
    }
}
