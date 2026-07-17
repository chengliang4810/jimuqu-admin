package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysNoticeBo;
import com.jimuqu.system.domain.query.SysNoticeQuery;
import com.jimuqu.system.domain.vo.SysNoticeVo;
import com.jimuqu.system.service.SysNoticeService;
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

/**
 * 通知公告管理。
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/notice")
public class SysNoticeController extends BaseController {

    private final SysNoticeService noticeService;

    @Get
    @Mapping("/list")
    @SaCheckPermission("system:notice:list")
    public Page<SysNoticeVo> list(SysNoticeQuery query, PageQuery pageQuery) {
        return noticeService.queryPage(query, pageQuery);
    }

    @Get
    @Mapping("/{noticeId}")
    @SaCheckPermission("system:notice:query")
    public SysNoticeVo getInfo(Long noticeId) {
        return noticeService.queryById(noticeId);
    }

    @Post
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:notice:add")
    @Log(title = "通知公告", businessType = BusinessType.ADD)
    public R<Void> add(@Validated SysNoticeBo bo) {
        return toAjax(noticeService.insert(bo));
    }

    @Put
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:notice:edit")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Validated SysNoticeBo bo) {
        return toAjax(noticeService.update(bo));
    }

    @Delete
    @Mapping("/{noticeIds}")
    @SaCheckPermission("system:notice:remove")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    public R<Void> remove(@NotEmpty(message = "主键不能为空") List<Long> noticeIds) {
        return toAjax(noticeService.delete(noticeIds));
    }
}
