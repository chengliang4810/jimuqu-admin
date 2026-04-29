package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysJob;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务视图对象
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysJob.class)
@AutoMapper(target = SysJob.class)
public class SysJobVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务主键
     */
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 白名单处理器标识
     */
    private String handlerKey;

    /**
     * 处理器参数JSON
     */
    private String handlerParam;

    /**
     * cron表达式
     */
    private String cronExpression;

    /**
     * 状态（0启用 1停用）
     */
    private Integer status;

    /**
     * 是否允许并发执行
     */
    private Boolean allowConcurrent;

    /**
     * 上次运行时间
     */
    private Date lastRunTime;

    /**
     * 下次运行时间
     */
    private Date nextRunTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
