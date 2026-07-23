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
import com.jimuqu.system.domain.query.ScheduledJobLogQuery;
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

    private final ScheduledJobService scheduledJobService;

    @Get
    @Mapping("/list")
    @SaCheckPermission("monitor:job:list")
    public R<List<ScheduledJobVo>> list() {
        return R.ok(scheduledJobService.list());
    }

    @Put
    @Mapping("/{jobName}/start")
    @SaCheckPermission("monitor:job:changeStatus")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    public R<Void> start(@NotBlank(message = "任务名称不能为空") String jobName) {
        scheduledJobService.start(jobName);
        return R.ok();
    }

    @Put
    @Mapping("/{jobName}/stop")
    @SaCheckPermission("monitor:job:changeStatus")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    public R<Void> stop(@NotBlank(message = "任务名称不能为空") String jobName) {
        scheduledJobService.stop(jobName);
        return R.ok();
    }

    @Post
    @Mapping("/{jobName}/run")
    @SaCheckPermission("monitor:job:run")
    @Log(title = "定时任务", businessType = BusinessType.OTHER)
    public R<Void> run(@NotBlank(message = "任务名称不能为空") String jobName) {
        scheduledJobService.run(jobName);
        return R.ok("定时任务已提交执行");
    }

    @Put
    @Mapping("/{jobName}/config")
    @SaCheckPermission("monitor:job:edit")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    public R<Void> updateConfig(@NotBlank(message = "任务名称不能为空") String jobName,
                                @Body @Validated ScheduledJobConfigBo bo) {
        scheduledJobService.updateConfig(jobName, bo);
        return R.ok();
    }

    @Get
    @Mapping("/log/list")
    @SaCheckPermission("monitor:job:log:list")
    public Page<ScheduledJobLogVo> logList(ScheduledJobLogQuery query, PageQuery pageQuery) {
        return scheduledJobService.queryLogPage(query, pageQuery);
    }

    @Delete
    @Mapping("/log/{ids}")
    @SaCheckPermission("monitor:job:log:remove")
    @Log(title = "定时任务执行日志", businessType = BusinessType.DELETE)
    public R<Void> deleteLogs(@NotEmpty(message = "执行日志ID不能为空") List<Long> ids) {
        Assert.gtZero(scheduledJobService.deleteLogs(ids), "删除执行日志失败");
        return R.ok();
    }

    @Delete
    @Mapping("/log/clean")
    @SaCheckPermission("monitor:job:log:remove")
    @Log(title = "定时任务执行日志", businessType = BusinessType.CLEAN)
    public R<Void> cleanLogs() {
        scheduledJobService.cleanLogs();
        return R.ok();
    }
}
