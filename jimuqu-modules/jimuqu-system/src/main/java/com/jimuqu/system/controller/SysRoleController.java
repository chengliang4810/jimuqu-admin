package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.query.SysRoleQuery;
import com.jimuqu.system.domain.query.SysUserQuery;
import com.jimuqu.system.domain.vo.DeptTreeSelectVo;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.service.SysDeptService;
import com.jimuqu.system.service.SysRoleService;
import com.jimuqu.system.service.SysUserService;
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
import java.util.List;

/**
 * 角色信息 Controller。
 *
 * @author chengliang4810
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/role")
public class SysRoleController extends BaseController {

    private final SysUserService userService;
    private final SysDeptService deptService;
    private final SysRoleService roleService;

    @Get
    @Mapping("/list")
    @SaCheckPermission("system:role:list")
    public Page<SysRoleVo> list(SysRoleQuery query, PageQuery pageQuery) {
        return roleService.queryPageList(query, pageQuery);
    }

    @Post
    @Mapping("/export")
    @SaCheckPermission("system:role:export")
    @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    public DownloadedFile export(SysRoleQuery query) {
        return ExcelUtil.exportExcel(roleService.queryList(query), "角色数据", SysRoleVo.class);
    }

    @Get
    @Mapping("/{roleId}")
    @SaCheckPermission("system:role:query")
    public SysRoleVo getInfo(@NotNull(message = "角色ID不能为空") Long roleId) {
        roleService.checkRoleDataScope(roleId);
        return roleService.queryById(roleId);
    }

    @Get
    @Mapping("/optionselect")
    @SaCheckPermission("system:role:query")
    public R<List<SysRoleVo>> optionselect(Long[] roleIds) {
        return R.ok(roleService.selectRoleByIds(roleIds == null ? null : Arrays.asList(roleIds)));
    }

    @Get
    @Mapping("/authUser/allocatedList")
    @SaCheckPermission("system:role:list")
    public Page<SysUserVo> allocatedList(SysUserQuery user, PageQuery pageQuery) {
        return userService.selectAllocatedList(user, pageQuery);
    }

    @Get
    @Mapping("/authUser/unallocatedList")
    @SaCheckPermission("system:role:list")
    public Page<SysUserVo> unallocatedList(SysUserQuery user, PageQuery pageQuery) {
        return userService.selectUnallocatedList(user, pageQuery);
    }

    @NoRepeatSubmit
    @Post
    @Mapping
    @SaCheckPermission("system:role:add")
    @Log(title = "角色管理", businessType = BusinessType.ADD)
    public R<Void> add(@Body @Validated(AddGroup.class) SysRoleBo role) {
        roleService.checkRoleAllowed(role);
        if (!roleService.checkRoleNameUnique(role)) {
            return R.fail("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return R.fail("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        return toAjax(roleService.insertByBo(role));
    }

    @Put
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Body @Validated(UpdateGroup.class) SysRoleBo role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getId());
        if (!roleService.checkRoleNameUnique(role)) {
            return R.fail("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return R.fail("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        if (roleService.updateByBo(role)) {
            roleService.cleanOnlineUserByRole(role.getId());
            return R.ok();
        }
        return R.fail("修改角色'" + role.getRoleName() + "'失败，请联系管理员");
    }

    @Put
    @Mapping("/permission")
    @NoRepeatSubmit
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    public R<Void> editPermission(@Body SysRoleBo role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getId());
        return toAjax(roleService.updateRolePermission(role));
    }

    @Put
    @Mapping("/dataScope")
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    public R<Void> dataScope(@Body SysRoleBo role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getId());
        return toAjax(roleService.authDataScope(role));
    }

    @Put
    @Mapping("/changeStatus")
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    public R<Void> changeStatus(@Body SysRoleBo role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getId());
        return toAjax(roleService.updateRoleStatus(role.getId(), role.getStatus()));
    }

    @Put
    @Mapping("/authUser/cancel")
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    public R<Void> cancelAuthUser(@Body SysUserRole userRole) {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    @Put
    @Mapping("/authUser/cancelAll")
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    public R<Void> cancelAuthUserAll(Long roleId, Long[] userIds) {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    @Put
    @Mapping("/authUser/selectAll")
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    public R<Void> selectAuthUserAll(Long roleId, Long[] userIds) {
        roleService.checkRoleDataScope(roleId);
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }

    @Delete
    @Mapping("/{roleIds}")
    @SaCheckPermission("system:role:remove")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    public R<Void> delete(@NotEmpty(message = "角色ID不能为空") List<Long> roleIds) {
        Integer num = roleService.deleteByIds(roleIds);
        Assert.gtZero(num, "删除角色失败");
        return R.ok();
    }

    @Get
    @Mapping("/deptTree/{roleId}")
    @SaCheckPermission("system:role:list")
    public R<DeptTreeSelectVo> roleDeptTreeSelect(Long roleId) {
        DeptTreeSelectVo selectVo = new DeptTreeSelectVo();
        selectVo.setCheckedKeys(deptService.selectDeptListByRoleId(roleId));
        selectVo.setDepts(deptService.selectDeptTreeList(new SysDeptQuery()));
        return R.ok(selectVo);
    }
}
