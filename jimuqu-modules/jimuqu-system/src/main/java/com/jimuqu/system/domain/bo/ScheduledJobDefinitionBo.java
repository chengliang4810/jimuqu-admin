package com.jimuqu.system.domain.bo;

import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Pattern;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线定时任务定义。
 */
@Data
public class ScheduledJobDefinitionBo implements Serializable {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务唯一名称。
     */
    @NotBlank(message = "任务名称不能为空")
    @Length(max = 64, message = "任务名称不能超过64个字符")
    @Pattern(value = "[A-Za-z][A-Za-z0-9_.-]*",
            message = "任务名称必须以字母开头且只能包含字母、数字、点、横线和下划线")
    private String jobName;

    /**
     * 任务说明。
     */
    @NotBlank(message = "任务说明不能为空")
    @Length(max = 200, message = "任务说明不能超过200个字符")
    private String description;

    /**
     * 白名单处理器标识。
     */
    @NotBlank(message = "任务处理器不能为空")
    @Length(max = 128, message = "任务处理器标识不能超过128个字符")
    private String handlerKey;

    /**
     * 调度类型。
     */
    @NotBlank(message = "调度类型不能为空")
    private String scheduleType;

    /**
     * Cron 表达式或毫秒间隔。
     */
    @NotBlank(message = "调度表达式不能为空")
    @Length(max = 128, message = "调度表达式不能超过128个字符")
    private String scheduleExpression;

    /**
     * Cron 时区。
     */
    @Length(max = 64, message = "时区不能超过64个字符")
    private String zone;

    /**
     * 首次执行延迟毫秒数。
     */
    @NotNull(message = "首次执行延迟不能为空")
    private Long initialDelayMs;

    /**
     * 并发策略。
     */
    @NotBlank(message = "并发策略不能为空")
    private String concurrentPolicy;

    /**
     * 错过执行策略。
     */
    @NotBlank(message = "错过执行策略不能为空")
    private String misfirePolicy;

    /**
     * 最大重试次数。
     */
    @NotNull(message = "最大重试次数不能为空")
    private Integer maxRetries;

    /**
     * 重试间隔毫秒数。
     */
    @NotNull(message = "重试间隔不能为空")
    private Long retryIntervalMs;
}
