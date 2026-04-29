package com.jimuqu.system.domain.bo;

import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysJobLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 定时任务运行日志业务对象
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysJobLog.class, reverseConvertGenerate = false)
public class SysJobLogBo extends BoBaseEntity {

    /**
     * 日志主键
     */
    private Long id;

    /**
     * 任务主键
     */
    private Long jobId;

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
     * 运行状态（0成功 1失败 2跳过）
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMessage;
}
