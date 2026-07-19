package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysDictDataBo;
import com.jimuqu.system.domain.vo.SysDictDataVo;
import com.jimuqu.system.domain.query.SysDictDataQuery;
import com.jimuqu.system.service.SysDictDataService;
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
 * 字典数据 Controller
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/dict/data")
public class SysDictDataController extends BaseController {

    private final SysDictDataService sysDictDataService;

    /**
     * 查询字典数据列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:dict:list")
    public Page<SysDictDataVo> list(SysDictDataQuery query, PageQuery pageQuery) {
        return sysDictDataService.queryPageList(query, pageQuery);
    }

    /**
     * 获取字典数据详细信息
     *
     * @param id 字典数据主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:dict:query")
    public SysDictDataVo getInfo(@NotNull(message = "字典数据主键不能为空") Long id) {
        return sysDictDataService.queryById(id);
    }

    /**
     * 获取字典数据详细信息
     *
     * @param dictTypeKey 字典类型标识
     */
    @Get
    @Mapping("/type/{dictTypeKey}")
    public List<SysDictDataVo> getListByDictTypeKey(@NotNull(message = "字典数据主键不能为空") String dictTypeKey) {
        return sysDictDataService.queryListByTypeKey(dictTypeKey);
    }

    /**
     * 新增字典数据
     */
    @Post
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:dict:add")
    @Log(title = "字典数据", businessType = BusinessType.ADD)
    public R<Void> add(@Body @Validated(AddGroup.class) SysDictDataBo bo) {
        normalizeBellFields(bo);
        if (!sysDictDataService.checkDictDataUnique(bo)) {
            throw new ServiceException("新增字典数据'" + bo.getDictValue() + "'失败，字典键值已存在");
        }
        boolean result = sysDictDataService.insertByBo(bo);
        Assert.isTrue(result, "新增字典数据失败");
        return R.ok();
    }

    /**
     * 更新字典数据
     */
    @NoRepeatSubmit
    @Put
    @Mapping
    @SaCheckPermission("system:dict:edit")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    public void edit(@Body @Validated(UpdateGroup.class) SysDictDataBo bo) {
        normalizeBellFields(bo);
        if (!sysDictDataService.checkDictDataUnique(bo)) {
            throw new ServiceException("修改字典数据'" + bo.getDictValue() + "'失败，字典键值已存在");
        }
        boolean result = sysDictDataService.updateByBo(bo);
        Assert.isTrue(result, "更新字典数据失败");
    }

    /**
     * 删除字典数据
     */
    @Delete
    @Mapping("/{ids}")
    @SaCheckPermission("system:dict:remove")
    @Log(title = "字典数据", businessType = BusinessType.DELETE)
    public R<Void> delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysDictDataService.deleteByIds(ids);
        Assert.gtZero(num, "删除字典数据失败");
        return R.ok();
    }

    @Post
    @Mapping("/export")
    @SaCheckPermission("system:dict:export")
    @Log(title = "字典数据", businessType = BusinessType.EXPORT)
    public DownloadedFile export(SysDictDataQuery query) {
        return ExcelUtil.exportExcel(sysDictDataService.queryList(query), "字典数据", SysDictDataVo.class);
    }

    private void normalizeBellFields(SysDictDataBo bo) {
        if (StringUtil.isBlank(bo.getDictTypeKey())) {
            bo.setDictTypeKey(bo.getDictType());
        }
    }

}
