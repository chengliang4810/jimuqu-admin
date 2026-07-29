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

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Solon 任务唯一名称。
     */
    private String jobName;

    /**
     * 任务来源：SYSTEM 或 DYNAMIC。
     */
    private String jobSource;

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
     * 动态任务白名单处理器标识。
     */
    private String handlerKey;

    /**
     * Cron 时区。
     */
    private String zone;

    /**
     * 首次执行延迟毫秒数。
     */
    private long initialDelayMs;

    /**
     * 当前管理状态。
     */
    private boolean enabled;

    /**
     * 当前节点运行状态：RUNNING、STOPPED 或 ERROR。
     */
    private String runtimeStatus;

    /**
     * 当前节点运行异常说明。
     */
    private String runtimeError;

    /**
     * 并发策略：ALLOW 或 FORBID。
     */
    private String concurrentPolicy;

    /**
     * 错过执行策略：IGNORE 或 FIRE_ONCE。
     */
    private String misfirePolicy;

    /**
     * 最大重试次数。
     */
    private int maxRetries;

    /**
     * 重试间隔毫秒数。
     */
    private long retryIntervalMs;
}
