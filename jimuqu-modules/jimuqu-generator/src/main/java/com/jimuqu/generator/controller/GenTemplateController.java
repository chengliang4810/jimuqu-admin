package com.jimuqu.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.generator.domain.bo.GenTemplateBo;
import com.jimuqu.generator.domain.vo.GenTemplateVo;
import com.jimuqu.generator.service.IGenTemplateService;
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
 * 代码生成模板
 *
 * @author chengliang4810
 * @date 2025-01-05
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/tool/gen-template")
public class GenTemplateController extends BaseController {

    private final IGenTemplateService genTemplateService;

    /**
     * 查询代码生成模板列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("generator:template:list")
    public Page<GenTemplateVo> list(GenTemplateBo bo, PageQuery pageQuery) {
        return genTemplateService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取代码生成模板详细信息
     *
     * @param id 主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("generator:template:query")
    public GenTemplateVo getInfo(@NotNull(message = "主键不能为空") Long id) {
        return genTemplateService.queryById(id);
    }

    /**
     * 新增代码生成模板
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("generator:template:add")
    @Log(title = "代码生成模板", businessType = BusinessType.ADD)
    public R<Void> add(@Validated(AddGroup.class) GenTemplateBo bo) {
        return toAjax(genTemplateService.insertByBo(bo));
    }

    /**
     * 修改代码生成模板
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("generator:template:edit")
    @Log(title = "代码生成模板", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Validated(UpdateGroup.class) GenTemplateBo bo) {
        return toAjax(genTemplateService.updateByBo(bo));
    }

    /**
     * 删除代码生成模板
     *
     * @param ids 主键串
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("generator:template:remove")
    @Log(title = "代码生成模板", businessType = BusinessType.DELETE)
    public R<Integer> remove(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = genTemplateService.deleteWithValidByIds(ids);
        Assert.gtZero(num, "删除失败");
        return R.ok(num);
    }
}
