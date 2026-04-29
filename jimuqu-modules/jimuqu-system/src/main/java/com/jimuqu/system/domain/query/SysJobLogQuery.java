package com.jimuqu.system.domain.query;

import cn.xbatis.core.sql.ObjectConditionLifeCycle;
import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysJobLog;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.EQ;
import static cn.xbatis.db.annotations.Condition.Type.LIKE;

/**
 * 定时任务运行日志查询条件对象
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@FieldNameConstants
@ConditionTarget(SysJobLog.class)
public class SysJobLogQuery implements Serializable, ObjectConditionLifeCycle {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    @Condition(value = EQ)
    private Long id;

    /**
     * 任务主键
     */
    @Condition(value = EQ)
    private Long jobId;

    /**
     * 任务名称
     */
    @Condition(value = LIKE)
    private String jobName;

    /**
     * 白名单处理器标识
     */
    @Condition(value = EQ)
    private String handlerKey;

    /**
     * 运行状态（0成功 1失败 2跳过）
     */
    @Condition(value = EQ)
    private Integer status;

    @Override
    public void beforeBuildCondition() {
    }
}
