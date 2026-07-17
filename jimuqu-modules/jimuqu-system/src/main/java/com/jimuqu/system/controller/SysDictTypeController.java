package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.excel.utils.ExcelUtil;
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
 * 字典类型 Controller
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/dict/type")
public class SysDictTypeController extends BaseController {

    private final SysDictTypeService sysDictTypeService;

    /**
     * 查询字典类型列表
     */
    @Get
    @Mapping("/all")
//    @SaCheckPermission("system:dict:list")
    public List<SysDictTypeVo> all(SysDictTypeQuery query) {
        return sysDictTypeService.queryList(query);
    }

    /**
     * 查询字典类型分页列表
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
    @Post
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:dict:add")
    @Log(title = "新增字典类型", businessType = BusinessType.ADD)
    public Long add(@Body @Validated(AddGroup.class) SysDictTypeBo bo) {
        normalizeBellFields(bo);
        boolean result = sysDictTypeService.insertByBo(bo);
        Assert.isTrue(result, "新增字典类型失败");
        return bo.getDictId();
    }

    /**
     * 更新字典类型
     */
    @NoRepeatSubmit
    @Put
    @Mapping
    @SaCheckPermission("system:dict:edit")
    @Log(title = "更新字典类型", businessType = BusinessType.UPDATE)
    public void edit(@Body @Validated(UpdateGroup.class) SysDictTypeBo bo) {
        normalizeBellFields(bo);
        boolean result = sysDictTypeService.updateByBo(bo);
        Assert.isTrue(result, "更新字典类型失败");
    }

    /**
     * 删除字典类型
     */
    @Delete
    @Mapping("/{ids}")
    @SaCheckPermission("system:dict:remove")
    @Log(title = "删除字典类型", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysDictTypeService.deleteByIds(ids);
        Assert.gtZero(num, "删除字典类型失败");
        return num;
    }

    @Get
    @Mapping("/optionselect")
    public List<SysDictTypeVo> optionselect() {
        return sysDictTypeService.queryList(new SysDictTypeQuery());
    }

    @Post
    @Mapping("/export")
    @SaCheckPermission("system:dict:export")
    public DownloadedFile export(SysDictTypeQuery query) {
        return ExcelUtil.exportExcel(sysDictTypeService.queryList(query), "字典类型", SysDictTypeVo.class);
    }

    @Delete
    @Mapping("/refreshCache")
    public R<Void> refreshCache() {
        return R.ok();
    }

    private void normalizeBellFields(SysDictTypeBo bo) {
        bo.setDictKey(bo.getDictType());
        bo.setDictType("L");
    }

}
