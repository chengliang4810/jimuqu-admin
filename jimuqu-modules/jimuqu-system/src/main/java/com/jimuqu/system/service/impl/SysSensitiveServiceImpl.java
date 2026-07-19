package com.jimuqu.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.common.core.service.SensitiveService;
import com.jimuqu.common.satoken.utils.LoginHelper;
import org.noear.solon.annotation.Component;

/**
 * 系统用户响应字段脱敏权限实现。
 */
@Component
public class SysSensitiveServiceImpl implements SensitiveService {

    @Override
    public boolean isSensitive(String[] roleKey, String[] perms) {
        if (!LoginHelper.isLogin()) {
            return true;
        }
        boolean hasRoleRule = roleKey != null && roleKey.length > 0;
        boolean hasPermissionRule = perms != null && perms.length > 0;
        if (hasRoleRule && hasPermissionRule) {
            if (StpUtil.hasRoleOr(roleKey) && StpUtil.hasPermissionOr(perms)) {
                return false;
            }
        } else if (hasRoleRule && StpUtil.hasRoleOr(roleKey)) {
            return false;
        } else if (hasPermissionRule && StpUtil.hasPermissionOr(perms)) {
            return false;
        }
        return !LoginHelper.isSuperAdmin();
    }
}
