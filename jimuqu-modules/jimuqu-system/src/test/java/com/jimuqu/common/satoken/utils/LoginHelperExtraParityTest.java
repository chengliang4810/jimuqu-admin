package com.jimuqu.common.satoken.utils;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.common.satoken.core.SaPermissionImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginHelperExtraParityTest {

    private static SaTokenContext originalContext;
    private static StpInterface originalStpInterface;

    @BeforeAll
    static void setUpSaToken() {
        originalContext = SaManager.getSaTokenContext();
        originalStpInterface = SaManager.getStpInterface();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaManager.setStpInterface(new SaPermissionImpl());
    }

    @AfterAll
    static void restoreSaToken() {
        SaManager.setStpInterface(originalStpInterface);
        SaManager.setSaTokenContext(originalContext);
    }

    @Test
    void storesIdentityAndDepartmentValuesAsTokenExtras() {
        SaTokenContextMockUtil.setMockContext();
        try {
            LoginUser user = new LoginUser();
            user.setUserId(42L);
            user.setUserType(UserType.SYS_USER.getUserType());
            user.setUsername("tester");
            user.setDeptId(103L);
            user.setDeptName("研发部门");
            user.setDeptCategory("technology");

            LoginHelper.login(user, new SaLoginParameter());

            assertEquals(42L, LoginHelper.getUserId());
            assertEquals("42", LoginHelper.getUserIdStr());
            assertEquals("tester", LoginHelper.getUsername());
            assertEquals(103L, LoginHelper.getDeptId());
            assertEquals("研发部门", LoginHelper.getDeptName());
            assertEquals("technology", LoginHelper.getDeptCategory());
        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
            SaTokenContextMockUtil.clearContext();
        }
    }

    @Test
    void preservesImmutableClientExtrasWhenAddingIdentityContext() {
        SaTokenContextMockUtil.setMockContext();
        try {
            LoginUser user = new LoginUser();
            user.setUserId(43L);
            user.setUserType(UserType.SYS_USER.getUserType());
            user.setUsername("immutable-extra-user");
            user.setDeptId(104L);
            user.setDeptName("测试部门");
            user.setDeptCategory("testing");
            Map<String, Object> originalExtras = Map.of(LoginHelper.CLIENT_KEY, "web-client");
            SaLoginParameter parameter = new SaLoginParameter().setExtraData(originalExtras);

            LoginHelper.login(user, parameter);

            assertEquals("web-client", StpUtil.getTokenSession().get(LoginHelper.CLIENT_KEY));
            assertEquals(43L, LoginHelper.getUserId());
            assertEquals("immutable-extra-user", LoginHelper.getUsername());
            assertEquals(104L, LoginHelper.getDeptId());
            assertEquals("测试部门", LoginHelper.getDeptName());
            assertEquals("testing", LoginHelper.getDeptCategory());
            assertEquals(Map.of(LoginHelper.CLIENT_KEY, "web-client"), originalExtras);
        } finally {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
            SaTokenContextMockUtil.clearContext();
        }
    }
}
