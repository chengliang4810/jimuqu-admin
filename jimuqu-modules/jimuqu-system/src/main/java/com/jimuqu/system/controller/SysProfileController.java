package com.jimuqu.system.controller;

import cn.dev33.satoken.secure.BCrypt;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.encrypt.annotation.ApiEncrypt;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.bo.SysUserPasswordBo;
import com.jimuqu.system.domain.bo.SysUserProfileBo;
import com.jimuqu.system.domain.vo.ProfileVo;
import com.jimuqu.system.domain.vo.ProfileUserVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.service.SysUserService;
import cn.hutool.core.bean.BeanUtil;
import org.noear.solon.annotation.*;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.ValidUtils;

/**
 * 个人信息 业务处理
 *
 * @author Lion Li,chengliang4810
 */
@Controller
@Mapping("/system/user/profile" )
public class SysProfileController extends BaseController {

    @Inject
    private SysUserService userService;

    /**
     * 个人信息
     */
    @Get
    @Mapping
    public R<ProfileVo> profile() {
        SysUserVo user = userService.queryById(LoginHelper.getUserId());
        ProfileVo profileVo = new ProfileVo();
        profileVo.setUser(BeanUtil.toBean(user, ProfileUserVo.class));
        profileVo.setRoleGroup(userService.selectUserRoleGroup(user.getId()));
        profileVo.setPostGroup(userService.selectUserPostGroup(user.getId()));
        return R.ok(profileVo);
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @NoRepeatSubmit
    @Put
    @Mapping
    public R<Void> updateProfile(@Body SysUserProfileBo profile) {
        ValidUtils.validateEntity(profile);
        SysUserBo user = BeanUtil.toBean(profile, SysUserBo.class);
        user.setId(LoginHelper.getUserId());
        String username = LoginHelper.getUsername();
        if (StringUtil.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return R.fail("修改用户'" + username + "'失败，手机号码已存在" );
        }
        if (StringUtil.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return R.fail("修改用户'" + username + "'失败，邮箱账号已存在" );
        }
        if (userService.updateUserProfile(user) > 0) {
            return R.ok();
        }
        return R.fail("修改个人信息异常，请联系管理员" );
    }

    /**
     * 重置密码
     *
     * @param bo 新旧密码
     */
    @ApiEncrypt
    @NoRepeatSubmit
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @Put
    @Mapping("/updatePwd" )
    public R<Void> updatePwd(@Body SysUserPasswordBo bo) {
        ValidUtils.validateEntity(bo);
        SysUserVo user = userService.queryById(LoginHelper.getUserId());
        String password = user.getPassword();
        if (!BCrypt.checkpw(bo.getOldPassword(), password)) {
            return R.fail("修改密码失败，旧密码错误" );
        }
        if (BCrypt.checkpw(bo.getNewPassword(), password)) {
            return R.fail("新密码不能与旧密码相同" );
        }

        if (userService.resetOwnUserPwd(user.getId(), BCrypt.hashpw(bo.getNewPassword()))) {
            return R.ok();
        }
        return R.fail("修改密码异常，请联系管理员" );
    }

}
