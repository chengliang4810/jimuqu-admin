package com.jimuqu.common.satoken.core;

import cn.dev33.satoken.stp.StpInterface;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.satoken.service.PermissionProvider;
import com.jimuqu.common.satoken.utils.LoginHelper;
import org.noear.solon.Solon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * SaToken用户权限
 */
public class SaPermissionImpl implements StpInterface {

    private final PermissionProvider permissionProvider;

    public SaPermissionImpl() {
        this.permissionProvider = null;
    }

    public SaPermissionImpl(PermissionProvider permissionProvider) {
        this.permissionProvider = Objects.requireNonNull(permissionProvider, "permissionProvider");
    }

    /**
     * 获取菜单权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return resolvePermissions(loginId, LoginUser::getMenuPermission,
                PermissionProvider::getMenuPermission);
    }

    /**
     * 获取角色权限列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return resolvePermissions(loginId, LoginUser::getRolePermission,
                PermissionProvider::getRolePermission);
    }

    private List<String> resolvePermissions(Object loginId,
                                            Function<LoginUser, Collection<String>> localExtractor,
                                            BiFunction<PermissionProvider, Long, Collection<String>> providerExtractor) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (!matches(loginUser, loginId)) {
            PermissionProvider provider = resolvePermissionProvider();
            return copy(providerExtractor.apply(provider, resolveUserId(loginId)));
        }
        return copy(localExtractor.apply(loginUser));
    }

    private PermissionProvider resolvePermissionProvider() {
        if (permissionProvider != null) {
            return permissionProvider;
        }
        try {
            PermissionProvider provider = Solon.context().getBean(PermissionProvider.class);
            if (provider != null) {
                return provider;
            }
        } catch (RuntimeException ignored) {
            // 独立使用 common-satoken 时可能没有 Solon 容器，统一按缺少实现处理。
        }
        throw new ServiceException("PermissionProvider 实现类不存在");
    }

    private List<String> copy(Collection<String> permissions) {
        return permissions == null || permissions.isEmpty()
                ? List.of()
                : new ArrayList<>(permissions);
    }

    private Long resolveUserId(Object loginId) {
        if (loginId == null) {
            throw new ServiceException("登录ID格式错误");
        }
        String value = loginId.toString();
        int separatorIndex = value.indexOf(':');
        if (separatorIndex < 0 || separatorIndex == value.length() - 1) {
            throw new ServiceException("登录ID格式错误");
        }
        try {
            return Long.valueOf(value.substring(separatorIndex + 1));
        } catch (NumberFormatException exception) {
            throw new ServiceException("登录ID格式错误");
        }
    }

    private boolean matches(LoginUser loginUser, Object loginId) {
        if (loginUser == null) {
            return false;
        }
        try {
            return Objects.equals(loginUser.getLoginId(), loginId);
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
