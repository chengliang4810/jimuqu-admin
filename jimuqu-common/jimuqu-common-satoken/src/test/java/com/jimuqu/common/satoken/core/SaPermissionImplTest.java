package com.jimuqu.common.satoken.core;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.satoken.service.PermissionProvider;
import com.jimuqu.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaPermissionImplTest {

    private static SaTokenContext originalContext;
    private final TestPermissionProvider provider = new TestPermissionProvider();
    private final SaPermissionImpl permissions = new SaPermissionImpl(provider);

    @BeforeAll
    static void setUpSaToken() {
        originalContext = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
    }

    @AfterAll
    static void restoreSaToken() {
        SaManager.setSaTokenContext(originalContext);
    }

    @Test
    void keepsAppUserPermissionsFromTheMatchingLoginSession() {
        LoginUser appUser = user(8101L, UserType.APP_USER,
                Set.of("app:profile:read"), Set.of("app_user"));
        SaTokenContextMockUtil.setMockContext();
        try {
            LoginHelper.login(appUser, new SaLoginParameter().setExtraData(Map.of()));

            assertEquals(Set.of("app:profile:read"),
                    Set.copyOf(permissions.getPermissionList(appUser.getLoginId(), "login")));
            assertEquals(Set.of("app_user"),
                    Set.copyOf(permissions.getRoleList(appUser.getLoginId(), "login")));
            assertEquals(0, provider.menuQueries);
            assertEquals(0, provider.roleQueries);
        } finally {
            StpUtil.logout(appUser.getLoginId());
            SaTokenContextMockUtil.clearContext();
        }
    }

    @Test
    void resolvesAnotherOfflineLoginIdFromPermissionProvider() {
        LoginUser current = user(8102L, UserType.SYS_USER,
                Set.of("*:*:*"), Set.of("superadmin"));
        provider.menuPermissions.put(999999L, Set.of("system:user:list"));
        provider.rolePermissions.put(999999L, Set.of("auditor"));
        SaTokenContextMockUtil.setMockContext();
        try {
            LoginHelper.login(current, new SaLoginParameter().setExtraData(Map.of()));

            assertEquals(Set.of("system:user:list"), Set.copyOf(
                    permissions.getPermissionList("sys_user:999999", "login")));
            assertEquals(Set.of("auditor"), Set.copyOf(
                    permissions.getRoleList("sys_user:999999", "login")));
            assertEquals(1, provider.menuQueries);
            assertEquals(1, provider.roleQueries);
        } finally {
            StpUtil.logout(current.getLoginId());
            SaTokenContextMockUtil.clearContext();
        }
    }

    @Test
    void resolvesAnotherOnlineLoginIdFromPermissionProvider() {
        LoginUser target = user(8103L, UserType.SYS_USER,
                Set.of("system:user:list"), Set.of("auditor"));
        provider.menuPermissions.put(8103L, Set.of("system:user:edit"));
        provider.rolePermissions.put(8103L, Set.of("db_auditor"));
        SaTokenContextMockUtil.setMockContext();
        LoginHelper.login(target, new SaLoginParameter().setExtraData(Map.of()));
        SaTokenContextMockUtil.clearContext();

        LoginUser current = user(8104L, UserType.SYS_USER,
                Set.of("system:role:list"), Set.of("operator"));
        SaTokenContextMockUtil.setMockContext();
        try {
            LoginHelper.login(current, new SaLoginParameter().setExtraData(Map.of()));

            assertEquals(Set.of("system:user:edit"), Set.copyOf(
                    permissions.getPermissionList(target.getLoginId(), "login")));
            assertEquals(Set.of("db_auditor"), Set.copyOf(
                    permissions.getRoleList(target.getLoginId(), "login")));
            assertEquals(1, provider.menuQueries);
            assertEquals(1, provider.roleQueries);
        } finally {
            StpUtil.logout(current.getLoginId());
            StpUtil.logout(target.getLoginId());
            SaTokenContextMockUtil.clearContext();
        }
    }

    @Test
    void resolvesPermissionWithoutCurrentLoginFromPermissionProvider() {
        provider.menuPermissions.put(8105L, Set.of("system:config:list"));
        provider.rolePermissions.put(8105L, Set.of("maintainer"));
        SaTokenContextMockUtil.setMockContext();
        try {
            assertEquals(Set.of("system:config:list"), Set.copyOf(
                    permissions.getPermissionList("sys_user:8105", "login")));
            assertEquals(Set.of("maintainer"), Set.copyOf(
                    permissions.getRoleList("sys_user:8105", "login")));
        } finally {
            SaTokenContextMockUtil.clearContext();
        }
    }

    @Test
    void rejectsMalformedLoginIdBeforeQueryingPermissionProvider() {
        SaTokenContextMockUtil.setMockContext();
        try {
            assertThrows(ServiceException.class,
                    () -> permissions.getPermissionList("invalid-login-id", "login"));
            assertThrows(ServiceException.class,
                    () -> permissions.getRoleList("sys_user:not-a-number", "login"));
            assertEquals(0, provider.menuQueries);
            assertEquals(0, provider.roleQueries);
        } finally {
            SaTokenContextMockUtil.clearContext();
        }
    }

    private static LoginUser user(Long userId, UserType userType,
                                  Set<String> menuPermissions, Set<String> rolePermissions) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUserType(userType.getUserType());
        user.setUsername("permission-" + userId);
        user.setMenuPermission(menuPermissions);
        user.setRolePermission(rolePermissions);
        return user;
    }

    private static final class TestPermissionProvider implements PermissionProvider {
        private final Map<Long, Set<String>> menuPermissions = new HashMap<>();
        private final Map<Long, Set<String>> rolePermissions = new HashMap<>();
        private int menuQueries;
        private int roleQueries;

        @Override
        public Set<String> getRolePermission(Long userId) {
            roleQueries++;
            return rolePermissions.getOrDefault(userId, Set.of());
        }

        @Override
        public Set<String> getMenuPermission(Long userId) {
            menuQueries++;
            return menuPermissions.getOrDefault(userId, Set.of());
        }
    }
}
