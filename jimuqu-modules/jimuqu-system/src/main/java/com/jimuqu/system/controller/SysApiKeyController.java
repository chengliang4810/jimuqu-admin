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
import com.jimuqu.system.domain.bo.SysApiKeyBo;
import com.jimuqu.system.domain.query.SysApiKeyQuery;
import com.jimuqu.system.domain.vo.SysApiKeyVo;
import com.jimuqu.system.service.ISysApiKeyService;
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
 * API密钥管理Controller
 * @author jimuqu-admin
 * @since 2025-08-18
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/api-key")
public class SysApiKeyController extends BaseController {

    private final ISysApiKeyService sysApiKeyService;

    /**
     * 查询API密钥列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:api-key:list")
    public Page<SysApiKeyVo> list(SysApiKeyQuery query, PageQuery pageQuery) {
        return sysApiKeyService.queryPageList(query, pageQuery);
    }

    /**
     * 获取API密钥详细信息
     *
     * @param id API密钥主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:api-key:query")
    public SysApiKeyVo getInfo(@NotNull(message = "API密钥主键不能为空") Long id) {
        return sysApiKeyService.queryById(id);
    }

    /**
     * 新增API密钥
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:api-key:add")
    @Log(title = "新增API密钥", businessType = BusinessType.ADD)
    public Long add(@Validated(AddGroup.class) SysApiKeyBo bo) {
        boolean result = sysApiKeyService.insertByBo(bo);
        Assert.isTrue(result, "新增API密钥失败");
        return bo.getId();
    }

    /**
     * 更新API密钥
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:api-key:update")
    @Log(title = "更新API密钥", businessType = BusinessType.UPDATE)
    public void edit(@Validated(UpdateGroup.class) SysApiKeyBo bo) {
        boolean result = sysApiKeyService.updateByBo(bo);
        Assert.isTrue(result, "更新API密钥失败");
    }

    /**
     * 删除API密钥
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:api-key:delete")
    @Log(title = "删除API密钥", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Integer num = sysApiKeyService.deleteByIds(ids);
        Assert.gtZero(num, "删除API密钥失败");
        return num;
    }

    /**
     * 修改API密钥状态
     */
    @Mapping("/change-status")
    @SaCheckPermission("system:api-key:update")
    @Log(title = "修改API密钥状态", businessType = BusinessType.UPDATE)
    public void changeStatus(@NotNull(message = "主键不能为空") Long id,
                             @NotNull(message = "状态不能为空") Boolean isValid) {
        boolean result = sysApiKeyService.updateStatus(id, isValid);
        Assert.isTrue(result, "修改API密钥状态失败");
    }

    /**
     * 生成新的API Key
     */
    @Get
    @Mapping("/generate")
    @SaCheckPermission("system:api-key:add")
    public String generate() {
        return sysApiKeyService.generateApiKey();
    }

    /**
     * 根据用户ID查询API密钥列表
     */
    @Get
    @Mapping("/user/{userId}")
    @SaCheckPermission("system:api-key:list")
    public List<SysApiKeyVo> getApiKeyByUserId(@NotNull(message = "用户ID不能为空") Long userId) {
        return sysApiKeyService.queryByUserId(userId);
    }
}