package com.jimuqu.system.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * Solon 运行时定时任务。
 */
@Data
@Accessors(chain = true)
public class ScheduledJobVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Solon 任务唯一名称。
     */
    private String jobName;

    /**
     * 任务显示名称。
     */
    private String description;

    /**
     * 调度类型：CRON、FIXED_RATE 或 FIXED_DELAY。
     */
    private String scheduleType;

    /**
     * Cron 表达式或毫秒间隔。
     */
    private String scheduleExpression;

    /**
     * 当前管理状态。
     */
    private boolean enabled;

    /**
     * 最大重试次数。
     */
    private int maxRetries;

    /**
     * 重试间隔毫秒数。
     */
    private long retryIntervalMs;
}
