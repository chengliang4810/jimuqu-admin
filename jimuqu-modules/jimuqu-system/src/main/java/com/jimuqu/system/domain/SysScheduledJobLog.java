package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.Index;

import java.io.Serial;
import java.util.Date;

/**
 * 定时任务每次执行尝试的结果。
 */
@Data
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table("sys_scheduled_job_log")
public class SysScheduledJobLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "执行日志ID")
    private Long logId;

    @Index(name = "sys_scheduled_job_log_name")
    @AutoColumn(comment = "Solon任务唯一名称", length = 255, notNull = true)
    private String jobName;

    @AutoColumn(comment = "执行状态", length = 16, notNull = true)
    private String status;

    @AutoColumn(comment = "触发类型", length = 16, notNull = true)
    private String triggerType;

    @AutoColumn(comment = "尝试次数", notNull = true)
    private Integer attempt;

    @AutoColumn(comment = "执行实例标识", length = 128, notNull = true)
    private String instanceId;

    @Index(name = "sys_scheduled_job_log_start")
    @AutoColumn(comment = "开始时间", notNull = true)
    private Date startTime;

    @AutoColumn(comment = "结束时间", notNull = true)
    private Date endTime;

    @AutoColumn(comment = "耗时毫秒数", notNull = true)
    private Long durationMs;

    @AutoColumn(comment = "异常摘要", length = 1000)
    private String errorSummary;
}
