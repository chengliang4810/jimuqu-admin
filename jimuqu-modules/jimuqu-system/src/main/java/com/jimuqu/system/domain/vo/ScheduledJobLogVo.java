package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysScheduledJobLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务执行日志。
 */
@Data
@ResultEntity(SysScheduledJobLog.class)
public class ScheduledJobLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long logId;
    private String jobName;
    private String status;
    private String triggerType;
    private Integer attempt;
    private String instanceId;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private String errorSummary;
}
