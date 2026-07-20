package com.jimuqu.auth.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.jimuqu.auth.domain.vo.LoginVo;
import com.jimuqu.auth.service.MiniProgramIdentityAdapter;
import com.jimuqu.auth.service.SysLoginService;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.domain.model.XcxLoginUser;
import com.jimuqu.common.core.enums.UserStatus;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.exception.user.UserException;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.vo.SysSocialVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.service.SysSocialService;
import com.jimuqu.system.service.SysUserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class XcxAuthStrategyTest {

    private static SaTokenContext originalContext;

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
    void logsInThroughAReplaceableAdapterResolvedUser() {
        MiniProgramIdentityAdapter adapter = mock(MiniProgramIdentityAdapter.class);
        SysSocialService socialService = mock(SysSocialService.class);
        SysUserService userService = mock(SysUserService.class);
        SysLoginService loginService = mock(SysLoginService.class);
        when(adapter.isAvailable()).thenReturn(true);
        when(adapter.authenticate("wx-app", "code-1")).thenReturn(
                new MiniProgramIdentityAdapter.MiniProgramIdentity(42L, "openid-42", "union-42"));
        SysUserVo user = enabledUser(42L);
        when(userService.queryById(42L)).thenReturn(user);
        when(loginService.buildLoginUser(user)).thenReturn(loginUser(42L));
        XcxAuthStrategy strategy = new XcxAuthStrategy(adapter, socialService, userService, loginService);

        SaTokenContextMockUtil.setMockContext();
        try {
            LoginVo result = strategy.login(body("code-1"), client());

            assertEquals("openid-42", result.getOpenid());
            assertEquals("client-id", result.getClientId());
            assertEquals(StpUtil.getTokenValue(), result.getAccessToken());
            XcxLoginUser sessionUser = assertInstanceOf(XcxLoginUser.class, LoginHelper.getLoginUser());
            assertEquals("openid-42", sessionUser.getOpenid());
            verifyNoInteractions(socialService);
        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
            SaTokenContextMockUtil.clearContext();
        }
    }

    @Test
    void requiresAnExistingBindingWhenAdapterDoesNotResolveUserId() {
        MiniProgramIdentityAdapter adapter = mock(MiniProgramIdentityAdapter.class);
        SysSocialService socialService = mock(SysSocialService.class);
        SysUserService userService = mock(SysUserService.class);
        SysLoginService loginService = mock(SysLoginService.class);
        when(adapter.isAvailable()).thenReturn(true);
        when(adapter.authenticate("wx-app", "code-1")).thenReturn(
                new MiniProgramIdentityAdapter.MiniProgramIdentity("unbound-openid", null));
        when(socialService.selectByAuthId("WECHAT_MINI_PROGRAMunbound-openid"))
                .thenReturn(List.of());
        XcxAuthStrategy strategy = new XcxAuthStrategy(adapter, socialService, userService, loginService);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> strategy.login(body("code-1"), client()));

        assertEquals("你还没有绑定小程序账号，绑定后才可以登录！", exception.getMessage());
        verifyNoInteractions(userService, loginService);
    }

    @Test
    void loadsTheUserFromAnExistingMiniProgramBinding() {
        MiniProgramIdentityAdapter adapter = mock(MiniProgramIdentityAdapter.class);
        SysSocialService socialService = mock(SysSocialService.class);
        SysUserService userService = mock(SysUserService.class);
        SysLoginService loginService = mock(SysLoginService.class);
        when(adapter.isAvailable()).thenReturn(true);
        when(adapter.authenticate("wx-app", "code-1")).thenReturn(
                new MiniProgramIdentityAdapter.MiniProgramIdentity("bound-openid", null));
        SysSocialVo social = new SysSocialVo();
        social.setUserId(43L);
        when(socialService.selectByAuthId("WECHAT_MINI_PROGRAMbound-openid"))
                .thenReturn(List.of(social));
        SysUserVo user = enabledUser(43L);
        when(userService.queryById(43L)).thenReturn(user);
        when(loginService.buildLoginUser(user)).thenReturn(loginUser(43L));
        XcxAuthStrategy strategy = new XcxAuthStrategy(adapter, socialService, userService, loginService);

        SaTokenContextMockUtil.setMockContext();
        try {
            LoginVo result = strategy.login(body("code-1"), client());

            assertEquals("bound-openid", result.getOpenid());
            verify(userService).queryById(43L);
        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
            SaTokenContextMockUtil.clearContext();
        }
    }

    @Test
    void rejectsUnavailableAdaptersAndDisabledUsers() {
        MiniProgramIdentityAdapter adapter = mock(MiniProgramIdentityAdapter.class);
        SysSocialService socialService = mock(SysSocialService.class);
        SysUserService userService = mock(SysUserService.class);
        SysLoginService loginService = mock(SysLoginService.class);
        XcxAuthStrategy strategy = new XcxAuthStrategy(adapter, socialService, userService, loginService);

        assertThrows(ServiceException.class, () -> strategy.login(body("code-1"), client()));
        verify(adapter, never()).authenticate("wx-app", "code-1");

        when(adapter.isAvailable()).thenReturn(true);
        when(adapter.authenticate("wx-app", "code-1")).thenReturn(
                new MiniProgramIdentityAdapter.MiniProgramIdentity(44L, "disabled-openid", null));
        when(userService.queryById(44L)).thenReturn(enabledUser(44L).setStatus(UserStatus.DISABLE.getCode()));

        assertThrows(UserException.class, () -> strategy.login(body("code-1"), client()));
        verifyNoInteractions(loginService);
    }

    private String body(String code) {
        return "{\"clientId\":\"client-id\",\"grantType\":\"xcx\","
                + "\"appid\":\"wx-app\",\"xcxCode\":\"" + code + "\"}";
    }

    private SysClient client() {
        return new SysClient()
                .setClientId("client-id")
                .setClientKey("mini-client")
                .setDeviceType("xcx")
                .setTimeout(604800L)
                .setActiveTimeout(1800L);
    }

    private SysUserVo enabledUser(Long userId) {
        return new SysUserVo()
                .setId(userId)
                .setUserName("mini-user-" + userId)
                .setNickName("Mini User")
                .setUserType(UserType.SYS_USER.getUserType())
                .setStatus(UserStatus.OK.getCode());
    }

    private LoginUser loginUser(Long userId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUserType(UserType.SYS_USER.getUserType());
        user.setUsername("mini-user-" + userId);
        user.setMenuPermission(Set.of("system:profile:list"));
        user.setRolePermission(Set.of("user"));
        return user;
    }
}
