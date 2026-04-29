package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

import java.io.Serial;
import java.util.Date;

/**
 * 定时任务
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_job")
public class SysJob extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务主键
     */
    @TableId(value = IdAutoType.GENERATOR, generatorName = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "任务主键")
    private Long id;

    /**
     * 任务名称
     */
    @AutoColumn(comment = "任务名称", length = 100)
    private String jobName;

    /**
     * 任务分组
     */
    @AutoColumn(comment = "任务分组", length = 100, defaultValue = "DEFAULT")
    private String jobGroup;

    /**
     * 白名单处理器标识
     */
    @AutoColumn(comment = "白名单处理器标识", length = 200)
    private String handlerKey;

    /**
     * 处理器参数JSON
     */
    @AutoColumn(comment = "处理器参数JSON", type = MysqlTypeConstant.TEXT)
    private String handlerParam;

    /**
     * cron表达式
     */
    @AutoColumn(comment = "cron表达式", length = 100)
    private String cronExpression;

    /**
     * 状态（0启用 1停用）
     */
    @AutoColumn(comment = "状态（0启用 1停用）", defaultValue = "1")
    private Integer status;

    /**
     * 是否允许并发执行
     */
    @AutoColumn(comment = "是否允许并发执行", defaultValue = "false")
    private Boolean allowConcurrent;

    /**
     * 上次运行时间
     */
    @AutoColumn(comment = "上次运行时间", type = MysqlTypeConstant.DATETIME)
    private Date lastRunTime;

    /**
     * 下次运行时间
     */
    @AutoColumn(comment = "下次运行时间", type = MysqlTypeConstant.DATETIME)
    private Date nextRunTime;

    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;
}
