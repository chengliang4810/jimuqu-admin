package com.jimuqu.system.domain.query;

import cn.xbatis.db.annotations.Condition;
import cn.xbatis.db.annotations.ConditionTarget;
import com.jimuqu.system.domain.SysScheduledJobLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static cn.xbatis.db.annotations.Condition.Type.EQ;

/**
 * 定时任务执行日志查询。
 */
@Data
@ConditionTarget(SysScheduledJobLog.class)
public class ScheduledJobLogQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Condition(EQ)
    private String jobName;

    @Condition(EQ)
    private String status;

    @Condition(EQ)
    private String triggerType;
}
