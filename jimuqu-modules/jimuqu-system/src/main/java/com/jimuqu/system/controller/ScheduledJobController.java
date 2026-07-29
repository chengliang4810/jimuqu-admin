package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.ScheduledJobConfigBo;
import com.jimuqu.system.domain.bo.ScheduledJobDefinitionBo;
import com.jimuqu.system.domain.query.ScheduledJobLogQuery;
import com.jimuqu.system.domain.vo.ScheduledJobHandlerVo;
import com.jimuqu.system.domain.vo.ScheduledJobLogVo;
import com.jimuqu.system.domain.vo.ScheduledJobVo;
import com.jimuqu.system.service.ScheduledJobService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * Solon 运行时定时任务管理。
 */
@Controller
@RequiredArgsConstructor
@Mapping("/monitor/job")
public class ScheduledJobController extends BaseController {

    /**
     * 定时任务应用服务。
     */
    private final ScheduledJobService scheduledJobService;

    /**
     * 查询代码任务与在线创建的动态任务。
     *
     * @return 定时任务列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("monitor:job:list")
    public R<List<ScheduledJobVo>> list() {
        return R.ok(scheduledJobService.list());
    }

    /**
     * 查询可供在线任务调用的白名单处理器。
     *
     * @return 白名单处理器
     */
    @Get
    @Mapping("/handlers")
    @SaCheckPermission("monitor:job:list")
    public R<List<ScheduledJobHandlerVo>> handlers() {
        return R.ok(scheduledJobService.listHandlers());
    }

    /**
     * 新增在线定时任务。
     *
     * @param bo 任务定义
     * @return 操作结果
     */
    @Post
    @Mapping
    @SaCheckPermission("monitor:job:add")
    @Log(title = "定时任务", businessType = BusinessType.ADD)
    public R<Void> create(
            @Body @Validated ScheduledJobDefinitionBo bo) {
        scheduledJobService.create(bo);
        return R.ok();
    }

    /**
     * 更新在线定时任务。
     *
     * @param jobName 任务名称
     * @param bo 任务定义
     * @return 操作结果
     */
    @Put
    @Mapping("/{jobName}")
    @SaCheckPermission("monitor:job:edit")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    public R<Void> update(
            @NotBlank(message = "任务名称不能为空") String jobName,
            @Body @Validated ScheduledJobDefinitionBo bo) {
        scheduledJobService.update(jobName, bo);
        return R.ok();
    }

    /**
     * 删除在线定时任务。
     *
     * @param jobName 任务名称
     * @return 操作结果
     */
    @Delete
    @Mapping("/{jobName}")
    @SaCheckPermission("monitor:job:remove")
    @Log(title = "定时任务", businessType = BusinessType.DELETE)
    public R<Void> delete(
            @NotBlank(message = "任务名称不能为空") String jobName) {
        scheduledJobService.delete(jobName);
        return R.ok();
    }

    /**
     * 启用指定任务。
     *
     * @param jobName 任务名称
     * @return 操作结果
     */
    @Put
    @Mapping("/{jobName}/start")
    @SaCheckPermission("monitor:job:changeStatus")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    public R<Void> start(@NotBlank(message = "任务名称不能为空") String jobName) {
        scheduledJobService.start(jobName);
        return R.ok();
    }

    /**
     * 停用指定任务。
     *
     * @param jobName 任务名称
     * @return 操作结果
     */
    @Put
    @Mapping("/{jobName}/stop")
    @SaCheckPermission("monitor:job:changeStatus")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    public R<Void> stop(@NotBlank(message = "任务名称不能为空") String jobName) {
        scheduledJobService.stop(jobName);
        return R.ok();
    }

    /**
     * 立即异步执行一次任务。
     *
     * @param jobName 任务名称
     * @return 提交结果
     */
    @Post
    @Mapping("/{jobName}/run")
    @SaCheckPermission("monitor:job:run")
    @Log(title = "定时任务", businessType = BusinessType.OTHER)
    public R<Void> run(@NotBlank(message = "任务名称不能为空") String jobName) {
        scheduledJobService.run(jobName);
        return R.ok("定时任务已提交执行");
    }

    /**
     * 更新指定任务的失败重试配置。
     *
     * @param jobName 任务名称
     * @param bo 重试配置
     * @return 操作结果
     */
    @Put
    @Mapping("/{jobName}/config")
    @SaCheckPermission("monitor:job:edit")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    public R<Void> updateConfig(@NotBlank(message = "任务名称不能为空") String jobName,
                                @Body @Validated ScheduledJobConfigBo bo) {
        scheduledJobService.updateConfig(jobName, bo);
        return R.ok();
    }

    /**
     * 分页查询任务执行记录。
     *
     * @param query 日志筛选条件
     * @param pageQuery 分页参数
     * @return 执行记录分页结果
     */
    @Get
    @Mapping("/log/list")
    @SaCheckPermission("monitor:job:log:list")
    public Page<ScheduledJobLogVo> logList(ScheduledJobLogQuery query, PageQuery pageQuery) {
        return scheduledJobService.queryLogPage(query, pageQuery);
    }

    /**
     * 批量删除任务执行记录。
     *
     * @param ids 执行记录主键
     * @return 操作结果
     */
    @Delete
    @Mapping("/log/{ids}")
    @SaCheckPermission("monitor:job:log:remove")
    @Log(title = "定时任务执行日志", businessType = BusinessType.DELETE)
    public R<Void> deleteLogs(@NotEmpty(message = "执行日志ID不能为空") List<Long> ids) {
        Assert.gtZero(scheduledJobService.deleteLogs(ids), "删除执行日志失败");
        return R.ok();
    }

    /**
     * 清空全部任务执行记录。
     *
     * @return 操作结果
     */
    @Delete
    @Mapping("/log/clean")
    @SaCheckPermission("monitor:job:log:remove")
    @Log(title = "定时任务执行日志", businessType = BusinessType.CLEAN)
    public R<Void> cleanLogs() {
        scheduledJobService.cleanLogs();
        return R.ok();
    }
}
