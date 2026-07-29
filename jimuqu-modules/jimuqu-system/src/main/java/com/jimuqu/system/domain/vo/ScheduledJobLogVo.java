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

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 执行日志主键。
     */
    private Long logId;

    /**
     * Solon 任务唯一名称。
     */
    private String jobName;

    /**
     * 同一次触发及其重试共享的执行链标识。
     */
    private String executionId;

    /**
     * 执行状态。
     */
    private String status;

    /**
     * 触发类型。
     */
    private String triggerType;

    /**
     * 当前尝试次数。
     */
    private Integer attempt;

    /**
     * 执行实例标识。
     */
    private String instanceId;

    /**
     * 开始时间。
     */
    private Date startTime;

    /**
     * 结束时间。
     */
    private Date endTime;

    /**
     * 执行耗时毫秒数。
     */
    private Long durationMs;

    /**
     * 异常摘要。
     */
    private String errorSummary;
}
