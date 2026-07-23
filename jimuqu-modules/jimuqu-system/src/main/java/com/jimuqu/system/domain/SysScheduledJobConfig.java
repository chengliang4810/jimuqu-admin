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
 * 代码注册定时任务的持久化运行配置。
 */
@Data
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table("sys_scheduled_job_config")
public class SysScheduledJobConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "配置ID")
    private Long configId;

    @Index(name = "uk_sys_scheduled_job_name", type = IndexTypeEnum.UNIQUE)
    @AutoColumn(comment = "Solon任务唯一名称", length = 255, notNull = true)
    private String jobName;

    @AutoColumn(comment = "是否启用", notNull = true, defaultValue = "1")
    private Boolean enabled;

    @AutoColumn(comment = "最大重试次数", notNull = true, defaultValue = "0")
    private Integer maxRetries;

    @AutoColumn(comment = "重试间隔毫秒数", notNull = true, defaultValue = "1000")
    private Long retryIntervalMs;

    @AutoColumn(comment = "启停控制版本", notNull = true, defaultValue = "0")
    private Long controlVersion;
}
