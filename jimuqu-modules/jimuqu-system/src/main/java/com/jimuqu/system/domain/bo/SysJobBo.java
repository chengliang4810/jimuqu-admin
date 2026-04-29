package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysJob;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 定时任务业务对象
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysJob.class, reverseConvertGenerate = false)
public class SysJobBo extends BoBaseEntity {

    /**
     * 任务主键
     */
    @NotNull(message = "任务主键不能为空", groups = { UpdateGroup.class })
    private Long id;

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 白名单处理器标识
     */
    @NotBlank(message = "处理器标识不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String handlerKey;

    /**
     * 处理器参数JSON
     */
    private String handlerParam;

    /**
     * cron表达式
     */
    @NotBlank(message = "cron表达式不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String cronExpression;

    /**
     * 状态（0启用 1停用）
     */
    @NotNull(message = "状态不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Integer status;

    /**
     * 是否允许并发执行
     */
    private Boolean allowConcurrent;

    /**
     * 备注
     */
    private String remark;
}
