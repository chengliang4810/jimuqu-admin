package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.v7.core.convert.ConvertUtil;
import cn.hutool.v7.core.tree.MapTree;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysDeptBo;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.vo.SysDeptVo;
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
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;

/**
 * 部门 Controller。
 *
 * @author chengliang4810
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/dept")
public class SysDeptController extends BaseController {

    private static final Long DEFAULT_DEPT_ID = 100L;

    private final SysDeptService deptService;
    private final SysPostService postService;

    @Get
    @Mapping("/list")
    @SaCheckPermission("system:dept:list")
    public List<SysDeptVo> list(SysDeptQuery query) {
        return deptService.queryList(query);
    }

    @Get
    @Mapping("/tree")
    @SaCheckPermission("system:dept:list")
    public R<List<MapTree<Long>>> deptTree(SysDeptQuery query) {
        return R.ok(deptService.selectDeptTreeList(query));
    }

    @Get
    @Mapping("/list/exclude/{deptId}")
    @SaCheckPermission("system:dept:list")
    public R<List<SysDeptVo>> excludeChild(Long deptId) {
        List<SysDeptVo> depts = deptService.queryList(new SysDeptQuery());
        depts.removeIf(dept -> dept.getId().equals(deptId)
                || StringUtil.splitList(dept.getAncestors()).contains(ConvertUtil.toStr(deptId)));
        return R.ok(depts);
    }

    @Get
    @Mapping("/{deptId}")
    @SaCheckPermission("system:dept:query")
    public SysDeptVo getInfo(@NotNull(message = "部门ID不能为空") Long deptId) {
        deptService.checkDeptDataScope(deptId);
        return deptService.queryById(deptId);
    }

    @Get
    @Mapping("/optionselect")
    @SaCheckPermission("system:dept:query")
    public R<List<SysDeptVo>> optionselect(Long[] deptIds) {
        return R.ok(deptService.selectByIds(deptIds == null ? null : Arrays.asList(deptIds)));
    }

    @NoRepeatSubmit
    @Post
    @Mapping
    @SaCheckPermission("system:dept:add")
    @Log(title = "部门管理", businessType = BusinessType.ADD)
    public R<Void> add(@Body @Validated(AddGroup.class) SysDeptBo dept) {
        if (!deptService.checkDeptNameUnique(new SysDeptQuery()
                .setParentId(dept.getParentId()).setDeptName(dept.getDeptName()))) {
            return R.fail("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        return toAjax(deptService.insertByBo(dept));
    }

    @Put
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:dept:edit")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Body @Validated(UpdateGroup.class) SysDeptBo dept) {
        Long deptId = dept.getId();
        deptService.checkDeptDataScope(deptId);
        if (!deptService.checkDeptNameUnique(new SysDeptQuery().setParentId(dept.getParentId())
                .setDeptName(dept.getDeptName()).setId(deptId))) {
            return R.fail("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        if (dept.getParentId().equals(deptId)) {
            return R.fail("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        }
        if (UserConstants.DEPT_DISABLE.equals(dept.getStatus())) {
            if (deptService.selectNormalChildrenDeptById(deptId) > 0) {
                return R.fail("该部门包含未停用的子部门!");
            }
            if (deptService.checkDeptExistUser(deptId)) {
                return R.fail("该部门下存在已分配用户，不能禁用!");
            }
        }
        return toAjax(deptService.updateByBo(dept));
    }

    @Delete
    @Mapping("/{deptId}")
    @SaCheckPermission("system:dept:remove")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    public R<Void> delete(@NotNull(message = "部门ID不能为空") Long deptId) {
        if (DEFAULT_DEPT_ID.equals(deptId)) {
            return R.warn("默认部门,不允许删除");
        }
        if (deptService.hasChildByDeptId(deptId)) {
            return R.warn("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId)) {
            return R.warn("部门存在用户,不允许删除");
        }
        if (postService.countPostByDeptId(deptId) > 0) {
            return R.warn("部门存在岗位,不允许删除");
        }
        deptService.checkDeptDataScope(deptId);
        return toAjax(deptService.deleteByIds(Collections.singletonList(deptId)));
    }
}
