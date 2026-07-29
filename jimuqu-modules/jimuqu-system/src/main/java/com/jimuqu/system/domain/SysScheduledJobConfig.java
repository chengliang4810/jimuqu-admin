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
import org.dromara.autotable.annotation.enums.IndexTypeEnum;

import java.io.Serial;

/**
 * 定时任务持久化定义与运行配置。
 */
@Data
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table("sys_scheduled_job_config")
public class SysScheduledJobConfig extends BaseEntity {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置主键。
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "配置ID")
    private Long configId;

    /**
     * Solon 任务唯一名称。
     */
    @Index(name = "uk_sys_scheduled_job_name", type = IndexTypeEnum.UNIQUE)
    @AutoColumn(comment = "Solon任务唯一名称", length = 255, notNull = true)
    private String jobName;

    /**
     * 任务来源：SYSTEM 或 DYNAMIC。
     */
    @AutoColumn(comment = "任务来源", length = 16, notNull = true, defaultValue = "SYSTEM")
    private String jobSource;

    /**
     * 任务说明。
     */
    @AutoColumn(comment = "任务说明", length = 200)
    private String description;

    /**
     * 动态任务白名单处理器标识。
     */
    @AutoColumn(comment = "白名单处理器标识", length = 128)
    private String handlerKey;

    /**
     * 调度类型：CRON、FIXED_RATE 或 FIXED_DELAY。
     */
    @AutoColumn(comment = "调度类型", length = 16)
    private String scheduleType;

    /**
     * Cron 表达式或毫秒间隔。
     */
    @AutoColumn(comment = "调度表达式", length = 128)
    private String scheduleExpression;

    /**
     * Cron 时区。
     */
    @AutoColumn(comment = "Cron时区", length = 64)
    private String zone;

    /**
     * 首次执行延迟毫秒数。
     */
    @AutoColumn(comment = "首次执行延迟毫秒数", notNull = true, defaultValue = "0")
    private Long initialDelayMs;

    /**
     * 是否启用。
     */
    @AutoColumn(comment = "是否启用", notNull = true, defaultValue = "1")
    private Boolean enabled;

    /**
     * 并发策略：ALLOW 或 FORBID。
     */
    @AutoColumn(comment = "并发策略", length = 16, notNull = true, defaultValue = "ALLOW")
    private String concurrentPolicy;

    /**
     * 错过执行策略：IGNORE 或 FIRE_ONCE。
     */
    @AutoColumn(comment = "错过执行策略", length = 16, notNull = true, defaultValue = "IGNORE")
    private String misfirePolicy;

    /**
     * 最大重试次数。
     */
    @AutoColumn(comment = "最大重试次数", notNull = true, defaultValue = "0")
    private Integer maxRetries;

    /**
     * 重试间隔毫秒数。
     */
    @AutoColumn(comment = "重试间隔毫秒数", notNull = true, defaultValue = "1000")
    private Long retryIntervalMs;

    /**
     * 启停与定义变更控制版本。
     */
    @AutoColumn(comment = "启停控制版本", notNull = true, defaultValue = "0")
    private Long controlVersion;
}
