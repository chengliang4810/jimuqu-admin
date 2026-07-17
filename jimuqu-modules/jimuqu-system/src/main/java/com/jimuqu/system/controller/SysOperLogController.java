package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.query.SysOperLogQuery;
import com.jimuqu.system.domain.vo.SysOperLogVo;
import com.jimuqu.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.DownloadedFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Mapping("/monitor/operlog")
public class SysOperLogController extends BaseController {

    private final SysOperLogService service;

    @Get
    @Mapping("/list")
    @SaCheckPermission("monitor:operlog:list")
    public Page<SysOperLogVo> list(SysOperLogQuery query, PageQuery pageQuery) {
        return service.queryPage(query, pageQuery);
    }

    @Post
    @Mapping("/export")
    @Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @SaCheckPermission("monitor:operlog:export")
    public DownloadedFile export(SysOperLogQuery query) {
        return ExcelUtil.exportExcel(service.queryList(query), "操作日志", SysOperLogVo.class);
    }

    @Delete
    @Mapping("/{ids}")
    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("monitor:operlog:remove")
    public R<Void> remove(List<Long> ids) {
        return toAjax(service.delete(ids));
    }

    @Delete
    @Mapping("/clean")
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("monitor:operlog:remove")
    public R<Void> clean() {
        service.clean();
        return R.ok();
    }
}
