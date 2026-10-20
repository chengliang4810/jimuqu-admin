package com.jimuqu.common.satoken.core;

import cn.dev33.satoken.exception.SaJsonConvertException;
import cn.dev33.satoken.json.SaJsonTemplateForSnack4;
import cn.dev33.satoken.session.SaSession;
import com.jimuqu.common.core.domain.dto.PostDTO;
import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.satoken.config.SaTokenConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SaTokenJsonCompatibilityTest {
    private final SaJsonTemplateForSnack4 json = new SaTokenConfig().saJsonTemplate();

    @Test
    void readsPersistedLoginSessionIncludingNestedPermissionTypes() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUserType("sys_user");
        user.setMenuPermission(Set.of("system:user:list"));
        RoleDTO role = new RoleDTO();
        role.setRoleId(2L);
        role.setRoleKey("reader");
        user.setRoles(List.of(role));
        PostDTO post = new PostDTO();
        post.setPostId(3L);
        user.setPosts(List.of(post));
        user.setDataScopeRoleMap(new HashMap<>(Map.of("system:user:list", new ArrayList<>(List.of(2L)))));
        SaSession session = new SaSession("compat-session");
        session.set(LoginHelper.LOGIN_USER_KEY, user);

        SaSession restored = json.jsonToObject(json.objectToJson(session), SaSession.class);
        LoginUser actual = assertInstanceOf(LoginUser.class, restored.get(LoginHelper.LOGIN_USER_KEY));
        assertEquals(user.getUserId(), actual.getUserId());
        assertEquals(user.getMenuPermission(), actual.getMenuPermission());
        assertEquals(user.getDataScopeRoleMap(), actual.getDataScopeRoleMap());
        assertEquals("reader", actual.getRoles().getFirst().getRoleKey());
        assertEquals(3L, actual.getPosts().getFirst().getPostId());
    }

    @Test
    void readsImmutablePermissionMapsWithoutReconstructingTheirPrivateJdkImplementation() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUserType("sys_user");
        user.setDataScopeRoleMap(Map.of("system:user:list", List.of(2L)));
        LoginUser restored = json.jsonToObject(json.objectToJson(user), LoginUser.class);
        assertEquals(user.getDataScopeRoleMap(), restored.getDataScopeRoleMap());
    }

    @Test
    void stillRejectsUnregisteredPolymorphicTypes() {
        SaSession session = new SaSession("blocked-session");
        session.set("untrusted", new UnregisteredType());
        assertThrows(SaJsonConvertException.class,
                () -> json.jsonToObject(json.objectToJson(session), SaSession.class));
    }

    public static class UnregisteredType {
        public String value = "blocked";
    }
}
