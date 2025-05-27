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
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;
import com.jimuqu.system.service.SysConfigService;
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
 * 参数配置 Controller
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Post
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
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:config:add")
    @Log(title = "新增参数配置", businessType = BusinessType.ADD)
    public Long add(@Validated(AddGroup.class) SysConfigBo bo) {
        boolean result = sysConfigService.insertByBo(bo);
        Assert.isTrue(result, "新增参数配置失败");
        return bo.getId();
    }

    /**
     * 更新参数配置
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:config:update")
    @Log(title = "更新参数配置", businessType = BusinessType.UPDATE)
    public void edit(@Validated(UpdateGroup.class) SysConfigBo bo) {
        boolean result = sysConfigService.updateByBo(bo);
        Assert.isTrue(result, "更新参数配置失败");
    }

    /**
     * 删除参数配置
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:config:delete")
    @Log(title = "删除参数配置", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysConfigService.deleteByIds(ids);
        Assert.gtZero(num, "删除参数配置失败");
        return num;
    }

}