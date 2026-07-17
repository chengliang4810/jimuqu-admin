package com.jimuqu.auth.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.LoginType;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.core.utils.DateUtil;
import com.jimuqu.common.log.event.LogininforEvent;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.service.ISysPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.data.cache.CacheService;

import java.util.List;
import java.util.function.Supplier;

/**
 * 登录校验方法
 *
 * @author Lion Li,chengliang4810
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SysLoginService {

    @Inject(value = "${user.password.maxRetryCount: 5}", required = false)
    private Integer maxRetryCount;
    @Inject(value = "${user.password.lockTime: 10}", required = false)
    private Integer lockTime;
    private final ISysPermissionService permissionService;
    private final CacheService cacheService;
    private final SysUserMapper userMapper;


    /**
     * 退出登录
     */
    public void logout() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (ObjUtil.isNull(loginUser)) {
                return;
            }
            recordLogininfor(loginUser.getUsername(), Constants.LOGOUT, "退出成功" );
        } catch (NotLoginException ignored) {
        } finally {
            try {
                StpUtil.logout();
            } catch (NotLoginException ignored) {
            }
        }
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     */
    public void recordLogininfor(String username, String status, String message) {
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        EventBus.publish(logininforEvent);
    }


    /**
     * 构建登录用户
     */
    public LoginUser buildLoginUser(SysUserVo user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        loginUser.setUserType(user.getUserType());
        loginUser.setMenuPermission(permissionService.getMenuPermission(user.getId()));
        loginUser.setRolePermission(permissionService.getRolePermission(user.getId()));
        loginUser.setDeptName(ObjUtil.isNull(user.getDept()) ? "" : user.getDept().getDeptName());
        List<RoleDTO> roles = BeanUtil.copyToList(user.getRoles(), RoleDTO.class);
        loginUser.setRoles(roles);
        return loginUser;
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId, String ip) {
        SysUser sysUser = new SysUser();
        sysUser.setId(userId);
        sysUser.setLoginIp(ip);
        sysUser.setLoginDate(DateUtil.getNowDate());
        sysUser.setUpdateBy(userId);
        userMapper.update(sysUser);
    }

    /**
     * 登录校验
     */
    public void checkLogin(LoginType loginType, String username, Supplier<Boolean> supplier) {
        String errorKey = GlobalConstants.PWD_ERR_CNT_KEY + username;
        String loginFail = Constants.LOGIN_FAIL;

        // 获取用户登录错误次数，默认为0 (可自定义限制策略 例如: key + username + ip)
        int errorNumber = ObjUtil.defaultIfNull(cacheService.get(errorKey, Integer.class), 0);
        // 锁定时间内登录 则踢出
        if (errorNumber >= maxRetryCount) {
            recordLogininfor(username, loginFail, StrUtil.format(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }

        if (supplier.get()) {
            // 错误次数递增
            errorNumber++;
            cacheService.store(errorKey, errorNumber, lockTime * 60);
            // 达到规定错误次数 则锁定登录
            if (errorNumber >= maxRetryCount) {
                recordLogininfor(username, loginFail, StrUtil.format(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
            } else {
                // 未达到规定错误次数
                recordLogininfor(username, loginFail, StrUtil.format(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }

        // 登录成功 清空错误次数
        cacheService.remove(errorKey);
    }

}
