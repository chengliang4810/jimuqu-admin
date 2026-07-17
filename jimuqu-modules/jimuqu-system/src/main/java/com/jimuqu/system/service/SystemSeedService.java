package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.SysConfig;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysDictData;
import com.jimuqu.system.domain.SysDictType;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.SysPost;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.SysRoleMenu;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.SysUserPost;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.mapper.SysClientMapper;
import com.jimuqu.system.mapper.SysConfigMapper;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysDictDataMapper;
import com.jimuqu.system.mapper.SysDictTypeMapper;
import com.jimuqu.system.mapper.SysMenuMapper;
import com.jimuqu.system.mapper.SysPostMapper;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysRoleMenuMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.mapper.SysUserPostMapper;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;

/**
 * 使用 Xbatis 幂等写入系统运行所需的最小基础数据。
 */
@Component
@RequiredArgsConstructor
public class SystemSeedService {

    private static final String DEFAULT_PASSWORD =
            "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

    private final SysClientMapper clientMapper;
    private final SysConfigMapper configMapper;
    private final SysDeptMapper deptMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysPostMapper postMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysMenuMapper menuMapper;

    @Transaction
    public void initialize() {
        seedClient();
        seedConfig();
        seedDictionaries();
        seedDepartments();
        seedRoles();
        seedUsers();
        seedPosts();
        seedMenus();
        seedRoleMenus();
    }

    private void seedConfig() {
        insertConfig(1L, "用户初始密码", "sys.user.initPassword", "admin123", "新增用户的默认密码");
        insertConfig(2L, "是否开启用户注册功能", "sys.account.registerUser", "false",
                "true:开启, false:关闭");
        insertConfig(11L, "OSS预览列表资源开关", "sys.oss.previewListResource", "true",
                "true:开启, false:关闭");
    }

    private void insertConfig(Long id, String name, String key, String value, String remark) {
        if (QueryChain.of(configMapper).eq(SysConfig::getConfigKey, key).exists()) {
            return;
        }
        configMapper.save(new SysConfig()
                .setId(id)
                .setConfigName(name)
                .setConfigKey(key)
                .setConfigValue(value)
                .setConfigType("Y")
                .setRemark(remark));
    }

    private void seedClient() {
        if (QueryChain.of(clientMapper).eq(SysClient::getClientKey, "pc").exists()) {
            return;
        }
        clientMapper.save(new SysClient()
                .setId(1L)
                .setClientId("e5cd7e4891bf95d1d19206ce24a7b32e")
                .setClientKey("pc")
                .setClientSecret("pc123")
                .setGrantType("password,sms,email,social")
                .setDeviceType("pc")
                .setActiveTimeout(1800L)
                .setTimeout(604800L)
                .setStatus("0")
                .setDelFlag("0"));
    }

    private void seedDictionaries() {
        List<DictSeed> dictionaries = List.of(
                new DictSeed("sys_normal_disable", "正常状态", List.of("正常|0|primary", "停用|1|danger")),
                new DictSeed("sys_common_status", "通用状态", List.of("正常|0|primary", "关闭|1|danger")),
                new DictSeed("sys_yes_no", "是否", List.of("是|Y|primary", "否|N|info")),
                new DictSeed("sys_show_hide", "显示状态", List.of("显示|0|primary", "隐藏|1|info")),
                new DictSeed("sys_user_gender", "用户性别", List.of("男|0|primary", "女|1|success", "未知|2|info")),
                new DictSeed("sys_notice_type", "通知类型", List.of("通知|1|primary", "公告|2|warning")),
                new DictSeed("sys_notice_status", "通知状态", List.of("正常|0|primary", "关闭|1|danger")),
                new DictSeed("sys_oper_type", "操作类型", List.of("其他|0|info", "新增|1|primary", "修改|2|warning", "删除|3|danger", "授权|4|success", "导出|5|primary", "导入|6|primary", "强退|7|danger", "生成代码|8|info", "清空数据|9|danger")),
                new DictSeed("sys_grant_type", "授权类型", List.of("密码|password|primary", "短信|sms|success", "邮箱|email|warning", "社交|social|info", "小程序|xcx|primary")),
                new DictSeed("sys_device_type", "设备类型", List.of("PC|pc|primary", "Android|android|success", "iOS|ios|info", "小程序|mini_program|warning"))
        );
        long typeId = 1L;
        long dataId = 1L;
        for (DictSeed dictionary : dictionaries) {
            if (!QueryChain.of(dictTypeMapper).eq(SysDictType::getDictKey, dictionary.key()).exists()) {
                dictTypeMapper.save(new SysDictType()
                        .setDictId(typeId)
                        .setDictKey(dictionary.key())
                        .setDictName(dictionary.name())
                        .setDictType("L")
                        .setIsBuiltIn("Y"));
            }
            long sort = 1L;
            for (String encoded : dictionary.values()) {
                String[] parts = encoded.split("\\|", -1);
                if (!QueryChain.of(dictDataMapper)
                        .eq(SysDictData::getDictTypeKey, dictionary.key())
                        .eq(SysDictData::getDictValue, parts[1])
                        .exists()) {
                    dictDataMapper.save(new SysDictData()
                            .setId(dataId)
                            .setParentId(0L)
                            .setDictSort(sort)
                            .setDictLabel(parts[0])
                            .setDictValue(parts[1])
                            .setDictTypeKey(dictionary.key())
                            .setListClass(parts[2])
                            .setIsDefault("N"));
                }
                dataId++;
                sort++;
            }
            typeId++;
        }
    }

    private void seedDepartments() {
        insertDept(100L, 0L, "0", "积木区科技", 0);
        insertDept(101L, 100L, "0,100", "总部", 1);
        insertDept(103L, 101L, "0,100,101", "研发部", 1);
        insertDept(104L, 103L, "0,100,101,103", "平台组", 1);
        insertDept(105L, 101L, "0,100,101", "市场部", 2);
    }

    private void insertDept(Long id, Long parentId, String ancestors, String name, int order) {
        if (QueryChain.of(deptMapper).eq(SysDept::getId, id).exists()) {
            return;
        }
        deptMapper.save(new SysDept()
                .setId(id)
                .setParentId(parentId)
                .setAncestors(ancestors)
                .setDeptName(name)
                .setOrderNum(order)
                .setStatus("0")
                .setDelFlag("0"));
    }

    private void seedRoles() {
        insertRole(1L, "超级管理员", "superadmin", "1", 1);
        insertRole(2L, "自定义部门", "custom", "2", 2);
        insertRole(3L, "本部门", "department", "3", 3);
        insertRole(4L, "本部门及以下", "department_child", "4", 4);
        insertRole(5L, "仅本人", "self", "5", 5);
        insertRole(6L, "无权限", "no_permission", "5", 6);
        if (!QueryChain.of(roleDeptMapper)
                .eq(SysRoleDept::getRoleId, 2L)
                .eq(SysRoleDept::getDeptId, 103L)
                .exists()) {
            SysRoleDept relation = new SysRoleDept();
            relation.setRoleId(2L);
            relation.setDeptId(103L);
            roleDeptMapper.save(relation);
        }
    }

    private void insertRole(Long id, String name, String key, String scope, int order) {
        if (QueryChain.of(roleMapper).eq(SysRole::getId, id).exists()) {
            return;
        }
        roleMapper.save(new SysRole()
                .setId(id)
                .setRoleName(name)
                .setRoleKey(key)
                .setRoleSort(order)
                .setDataScope(scope)
                .setMenuCheckStrictly(true)
                .setDeptCheckStrictly(true)
                .setStatus("0")
                .setDelFlag("0"));
    }

    private void seedUsers() {
        insertUser(1L, 103L, "admin", "系统管理员", "0", 1L);
        insertUser(2L, 103L, "custom_user", "自定义部门用户", "0", 2L);
        insertUser(3L, 103L, "dept_user", "本部门用户", "0", 3L);
        insertUser(4L, 103L, "dept_child_user", "部门及以下用户", "0", 4L);
        insertUser(5L, 104L, "self_user", "仅本人用户", "0", 5L);
        insertUser(6L, 105L, "disabled_user", "停用用户", "1", 5L);
        insertUser(7L, 105L, "no_permission", "无权限用户", "0", 6L);
    }

    private void insertUser(Long id, Long deptId, String name, String nickName, String status, Long roleId) {
        if (!QueryChain.of(userMapper).eq(SysUser::getId, id).exists()) {
            userMapper.save(new SysUser()
                    .setId(id)
                    .setDeptId(deptId)
                    .setUserName(name)
                    .setNickName(nickName)
                    .setUserType("pc_user")
                    .setEmail(name + "@jimuqu.local")
                    .setPhonenumber("1380000000" + id)
                    .setSex("0")
                    .setPassword(DEFAULT_PASSWORD)
                    .setStatus(status)
                    .setDelFlag("0"));
        }
        if (!QueryChain.of(userRoleMapper)
                .eq(SysUserRole::getUserId, id)
                .eq(SysUserRole::getRoleId, roleId)
                .exists()) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(id);
            relation.setRoleId(roleId);
            userRoleMapper.save(relation);
        }
    }

    private void seedPosts() {
        if (!QueryChain.of(postMapper).eq(SysPost::getPostId, 1L).exists()) {
            postMapper.save(new SysPost()
                    .setPostId(1L)
                    .setDeptId(103L)
                    .setPostCode("admin")
                    .setPostName("管理员")
                    .setPostSort(1L)
                    .setStatus("0"));
        }
        if (!QueryChain.of(userPostMapper)
                .eq(SysUserPost::getUserId, 1L)
                .eq(SysUserPost::getPostId, 1L)
                .exists()) {
            SysUserPost relation = new SysUserPost();
            relation.setUserId(1L);
            relation.setPostId(1L);
            userPostMapper.save(relation);
        }
    }

    private void seedMenus() {
        List<MenuSeed> menus = List.of(
                new MenuSeed(1, 0, "系统管理", 1, "system", "", "M", "system", ""),
                new MenuSeed(2, 0, "系统监控", 2, "monitor", "", "M", "monitor", ""),
                new MenuSeed(3, 0, "资源管理", 3, "resource", "", "M", "resource", ""),
                new MenuSeed(100, 1, "用户管理", 1, "user", "system/user/index", "C", "user", "system:user:list"),
                new MenuSeed(101, 1, "角色管理", 2, "role", "system/role/index", "C", "peoples", "system:role:list"),
                new MenuSeed(102, 1, "菜单管理", 3, "menu", "system/menu/index", "C", "tree-table", "system:menu:list"),
                new MenuSeed(103, 1, "部门管理", 4, "dept", "system/dept/index", "C", "tree", "system:dept:list"),
                new MenuSeed(104, 1, "岗位管理", 5, "post", "system/post/index", "C", "post", "system:post:list"),
                new MenuSeed(105, 1, "字典管理", 6, "dict", "system/dict/type/index", "C", "dict", "system:dict:list"),
                new MenuSeed(106, 1, "参数设置", 7, "config", "system/config/index", "C", "edit", "system:config:list"),
                new MenuSeed(107, 1, "通知公告", 8, "notice", "system/notice/index", "C", "message", "system:notice:list"),
                new MenuSeed(108, 1, "客户端管理", 9, "client", "system/client/index", "C", "client", "system:client:list"),
                new MenuSeed(200, 2, "在线用户", 1, "online", "monitor/online/index", "C", "online", "monitor:online:list"),
                new MenuSeed(201, 2, "操作日志", 2, "operlog", "monitor/operlog/index", "C", "form", "monitor:operlog:list"),
                new MenuSeed(202, 2, "登录日志", 3, "loginInfo", "monitor/logininfo/index", "C", "logininfor", "monitor:logininfor:list"),
                new MenuSeed(203, 2, "缓存监控", 4, "cache", "monitor/cache/index", "C", "redis", "monitor:cache:list"),
                new MenuSeed(300, 3, "文件管理", 1, "oss", "system/oss/index", "C", "upload", "system:oss:list"),
                new MenuSeed(301, 3, "存储配置", 2, "oss-config", "system/oss-config/index", "C", "server", "system:ossConfig:list")
        );
        for (MenuSeed menu : menus) {
            if (QueryChain.of(menuMapper).eq(SysMenu::getId, menu.id()).exists()) {
                continue;
            }
            menuMapper.save(new SysMenu()
                    .setId(menu.id())
                    .setParentId(menu.parentId())
                    .setMenuName(menu.name())
                    .setOrderNum(menu.order())
                    .setPath(menu.path())
                    .setComponent(menu.component())
                    .setIsFrame("1")
                    .setIsCache("0")
                    .setMenuType(menu.type())
                    .setVisible("0")
                    .setStatus("0")
                    .setPerms(menu.permission())
                    .setIcon(menu.icon()));
        }
    }

    private void seedRoleMenus() {
        for (long roleId = 2L; roleId <= 5L; roleId++) {
            for (long menuId : List.of(1L, 100L)) {
                if (!QueryChain.of(roleMenuMapper)
                        .eq(SysRoleMenu::getRoleId, roleId)
                        .eq(SysRoleMenu::getMenuId, menuId)
                        .exists()) {
                    SysRoleMenu relation = new SysRoleMenu();
                    relation.setRoleId(roleId);
                    relation.setMenuId(menuId);
                    roleMenuMapper.save(relation);
                }
            }
        }
    }

    private record MenuSeed(long id, long parentId, String name, int order, String path,
                            String component, String type, String icon, String permission) {
    }

    private record DictSeed(String key, String name, List<String> values) {
    }
}
