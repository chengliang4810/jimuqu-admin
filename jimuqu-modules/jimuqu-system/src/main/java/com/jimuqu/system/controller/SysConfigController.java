package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;
import com.jimuqu.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 参数配置 Controller
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/config")
public class SysConfigController extends BaseController {

    private final SysConfigService sysConfigService;

    /**
     * 查询参数配置列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:config:list")
    public Page<SysConfigVo> list(SysConfigQuery query, PageQuery pageQuery) {
        return sysConfigService.queryPageList(query, pageQuery);
    }

    /**
     * 获取参数配置详细信息
     *
     * @param id 参数配置主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:config:query")
    public SysConfigVo getInfo(@NotNull(message = "参数配置主键不能为空") Long id) {
        return sysConfigService.queryById(id);
    }

    /**
     * 新增参数配置
     */
    @Post
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:config:add")
    @Log(title = "参数管理", businessType = BusinessType.ADD)
    public R<Void> add(@Body @Validated(AddGroup.class) SysConfigBo bo) {
        if (!sysConfigService.checkConfigKeyUnique(bo)) {
            throw new ServiceException("新增参数'" + bo.getConfigName() + "'失败，参数键名已存在");
        }
        boolean result = sysConfigService.insertByBo(bo);
        Assert.isTrue(result, "新增参数配置失败");
        return R.ok();
    }

    /**
     * 更新参数配置
     */
    @NoRepeatSubmit
    @Put
    @Mapping
    @SaCheckPermission("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    public void edit(@Body @Validated(UpdateGroup.class) SysConfigBo bo) {
        if (!sysConfigService.checkConfigKeyUnique(bo)) {
            throw new ServiceException("修改参数'" + bo.getConfigName() + "'失败，参数键名已存在");
        }
        boolean result = sysConfigService.updateByBo(bo);
        Assert.isTrue(result, "更新参数配置失败");
    }

    /**
     * 删除参数配置
     */
    @Delete
    @Mapping("/{ids}")
    @SaCheckPermission("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.DELETE)
    public R<Void> delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysConfigService.deleteByIds(ids);
        Assert.gtZero(num, "删除参数配置失败");
        return R.ok();
    }

    @NoRepeatSubmit
    @Put
    @Mapping("/updateByKey")
    @SaCheckPermission("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    public void updateByKey(@Body SysConfigBo bo) {
        Assert.isTrue(sysConfigService.updateByKey(bo), "参数不存在");
    }

    @Get
    @Mapping("/configKey/{configKey}")
    public R<String> getByKey(String configKey) {
        return R.data(sysConfigService.selectConfigByKey(configKey));
    }

    @Post
    @Mapping("/export")
    @SaCheckPermission("system:config:export")
    @Log(title = "参数管理", businessType = BusinessType.EXPORT)
    public DownloadedFile export(SysConfigQuery query) {
        return ExcelUtil.exportExcel(sysConfigService.queryList(query), "参数数据", SysConfigVo.class);
    }

    @Delete
    @Mapping("/refreshCache")
    @SaCheckPermission("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.CLEAN)
    public R<Void> refreshCache() {
        sysConfigService.resetConfigCache();
        return R.ok();
    }

}
