package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysJobBo;
import com.jimuqu.system.domain.query.SysJobQuery;
import com.jimuqu.system.domain.vo.SysJobVo;
import com.jimuqu.system.job.SysJobHandlerVo;
import com.jimuqu.system.service.SysJobService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 定时任务Controller
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/job")
public class SysJobController extends BaseController {

    private final SysJobService sysJobService;

    /**
     * 查询定时任务列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:job:list")
    public Page<SysJobVo> list(SysJobQuery query, PageQuery pageQuery) {
        return sysJobService.queryPageList(query, pageQuery);
    }

    /**
     * 查询可用处理器列表
     */
    @Get
    @Mapping("/handlers")
    @SaCheckPermission("system:job:list")
    public List<SysJobHandlerVo> handlers() {
        return sysJobService.listHandlers();
    }

    /**
     * 获取定时任务详细信息
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:job:query")
    public SysJobVo getInfo(@NotNull(message = "定时任务主键不能为空") Long id) {
        return sysJobService.queryById(id);
    }

    /**
     * 新增定时任务
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:job:add")
    @Log(title = "新增定时任务", businessType = BusinessType.ADD)
    public Long add(@Validated(AddGroup.class) SysJobBo bo) {
        boolean result = sysJobService.insertByBo(bo);
        Assert.isTrue(result, "新增定时任务失败");
        return bo.getId();
    }

    /**
     * 更新定时任务
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:job:update")
    @Log(title = "更新定时任务", businessType = BusinessType.UPDATE)
    public void edit(@Validated(UpdateGroup.class) SysJobBo bo) {
        boolean result = sysJobService.updateByBo(bo);
        Assert.isTrue(result, "更新定时任务失败");
    }

    /**
     * 删除定时任务
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:job:delete")
    @Log(title = "删除定时任务", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysJobService.deleteByIds(ids);
        Assert.gtZero(num, "删除定时任务失败");
        return num;
    }

    /**
     * 启动定时任务
     */
    @Mapping("/start/{id}")
    @SaCheckPermission("system:job:start")
    @Log(title = "启动定时任务", businessType = BusinessType.UPDATE)
    public void start(@NotNull(message = "定时任务主键不能为空") Long id) {
        boolean result = sysJobService.start(id);
        Assert.isTrue(result, "启动定时任务失败");
    }

    /**
     * 停止定时任务
     */
    @Mapping("/stop/{id}")
    @SaCheckPermission("system:job:stop")
    @Log(title = "停止定时任务", businessType = BusinessType.UPDATE)
    public void stop(@NotNull(message = "定时任务主键不能为空") Long id) {
        boolean result = sysJobService.stop(id);
        Assert.isTrue(result, "停止定时任务失败");
    }

    /**
     * 手动执行一次
     */
    @Mapping("/run/{id}")
    @SaCheckPermission("system:job:run")
    @Log(title = "手动执行定时任务", businessType = BusinessType.OTHER)
    public void run(@NotNull(message = "定时任务主键不能为空") Long id) {
        boolean result = sysJobService.run(id);
        Assert.isTrue(result, "手动执行定时任务失败");
    }
}
