package com.jimuqu.system.domain.bo;

import lombok.Data;
import org.noear.solon.validation.annotation.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定时任务重试配置。
 */
@Data
public class ScheduledJobConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "最大重试次数不能为空")
    private Integer maxRetries;

    @NotNull(message = "重试间隔不能为空")
    private Long retryIntervalMs;
}
