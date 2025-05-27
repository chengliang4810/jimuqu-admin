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
import com.jimuqu.system.domain.bo.SysDictTypeBo;
import com.jimuqu.system.domain.vo.SysDictTypeVo;
import com.jimuqu.system.domain.query.SysDictTypeQuery;
import com.jimuqu.system.service.SysDictTypeService;
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
 * 字典类型 Controller
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/dict/type")
public class SysDictTypeController extends BaseController {

    private final SysDictTypeService sysDictTypeService;

    /**
     * 查询字典类型列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:dict:list")
    public Page<SysDictTypeVo> list(SysDictTypeQuery query, PageQuery pageQuery) {
        return sysDictTypeService.queryPageList(query, pageQuery);
    }

    /**
     * 获取字典类型详细信息
     *
     * @param dictId 字典类型主键
     */
    @Get
    @Mapping("/{dictId}")
    @SaCheckPermission("system:dict:query")
    public SysDictTypeVo getInfo(@NotNull(message = "字典类型主键不能为空") Long dictId) {
        return sysDictTypeService.queryById(dictId);
    }

    /**
     * 新增字典类型
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:dict:add")
    @Log(title = "新增字典类型", businessType = BusinessType.ADD)
    public Long add(@Validated(AddGroup.class) SysDictTypeBo bo) {
        boolean result = sysDictTypeService.insertByBo(bo);
        Assert.isTrue(result, "新增字典类型失败");
        return bo.getDictId();
    }

    /**
     * 更新字典类型
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:dict:update")
    @Log(title = "更新字典类型", businessType = BusinessType.UPDATE)
    public void edit(@Validated(UpdateGroup.class) SysDictTypeBo bo) {
        boolean result = sysDictTypeService.updateByBo(bo);
        Assert.isTrue(result, "更新字典类型失败");
    }

    /**
     * 删除字典类型
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:dict:delete")
    @Log(title = "删除字典类型", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysDictTypeService.deleteByIds(ids);
        Assert.gtZero(num, "删除字典类型失败");
        return num;
    }

}