package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.v7.core.tree.MapTree;
import cn.hutool.v7.core.util.ObjUtil;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysPostBo;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.query.SysPostQuery;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.service.SysDeptService;
import com.jimuqu.system.service.SysPostService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 岗位信息 Controller。
 *
 * @author chengliang4810
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/post")
public class SysPostController extends BaseController {

    private final SysPostService postService;
    private final SysDeptService deptService;

    @Get
    @Mapping("/list")
    @SaCheckPermission("system:post:list")
    public Page<SysPostVo> list(SysPostQuery query, PageQuery pageQuery) {
        return postService.queryPageList(query, pageQuery);
    }

    @Post
    @Mapping("/export")
    @SaCheckPermission("system:post:export")
    @Log(title = "岗位管理", businessType = BusinessType.EXPORT)
    public DownloadedFile export(SysPostQuery query) {
        return ExcelUtil.exportExcel(postService.queryList(query), "岗位数据", SysPostVo.class);
    }

    @Get
    @Mapping("/{postId}")
    @SaCheckPermission("system:post:query")
    public SysPostVo getInfo(@NotNull(message = "岗位ID不能为空") Long postId) {
        return postService.queryById(postId);
    }

    @NoRepeatSubmit
    @Post
    @Mapping
    @SaCheckPermission("system:post:add")
    @Log(title = "岗位管理", businessType = BusinessType.ADD)
    public R<Void> add(@Body @Validated(AddGroup.class) SysPostBo post) {
        if (!postService.checkPostNameUnique(post)) {
            return R.fail("新增岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        if (!postService.checkPostCodeUnique(post)) {
            return R.fail("新增岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        return toAjax(postService.insertByBo(post));
    }

    @Put
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:post:edit")
    @Log(title = "岗位管理", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Body @Validated(UpdateGroup.class) SysPostBo post) {
        if (!postService.checkPostNameUnique(post)) {
            return R.fail("修改岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        if (!postService.checkPostCodeUnique(post)) {
            return R.fail("修改岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        if ("1".equals(post.getStatus()) && postService.countUserPostById(post.getPostId()) > 0) {
            return R.fail("该岗位下存在已分配用户，不能禁用!");
        }
        return toAjax(postService.updateByBo(post));
    }

    @Delete
    @Mapping("/{postIds}")
    @SaCheckPermission("system:post:remove")
    @Log(title = "岗位管理", businessType = BusinessType.DELETE)
    public R<Void> delete(@NotEmpty(message = "岗位ID不能为空") List<Long> postIds) {
        return toAjax(postService.deleteByIds(postIds));
    }

    @Get
    @Mapping("/optionselect")
    @SaCheckPermission("system:post:query")
    public R<List<SysPostVo>> optionselect(Long[] postIds, Long deptId) {
        if (ObjUtil.isNotNull(deptId)) {
            return R.ok(postService.queryList(new SysPostQuery().setDeptId(deptId)));
        }
        if (postIds != null) {
            return R.ok(postService.selectPostByIds(Arrays.asList(postIds)));
        }
        return R.ok(Collections.emptyList());
    }

    @Get
    @Mapping("/deptTree")
    @SaCheckPermission("system:post:list")
    public R<List<MapTree<Long>>> deptTree(SysDeptQuery query) {
        return R.ok(deptService.selectDeptTreeList(query));
    }
}
