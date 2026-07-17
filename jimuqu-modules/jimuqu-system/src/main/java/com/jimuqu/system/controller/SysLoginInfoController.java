package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.query.SysLoginInfoQuery;
import com.jimuqu.system.domain.vo.SysLoginInfoVo;
import com.jimuqu.system.service.SysLoginInfoService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.data.cache.CacheService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Mapping("/monitor/loginInfo")
public class SysLoginInfoController extends BaseController {

    private final SysLoginInfoService service;
    private final CacheService cacheService;

    @Get
    @Mapping("/list")
    @SaCheckPermission("monitor:logininfor:list")
    public Page<SysLoginInfoVo> list(SysLoginInfoQuery query, PageQuery pageQuery) {
        return service.queryPage(query, pageQuery);
    }

    @Post
    @Mapping("/export")
    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @SaCheckPermission("monitor:logininfor:export")
    public DownloadedFile export(SysLoginInfoQuery query) {
        return ExcelUtil.exportExcel(service.queryList(query), "登录日志", SysLoginInfoVo.class);
    }

    @Delete
    @Mapping("/{ids}")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("monitor:logininfor:remove")
    public R<Void> remove(List<Long> ids) {
        return toAjax(service.delete(ids));
    }

    @Delete
    @Mapping("/clean")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("monitor:logininfor:remove")
    public R<Void> clean() {
        service.clean();
        return R.ok();
    }

    @Get
    @Mapping("/unlock/{username}")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @SaCheckPermission("monitor:logininfor:unlock")
    public R<Void> unlock(String username) {
        cacheService.remove(GlobalConstants.PWD_ERR_CNT_KEY + username);
        return R.ok();
    }
}
