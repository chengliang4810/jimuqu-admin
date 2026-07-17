package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.core.tree.MapTree;
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
import com.jimuqu.common.excel.core.ExcelResult;
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
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.query.SysRoleQuery;
import com.jimuqu.system.domain.query.SysUserQuery;
import com.jimuqu.system.domain.vo.*;
import com.jimuqu.system.service.SysDeptService;
import com.jimuqu.system.service.SysPostService;
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
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;
import java.util.ArrayList;

/**
 * 用户信息 Controller
 *
 * @author chengliang4810
 * @since 2025-06-05
 */
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
    @Mapping("/{userId}")
    @SaCheckPermission("system:user:query")
    public R<SysUserInfoVo> getInfoById(Long userId) {
        return R.ok(buildUserInfo(userId));
    }

    /**
     * 获取新增用户所需的角色和岗位选项。
     */
    @Get
    @Mapping("/")
    @SaCheckPermission("system:user:query")
    public R<SysUserInfoVo> getCreateInfo() {
        return R.ok(buildUserInfo(null));
    }

    private SysUserInfoVo buildUserInfo(Long userId) {
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
    @Mapping("/getInfo")
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
    @NoRepeatSubmit
    @Post
    @Mapping
    @SaCheckPermission("system:user:add")
    @Log(title = "新增用户信息", businessType = BusinessType.ADD)
    public R<Void> add(@Body @Validated(AddGroup.class) SysUserBo user) {
        deptService.checkDeptDataScope(user.getDeptId());
        if (!sysUserService.checkUserNameUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StringUtil.isNotEmpty(user.getPhonenumber()) && !sysUserService.checkPhoneUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtil.isNotEmpty(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        return toAjax(sysUserService.insertByBo(user));
    }

    /**
     * 更新用户信息
     */
    @NoRepeatSubmit
    @Put
    @Mapping
    @SaCheckPermission("system:user:edit")
    @Log(title = "更新用户信息", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Body @Validated(UpdateGroup.class) SysUserBo user) {
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
        return toAjax(sysUserService.updateByBo(user));
    }

    /**
     * 重置密码
     */
    @Put
    @Mapping("/resetPwd")
    @SaCheckPermission("system:user:resetPwd")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public R<Void> resetPwd(@Body SysUserBo user) {
        sysUserService.checkUserAllowed(user.getId());
        sysUserService.checkUserDataScope(user.getId());
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        return toAjax(sysUserService.resetUserPwd(user.getId(), user.getPassword()));
    }

    /**
     * 状态修改
     */
    @Put
    @Mapping("/changeStatus")
    @SaCheckPermission("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public R<Void> changeStatus(@Body SysUserBo user) {
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
    @Put
    @Mapping("/authRole")
    @SaCheckPermission("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    public R<Void> insertAuthRole(@Body SysUserBo user) {
        sysUserService.checkUserDataScope(user.getId());
        Long[] roleIds = user.getRoleIds() == null ? new Long[0] : user.getRoleIds().toArray(Long[]::new);
        sysUserService.insertUserAuth(user.getId(), roleIds);
        return R.ok();
    }

    /**
     * 删除用户信息
     */
    @Delete
    @Mapping("/{ids}")
    @SaCheckPermission("system:user:remove")
    @Log(title = "删除用户信息", businessType = BusinessType.DELETE)
    public R<Void> delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        Assert.isFalse(ids.contains(LoginHelper.getUserId()), "当前用户不能删除");
        Integer num = sysUserService.deleteByIds(ids);
        Assert.gtZero(num, "删除用户信息失败");
        return R.ok();
    }

    /**
     * 获取用户筛选使用的部门树。
     */
    @Get
    @Mapping("/deptTree")
    @SaCheckPermission("system:user:list")
    public R<List<MapTree<Long>>> deptTree(SysDeptQuery query) {
        return R.ok(deptService.selectDeptTreeList(query));
    }

    /**
     * 导出用户列表
     */
    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @Post
    @SaCheckPermission("system:user:export")
    @Mapping("/export")
    public DownloadedFile export(SysUserQuery query) {
        List<SysUserVo> list = sysUserService.queryList(query);
        List<SysUserExportVo> listVo = MapstructUtil.convert(list, SysUserExportVo.class);
        return ExcelUtil.exportExcel(listVo, "用户数据", SysUserExportVo.class);
    }

    /**
     * 导入用户数据。
     */
    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @Post
    @Mapping("/importData")
    @SaCheckPermission("system:user:import")
    public R<Void> importData(UploadedFile file, boolean updateSupport) {
        ExcelResult<SysUserImportVo> result = ExcelUtil.importExcel(
                file.getContent(), SysUserImportVo.class, true);
        return R.ok(sysUserService.importUsers(result.getList(), updateSupport));
    }

    /**
     * 下载用户导入模板。
     */
    @Post
    @Mapping("/importTemplate")
    @SaCheckPermission("system:user:import")
    public DownloadedFile importTemplate() {
        return ExcelUtil.exportExcel(new ArrayList<>(), "用户数据", SysUserImportVo.class);
    }

}
