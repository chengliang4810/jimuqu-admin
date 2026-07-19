package com.jimuqu.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.common.core.sensitive.annotation.Sensitive;
import com.jimuqu.common.core.sensitive.enums.SensitiveType;
import com.jimuqu.common.satoken.core.SaPermissionImpl;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.web.sensitive.SensitiveJsonRender;
import com.jimuqu.system.domain.vo.SysUserVo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.noear.snack4.ONode;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.serialization.snack4.Snack4StringSerializer;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysSensitiveServiceImplTest {

    private static final SysSensitiveServiceImpl SERVICE = new SysSensitiveServiceImpl();
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
    void masksSensitiveFieldsWhenNotLoggedIn() throws Throwable {
        assertContact(serialize(contact(), null), "t***@example.com", "138****5678");
    }

    @Test
    void masksSensitiveFieldsWithoutPermission() throws Throwable {
        assertContact(serialize(contact(), user(2L, Set.of(), Set.of())),
                "t***@example.com", "138****5678");
    }

    @Test
    void keepsSensitiveFieldsWithPermission() throws Throwable {
        assertContact(serialize(contact(), user(3L, Set.of("system:user:edit"), Set.of())),
                "test@example.com", "13812345678");
    }

    @Test
    void keepsSensitiveFieldsForSuperAdmin() throws Throwable {
        assertContact(serialize(contact(), user(UserConstants.SUPER_ADMIN_ID, Set.of(), Set.of())),
                "test@example.com", "13812345678");
    }

    @Test
    void requiresMatchingRoleAndPermissionWhenBothAreConfigured() throws Throwable {
        AccessView value = new AccessView();
        Map<?, ?> roleOnly = serialize(value, user(4L, Set.of(), Set.of("auditor")));
        Map<?, ?> roleAndPermission = serialize(value,
                user(5L, Set.of("system:user:view"), Set.of("auditor")));

        assertEquals("138****5678", roleOnly.get("phoneNumber"));
        assertEquals("13812345678", roleAndPermission.get("phoneNumber"));
    }

    private static Map<?, ?> serialize(Object value, LoginUser loginUser) throws Throwable {
        SaTokenContextMockUtil.setMockContext();
        try {
            if (loginUser != null) {
                LoginHelper.login(loginUser, new SaLoginParameter().setExtraData(Map.of()));
            }
            String json = new SensitiveJsonRender(new Snack4StringSerializer(), SERVICE)
                    .renderAndReturn(value, ContextEmpty.create());
            return ONode.deserialize(json, Map.class);
        } finally {
            if (loginUser != null) {
                StpUtil.logout();
            }
            SaTokenContextMockUtil.clearContext();
        }
    }

    private static SysUserVo contact() {
        return new SysUserVo()
                .setEmail("test@example.com")
                .setPhonenumber("13812345678");
    }

    private static LoginUser user(Long userId, Set<String> permissions, Set<String> roles) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUserType(UserType.SYS_USER.getUserType());
        user.setMenuPermission(permissions);
        user.setRolePermission(roles);
        return user;
    }

    private static void assertContact(Map<?, ?> value, String email, String phoneNumber) {
        assertEquals(email, value.get("email"));
        assertEquals(phoneNumber, value.get("phoneNumber"));
    }

    private static final class AccessView {

        @Sensitive(type = SensitiveType.MOBILE,
                roleKey = {"auditor", "operator"},
                perms = {"system:user:view", "system:user:edit"})
        private final String phoneNumber = "13812345678";

        public String getPhoneNumber() {
            return phoneNumber;
        }
    }
}
