package com.jimuqu.system.domain;

import cn.xbatis.core.logicDelete.LogicDeleteSwitch;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.Application;
import com.jimuqu.system.domain.bo.SysClientBo;
import com.jimuqu.system.domain.bo.SysPostBo;
import com.jimuqu.system.domain.bo.SysRoleBo;
import com.jimuqu.system.domain.bo.SysUserBo;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.mapper.SysClientMapper;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysPostMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.service.SysClientService;
import com.jimuqu.system.service.SysDeptService;
import com.jimuqu.system.service.SysPostService;
import com.jimuqu.system.service.SysRoleService;
import com.jimuqu.system.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SolonTest(value = Application.class, env = "test", debug = false)
public class LogicalDeleteIntegrationTest {

    @Inject
    private SysUserMapper userMapper;
    @Inject
    private SysRoleMapper roleMapper;
    @Inject
    private SysDeptMapper deptMapper;
    @Inject
    private SysPostMapper postMapper;
    @Inject
    private SysClientMapper clientMapper;
    @Inject
    private SysUserService userService;
    @Inject
    private SysRoleService roleService;
    @Inject
    private SysDeptService deptService;
    @Inject
    private SysPostService postService;
    @Inject
    private SysClientService clientService;

    @Test
    void deleteByIdsKeepsPhysicalRowsButHidesThemAndAllowsReuse() {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36);
        String userName = "logic_user_" + suffix;
        String roleName = "逻辑角色-" + suffix;
        String roleKey = "logic_role_" + suffix;
        String deptName = "逻辑部门-" + suffix;
        String postName = "逻辑岗位-" + suffix;
        String postCode = "logic_post_" + suffix;
        String clientKey = "logic_client_" + suffix;
        List<Long> userIds = new ArrayList<>();
        List<Long> roleIds = new ArrayList<>();
        List<Long> deptIds = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();
        List<Long> clientIds = new ArrayList<>();

        try {
            SysUser user = user(userName, suffix);
            SysRole role = role(roleName, roleKey);
            SysDept dept = dept(deptName);
            SysPost post = post(postName, postCode);
            SysClient client = client(clientKey, suffix);
            assertEquals(1, userMapper.save(user));
            assertEquals(1, roleMapper.save(role));
            assertEquals(1, deptMapper.save(dept));
            assertEquals(1, postMapper.save(post));
            assertEquals(1, clientMapper.save(client));
            userIds.add(user.getId());
            roleIds.add(role.getId());
            deptIds.add(dept.getId());
            postIds.add(post.getPostId());
            clientIds.add(client.getId());

            assertEquals(1, userMapper.deleteByIds(List.of(user.getId())));
            assertEquals(1, roleMapper.deleteByIds(List.of(role.getId())));
            assertEquals(1, deptMapper.deleteByIds(List.of(dept.getId())));
            assertEquals(1, postMapper.deleteByIds(List.of(post.getPostId())));
            assertEquals(1, clientMapper.deleteByIds(List.of(client.getId())));

            assertNull(userMapper.getById(user.getId()));
            assertNull(roleMapper.getById(role.getId()));
            assertNull(deptMapper.getById(dept.getId()));
            assertNull(postMapper.getById(post.getPostId()));
            assertNull(clientMapper.getById(client.getId()));
            assertTrue(QueryChain.of(userMapper).eq(SysUser::getId, user.getId()).list().isEmpty());
            assertTrue(QueryChain.of(roleMapper).eq(SysRole::getId, role.getId()).list().isEmpty());
            assertTrue(QueryChain.of(deptMapper).eq(SysDept::getId, dept.getId()).list().isEmpty());
            assertTrue(QueryChain.of(postMapper).eq(SysPost::getPostId, post.getPostId()).list().isEmpty());
            assertTrue(QueryChain.of(clientMapper).eq(SysClient::getId, client.getId()).list().isEmpty());

            try (LogicDeleteSwitch ignored = LogicDeleteSwitch.with(false)) {
                assertEquals("1", userMapper.getById(user.getId()).getDelFlag());
                assertEquals("1", roleMapper.getById(role.getId()).getDelFlag());
                assertEquals("1", deptMapper.getById(dept.getId()).getDelFlag());
                assertEquals("1", postMapper.getById(post.getPostId()).getDelFlag());
                assertEquals("1", clientMapper.getById(client.getId()).getDelFlag());
            }

            SysUserBo userBo = new SysUserBo().setUserName(userName)
                    .setPhonenumber("139" + suffix.substring(0, Math.min(8, suffix.length())))
                    .setEmail("logic-" + suffix + "@example.com");
            assertTrue(userService.checkUserNameUnique(userBo));
            assertTrue(userService.checkPhoneUnique(userBo));
            assertTrue(userService.checkEmailUnique(userBo));
            assertTrue(roleService.checkRoleNameUnique(new SysRoleBo().setRoleName(roleName)));
            assertTrue(roleService.checkRoleKeyUnique(new SysRoleBo().setRoleKey(roleKey)));
            assertTrue(deptService.checkDeptNameUnique(new SysDeptQuery().setParentId(0L).setDeptName(deptName)));
            assertTrue(postService.checkPostNameUnique(new SysPostBo().setDeptId(103L).setPostName(postName)));
            assertTrue(postService.checkPostCodeUnique(new SysPostBo().setPostCode(postCode)));
            assertTrue(clientService.checkClientKeyUnique(new SysClientBo().setClientKey(clientKey)));

            SysUser replacementUser = user(userName, suffix);
            SysRole replacementRole = role(roleName, roleKey);
            SysDept replacementDept = dept(deptName);
            SysPost replacementPost = post(postName, postCode);
            SysClient replacementClient = client(clientKey, suffix);
            assertEquals(1, userMapper.save(replacementUser));
            assertEquals(1, roleMapper.save(replacementRole));
            assertEquals(1, deptMapper.save(replacementDept));
            assertEquals(1, postMapper.save(replacementPost));
            assertEquals(1, clientMapper.save(replacementClient));
            userIds.add(replacementUser.getId());
            roleIds.add(replacementRole.getId());
            deptIds.add(replacementDept.getId());
            postIds.add(replacementPost.getPostId());
            clientIds.add(replacementClient.getId());
            assertNotEquals(user.getId(), replacementUser.getId());
            assertNotEquals(role.getId(), replacementRole.getId());
            assertNotEquals(dept.getId(), replacementDept.getId());
            assertNotEquals(post.getPostId(), replacementPost.getPostId());
            assertNotEquals(client.getId(), replacementClient.getId());
        } finally {
            try (LogicDeleteSwitch ignored = LogicDeleteSwitch.with(false)) {
                userMapper.deleteByIds(userIds);
                roleMapper.deleteByIds(roleIds);
                deptMapper.deleteByIds(deptIds);
                postMapper.deleteByIds(postIds);
                clientMapper.deleteByIds(clientIds);
            }
        }
    }

    private static SysUser user(String userName, String suffix) {
        String phone = "139" + suffix.substring(0, Math.min(8, suffix.length()));
        return new SysUser().setDeptId(103L).setUserName(userName).setNickName(userName)
                .setEmail("logic-" + suffix + "@example.com").setPhonenumber(phone)
                .setPassword("test").setStatus("0").setDelFlag("0");
    }

    private static SysRole role(String roleName, String roleKey) {
        return new SysRole().setRoleName(roleName).setRoleKey(roleKey).setRoleSort(99)
                .setDataScope("1").setStatus("0").setDelFlag("0");
    }

    private static SysDept dept(String deptName) {
        return new SysDept().setParentId(0L).setAncestors("0").setDeptName(deptName)
                .setOrderNum(99).setStatus("0").setDelFlag("0");
    }

    private static SysPost post(String postName, String postCode) {
        return new SysPost().setDeptId(103L).setPostName(postName).setPostCode(postCode)
                .setPostSort(99).setStatus("0").setDelFlag("0");
    }

    private static SysClient client(String clientKey, String suffix) {
        return new SysClient().setClientId("logic-" + suffix).setClientKey(clientKey)
                .setClientSecret("test").setGrantType("password").setDeviceType("pc")
                .setActiveTimeout(-1L).setTimeout(604800L).setStatus("0").setDelFlag("0");
    }
}
