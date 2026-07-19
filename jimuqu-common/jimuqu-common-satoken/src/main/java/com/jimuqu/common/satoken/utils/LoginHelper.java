package com.jimuqu.common.satoken.utils;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.UserType;
import cn.hutool.v7.core.convert.ConvertUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.util.ObjUtil;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * 登录鉴权助手
 * <p>
 * user_type 为 用户类型 同一个用户表 可以有多种用户类型 例如 pc,app
 * deivce 为 设备类型 同一个用户类型 可以有 多种设备类型 例如 web,ios
 * 可以组成 用户类型与设备类型多对多的 权限灵活控制
 * <p>
 * 多用户体系 针对 多种用户类型 但权限控制不一致
 * 可以组成 多用户类型表与多设备类型 分别控制权限
 *
 * @author Lion Li,chengliang4810
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginHelper {

    public static final String LOGIN_USER_KEY = "loginUser";
    public static final String USER_KEY = "userId";
    public static final String USER_NAME_KEY = "userName";
    public static final String DEPT_KEY = "deptId";
    public static final String DEPT_NAME_KEY = "deptName";
    public static final String DEPT_CATEGORY_KEY = "deptCategory";
    public static final String CLIENT_KEY = "clientid";
    public static final String CLIENT_ACCESS_PATH_KEY = "clientAccessPath";
    public static final String CLIENT_IP_WHITELIST_KEY = "clientIpWhitelist";

    /**
     * 登录系统 基于 设备类型
     * 针对相同用户体系不同设备
     *
     * @param loginUser 登录用户信息
     * @param model     配置参数
     */
    public static void login(LoginUser loginUser, SaLoginParameter model) {
        SaStorage storage = SaHolder.getStorage();
        storage.set(LOGIN_USER_KEY, loginUser);
        storage.set(USER_KEY, loginUser.getUserId());
        storage.set(DEPT_KEY, loginUser.getDeptId());
        model = ObjUtil.defaultIfNull(model, new SaLoginParameter());
        model.setExtraData(model.getExtraData() == null
                ? new HashMap<>()
                : new HashMap<>(model.getExtraData()));
        model.setExtra(USER_KEY, loginUser.getUserId())
                .setExtra(USER_NAME_KEY, loginUser.getUsername())
                .setExtra(DEPT_KEY, loginUser.getDeptId())
                .setExtra(DEPT_NAME_KEY, loginUser.getDeptName())
                .setExtra(DEPT_CATEGORY_KEY, loginUser.getDeptCategory());
        StpUtil.login(loginUser.getLoginId(), model);
        SaSession tokenSession = StpUtil.getTokenSession();
        tokenSession.updateTimeout(model.getTimeout());
        tokenSession.set(LOGIN_USER_KEY, loginUser);
        model.getExtraData().forEach((key, value) -> {
            if (ObjUtil.isNotNull(value)) {
                tokenSession.set(key, value);
            }
        });
    }

    /**
     * 获取用户(多级缓存)
     */
    public static LoginUser getLoginUser() {
        return (LoginUser) getStorageIfAbsentSet(LOGIN_USER_KEY, () -> {
            SaSession session = StpUtil.getTokenSession();
            if (ObjUtil.isNull(session)) {
                return null;
            }
            Object o = session.get(LOGIN_USER_KEY);
            if (ObjUtil.isNull(o)) {
                return null;
            }
            return o;
        });
    }

    /**
     * 获取用户基于token
     */
    public static LoginUser getLoginUser(String token) {
        SaSession session = StpUtil.getTokenSessionByToken(token);
        if (ObjUtil.isNull(session)) {
            return null;
        }
        return (LoginUser) session.get(LOGIN_USER_KEY);
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
        return ConvertUtil.toLong(getExtra(USER_KEY));
    }

    /**
     * 获取用户ID字符串
     */
    public static String getUserIdStr() {
        return ConvertUtil.toStr(getExtra(USER_KEY));
    }

    /**
     * 获取部门ID
     */
    public static Long getDeptId() {
        return ConvertUtil.toLong(getExtra(DEPT_KEY));
    }

    /**
     * 获取部门名称
     */
    public static String getDeptName() {
        return ConvertUtil.toStr(getExtra(DEPT_NAME_KEY));
    }

    /**
     * 获取部门类别编码
     */
    public static String getDeptCategory() {
        return ConvertUtil.toStr(getExtra(DEPT_CATEGORY_KEY));
    }

    /**
     * 获取角色ID
     */
    public static Long getRoleId() {
        LoginUser loginUser = getLoginUser();
        if (ObjUtil.isNull(loginUser)) {
            return null;
        }
        return loginUser.getRoleId();
    }

    /**
     * 获取用户账户
     */
    public static String getUsername() {
        return ConvertUtil.toStr(getExtra(USER_NAME_KEY));
    }

    private static Object getExtra(String key) {
        try {
            Object extra = StpUtil.getExtra(key);
            if (ObjUtil.isNotNull(extra)) {
                return extra;
            }
        } catch (Exception ignored) {
            // 普通 UUID Token 不支持直接读取 extra，继续从 TokenSession 获取。
        }
        try {
            SaSession session = StpUtil.getTokenSession();
            return ObjUtil.isNull(session) ? null : session.get(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取用户类型
     */
    public static UserType getUserType() {
        String loginType = StpUtil.getLoginIdAsString();
        return UserType.getUserType(loginType);
    }

    /**
     * 是否为超级管理员
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isSuperAdmin(Long userId) {
        return UserConstants.SUPER_ADMIN_ID.equals(userId);
    }

    public static boolean isSuperAdmin() {
        return isSuperAdmin(getUserId());
    }

    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    public static <T> T getStorageIfAbsentSet(String key, Supplier<T> handle) {
        try {
            Object obj = SaHolder.getStorage().get(key);
            if (ObjUtil.isNull(obj)) {
                T tObj = handle.get();
                SaHolder.getStorage().set(key, tObj);
                return tObj;
            }
            return (T)obj;
        } catch (Exception e) {
            return null;
        }
    }
}
