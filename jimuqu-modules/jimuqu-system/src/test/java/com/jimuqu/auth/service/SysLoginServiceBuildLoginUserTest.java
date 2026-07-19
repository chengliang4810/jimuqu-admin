package com.jimuqu.auth.service;

import com.jimuqu.common.core.domain.dto.RoleDTO;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.service.ISysPermissionService;
import com.jimuqu.system.service.SysPostService;
import com.jimuqu.system.service.SysRoleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysLoginServiceBuildLoginUserTest {

    @Test
    void loginTimestampsUseUpstreamLongType() throws NoSuchFieldException {
        assertEquals(Long.class, LoginUser.class.getDeclaredField("loginTime").getType());
        assertEquals(Long.class, LoginUser.class.getDeclaredField("expireTime").getType());
    }

    @Test
    void buildsCompleteUpstreamCompatibleLoginContext() {
        ISysPermissionService permissionService = mock(ISysPermissionService.class);
        SysRoleService roleService = mock(SysRoleService.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysPostService postService = mock(SysPostService.class);
        SysLoginService service = new SysLoginService(
                permissionService, null, null, roleService, deptMapper, postService);

        SysRoleVo role = new SysRoleVo().setId(7L).setRoleName("审计员")
                .setRoleKey("auditor").setDataScope("3");
        SysPostVo post = new SysPostVo().setPostId(9L).setDeptId(103L)
                .setPostCode("audit").setPostName("审计岗").setPostCategory("control");
        SysDeptVo dept = new SysDeptVo().setId(103L).setDeptName("研发部门")
                .setDeptCategory("technology");
        when(roleService.selectRolesByUserId(42L)).thenReturn(List.of(role));
        when(postService.selectPostsByUserId(42L)).thenReturn(List.of(post));
        when(deptMapper.getById(103L, SysDeptVo.class)).thenReturn(dept);
        when(permissionService.getMenuPermission(42L)).thenReturn(Set.of("system:user:list"));
        when(permissionService.getRolePermission(42L)).thenReturn(Set.of("auditor"));
        when(permissionService.getDataScopeRoleMap(anyList())).thenAnswer(invocation -> {
            List<RoleDTO> roles = invocation.getArgument(0);
            assertEquals(7L, roles.get(0).getRoleId());
            return Map.of("system:user:list", List.of(7L));
        });

        LoginUser loginUser = service.buildLoginUser(new SysUserVo()
                .setId(42L).setDeptId(103L).setUserName("tester")
                .setNickName("测试用户").setUserType("sys_user"));

        assertEquals("研发部门", loginUser.getDeptName());
        assertEquals("technology", loginUser.getDeptCategory());
        assertEquals(7L, loginUser.getRoles().get(0).getRoleId());
        assertEquals(List.of(7L), loginUser.getDataScopeRoleMap().get("system:user:list"));
        assertEquals(9L, loginUser.getPosts().get(0).getPostId());
        assertEquals("control", loginUser.getPosts().get(0).getPostCategory());
    }
}
