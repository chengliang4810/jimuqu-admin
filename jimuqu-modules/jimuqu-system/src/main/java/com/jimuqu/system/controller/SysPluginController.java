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
import com.jimuqu.system.domain.bo.SysPluginBo;
import com.jimuqu.system.domain.query.SysPluginQuery;
import com.jimuqu.system.domain.vo.SysPluginVo;
import com.jimuqu.system.service.SysPluginService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 在线插件Controller。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/plugin")
public class SysPluginController extends BaseController {

    private final SysPluginService sysPluginService;

    /**
     * 查询插件列表。
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:plugin:list")
    public Page<SysPluginVo> list(SysPluginQuery query, PageQuery pageQuery) {
        return sysPluginService.queryPageList(query, pageQuery);
    }

    /**
     * 获取插件详细信息。
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:plugin:query")
    public SysPluginVo getInfo(@NotNull(message = "插件主键不能为空") Long id) {
        return sysPluginService.queryById(id);
    }

    /**
     * 新增插件。
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:plugin:add")
    @Log(title = "新增在线插件", businessType = BusinessType.ADD)
    public Long add(@Validated(AddGroup.class) SysPluginBo bo) {
        boolean result = sysPluginService.insertByBo(bo);
        Assert.isTrue(result, "新增在线插件失败");
        return bo.getId();
    }

    /**
     * 更新插件。
     */
    @Mapping("/update")
    @NoRepeatSubmit
    @SaCheckPermission("system:plugin:update")
    @Log(title = "更新在线插件", businessType = BusinessType.UPDATE)
    public void edit(@Validated(UpdateGroup.class) SysPluginBo bo) {
        boolean result = sysPluginService.updateByBo(bo);
        Assert.isTrue(result, "更新在线插件失败");
    }

    /**
     * 删除插件。
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:plugin:delete")
    @Log(title = "删除在线插件", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysPluginService.deleteByIds(ids);
        Assert.gtZero(num, "删除在线插件失败");
        return num;
    }

    /**
     * 修改插件状态。
     */
    @Mapping("/status/{id}/{status}")
    @SaCheckPermission("system:plugin:update")
    @Log(title = "修改在线插件状态", businessType = BusinessType.UPDATE)
    public void updateStatus(@NotNull(message = "插件主键不能为空") Long id,
                             @NotNull(message = "插件状态不能为空") Integer status) {
        boolean result = sysPluginService.updateStatus(id, status);
        Assert.isTrue(result, "修改在线插件状态失败");
    }

    /**
     * 扫描本地插件目录。
     */
    @Mapping("/scan")
    @SaCheckPermission("system:plugin:scan")
    @Log(title = "扫描在线插件", businessType = BusinessType.OTHER)
    public Integer scan() {
        return sysPluginService.scan();
    }

    /**
     * 上传插件包。
     */
    @Mapping("/upload")
    @NoRepeatSubmit
    @SaCheckPermission("system:plugin:upload")
    @Log(title = "上传在线插件", businessType = BusinessType.UPLOAD)
    public Long upload(UploadedFile file) {
        return sysPluginService.upload(file);
    }

    /**
     * 下载插件开发模板。
     */
    @Get
    @Mapping("/template")
    @SaCheckPermission("system:plugin:query")
    public DownloadedFile template() {
        return sysPluginService.downloadTemplate();
    }
}
