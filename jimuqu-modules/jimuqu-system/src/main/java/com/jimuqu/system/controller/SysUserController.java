package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.v7.core.util.ObjUtil;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StreamUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.excel.utils.ExcelUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.mybatis.annotation.DataColumn;
import com.jimuqu.common.mybatis.annotation.DataPermission;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.query.SysPostQuery;
import com.jimuqu.system.domain.query.SysRoleQuery;
import com.jimuqu.system.domain.query.SysUserQuery;
import com.jimuqu.system.domain.vo.*;
import com.jimuqu.system.service.SysDeptService;
import com.jimuqu.system.service.SysPostService;
import com.jimuqu.system.service.SysRoleService;
import com.jimuqu.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 用户信息 Controller
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/user")
public class SysUserController extends BaseController {

    private final SysUserService sysUserService;
    private final SysRoleService roleService;
    private final SysPostService postService;
    private final SysDeptService deptService;

    /**
     * 查询用户信息列表
     * 应用数据权限：用户只能查看自己权限范围内的用户
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:user:list")
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    public Page<SysUserVo> list(SysUserQuery query, PageQuery pageQuery) {
        return sysUserService.queryPageList(query, pageQuery);
    }

    /**
     * 获取部门下的所有用户信息
     */
    @Get
    @Mapping("/list/dept/{deptId}")
    @SaCheckPermission("system:user:list")
    public R<List<SysUserVo>> listByDept(@NotNull Long deptId) {
        return R.ok(sysUserService.selectUserListByDept(deptId));
    }

    /**
     * 获取指定用户信息详细信息
     *
     * @param userId 用户信息主键
     */
    @Get
    @Mapping("/{userId}?") // {userId}?表示userId参数是可选的
    @SaCheckPermission("system:user:query")
    public SysUserInfoVo getInfoById(Long userId) {
        sysUserService.checkUserDataScope(userId);
        SysUserInfoVo userInfoVo = new SysUserInfoVo();
        SysRoleQuery roleBo = new SysRoleQuery();
        roleBo.setStatus(UserConstants.ROLE_NORMAL);
        List<SysRoleVo> roles = roleService.queryList(roleBo);
        userInfoVo.setRoles(LoginHelper.isSuperAdmin(userId) ? roles : StreamUtil.filter(roles, r -> !r.isSuperAdmin()));
        userInfoVo.setPosts(postService.queryList(new SysPostQuery().setStatus(UserConstants.POST_NORMAL)));
        if (ObjUtil.isNotNull(userId)) {
            SysUserVo sysUser = sysUserService.queryById(userId);
            List<Long> roleIds = StreamUtil.toList(sysUser.getRoles(), SysRoleVo::getId);
            sysUser.setRoleIds(roleIds);
            userInfoVo.setUser(sysUser);
            userInfoVo.setRoleIds(roleIds);
            userInfoVo.setPostIds(postService.selectPostListByUserId(userId));
        }
        return userInfoVo;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @Get
    @Mapping("/info")
    public R<UserInfoVo> getInfo() {
        UserInfoVo userInfoVo = new UserInfoVo();
        LoginUser loginUser = LoginHelper.getLoginUser();
        SysUserVo user = sysUserService.queryById(loginUser.getUserId());
        if (ObjUtil.isNull(user)) {
            return R.fail("没有权限访问用户数据!");
        }
        userInfoVo.setUser(user);
        userInfoVo.setPermissions(loginUser.getMenuPermission());
        userInfoVo.setRoles(loginUser.getRolePermission());
        return R.ok(userInfoVo);
    }

    /**
     * 根据用户编号获取授权角色
     *
     * @param userId 用户ID
     */
    @Get
    @Mapping("/authRole/{userId}")
    @SaCheckPermission("system:user:query")
    public R<SysUserInfoVo> authRole(Long userId) {
        SysUserVo user = sysUserService.queryById(userId);
        List<SysRoleVo> roles = roleService.selectRolesByUserId(userId);
        SysUserInfoVo userInfoVo = new SysUserInfoVo();
        userInfoVo.setUser(user);
        userInfoVo.setRoles(LoginHelper.isSuperAdmin(userId) ? roles : StreamUtil.filter(roles, r -> !r.isSuperAdmin()));
        return R.ok(userInfoVo);
    }

    /**
     * 新增用户信息
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:user:add")
    @Log(title = "新增用户信息", businessType = BusinessType.ADD)
    public R<Long> add(@Validated(AddGroup.class) SysUserBo user) {
        deptService.checkDeptDataScope(user.getDeptId());
        if (!sysUserService.checkUserNameUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StringUtil.isNotEmpty(user.getPhonenumber()) && !sysUserService.checkPhoneUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtil.isNotEmpty(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        boolean result = sysUserService.insertByBo(user);
        Assert.isTrue(result, "新增用户信息失败");
        return R.ok(user.getId());
    }

    /**
     * 更新用户信息
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:user:update")
    @Log(title = "更新用户信息", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Validated(UpdateGroup.class) SysUserBo user) {
        sysUserService.checkUserAllowed(user.getId());
        sysUserService.checkUserDataScope(user.getId());
        deptService.checkDeptDataScope(user.getDeptId());
        if (!sysUserService.checkUserNameUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StringUtil.isNotEmpty(user.getPhonenumber()) && !sysUserService.checkPhoneUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtil.isNotEmpty(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        boolean result = sysUserService.updateByBo(user);
        Assert.isTrue(result, "更新用户信息失败");
        return R.ok();
    }

    /**
     * 重置密码
     */
    @Mapping("/resetPwd")
    @SaCheckPermission("system:user:resetPwd")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public R<Void> resetPwd(SysUserBo user) {
        sysUserService.checkUserAllowed(user.getId());
        sysUserService.checkUserDataScope(user.getId());
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        return toAjax(sysUserService.resetUserPwd(user.getId(), user.getPassword()));
    }

    /**
     * 状态修改
     */
    @Mapping("/changeStatus")
    @SaCheckPermission("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public R<Void> changeStatus(SysUserBo user) {
        sysUserService.checkUserAllowed(user.getId());
        sysUserService.checkUserDataScope(user.getId());
        return toAjax(sysUserService.updateUserStatus(user.getId(), user.getStatus()));
    }

    /**
     * 用户授权角色
     *
     * @param userId  用户Id
     * @param roleIds 角色ID串
     */
    @Mapping("/authRole")
    @SaCheckPermission("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    public R<Void> insertAuthRole(Long userId, Long[] roleIds) {
        sysUserService.checkUserDataScope(userId);
        sysUserService.insertUserAuth(userId, roleIds);
        return R.ok();
    }

    /**
     * 删除用户信息
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:user:delete")
    @Log(title = "删除用户信息", businessType = BusinessType.DELETE)
    public Integer delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Assert.isFalse(ids.contains(LoginHelper.getUserId()), "当前用户不能删除");
        Integer num = sysUserService.deleteByIds(ids);
        Assert.gtZero(num, "删除用户信息失败");
        return num;
    }

    /**
     * 导出用户列表
     */
    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @SaCheckPermission("system:user:export")
    @Mapping("/export")
    public DownloadedFile export(SysUserQuery query) {
        List<SysUserVo> list = sysUserService.queryList(query);
        List<SysUserExportVo> listVo = MapstructUtil.convert(list, SysUserExportVo.class);
        return ExcelUtil.exportExcel(listVo, "用户数据", SysUserExportVo.class);
    }

}