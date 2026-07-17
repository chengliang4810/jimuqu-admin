package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysOssConfigBo;
import com.jimuqu.system.domain.query.SysOssConfigQuery;
import com.jimuqu.system.domain.vo.SysOssConfigVo;
import com.jimuqu.system.service.SysOssConfigService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Mapping("/resource/oss/config")
public class SysOssConfigController extends BaseController {

    private final SysOssConfigService service;

    @Get
    @Mapping("/list")
    @SaCheckPermission("system:ossConfig:list")
    public Page<SysOssConfigVo> list(SysOssConfigQuery query, PageQuery pageQuery) {
        return service.queryPage(query, pageQuery);
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:ossConfig:list")
    public SysOssConfigVo getInfo(Long id) {
        return service.queryById(id);
    }

    @Post
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:ossConfig:add")
    @Log(title = "对象存储配置", businessType = BusinessType.ADD)
    public R<Void> add(@Validated SysOssConfigBo bo) {
        return toAjax(service.insert(bo));
    }

    @Put
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:ossConfig:edit")
    @Log(title = "对象存储配置", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Validated SysOssConfigBo bo) {
        return toAjax(service.update(bo));
    }

    @Put
    @Mapping("/changeStatus")
    @NoRepeatSubmit
    @SaCheckPermission("system:ossConfig:edit")
    @Log(title = "对象存储状态修改", businessType = BusinessType.UPDATE)
    public R<Void> changeStatus(SysOssConfigBo bo) {
        return toAjax(service.changeStatus(bo));
    }

    @Delete
    @Mapping("/{ids}")
    @SaCheckPermission("system:ossConfig:remove")
    @Log(title = "对象存储配置", businessType = BusinessType.DELETE)
    public R<Void> remove(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        return toAjax(service.delete(ids));
    }
}
