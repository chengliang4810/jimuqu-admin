---
title: 数据权限使用示例
description: 数据范围注解、表达式、配置与最佳实践
---

## 概述

本文档介绍了如何在jimuqu-admin项目中使用数据权限功能。数据权限功能基于XBatis QueryChain和自定义表达式解析器实现，支持多种权限级别的数据访问控制。

## 数据权限类型

| 权限类型 | 代码 | 说明 |
|---------|------|------|
| 全部数据权限 | 1 | 可以访问所有数据 |
| 自定数据权限 | 2 | 只能访问指定部门的数据 |
| 部门数据权限 | 3 | 只能访问本部门的数据 |
| 部门及以下数据权限 | 4 | 可以访问本部门及以下所有部门的数据 |
| 仅本人数据权限 | 5 | 只能访问自己的数据 |

## 核心组件

### 1. 数据权限注解

#### @DataPermission
用于标记需要数据权限控制的方法或类。

```java
@DataPermission({
    @DataColumn(key = "deptName", value = "d.dept_id"),
    @DataColumn(key = "userName", value = "u.user_id")
})
public List<SysUserVo> selectUserList(SysUserBo user) {
    // 查询逻辑
}
```

#### @DataColumn
定义数据权限的列映射关系。

- `key`: 表达式中的占位符关键字
- `value`: 占位符对应的数据库字段

### 2. 数据权限服务

### SysDataScopeService
系统数据权限服务，提供权限查询相关方法。

```java
@Component("sysDataScopeService")
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    // 获取角色自定义权限部门ID列表
    List<Long> getRoleCustom(Long roleId);

    // 获取部门及以下部门ID列表
    List<Long> getDeptAndChild(Long deptId);

    // 获取用户数据权限部门ID列表
    List<Long> getUserDataScope(Long userId);

    // 检查用户是否有部门数据权限
    boolean checkUserDataScope(Long userId, Long deptId);
}
```

## 使用示例

### 1. 控制器中使用数据权限

```java
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 获取用户列表
     * 应用数据权限：用户只能查看自己权限范围内的用户
     */
    @GetMapping("/list")
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    public TableDataInfo<SysUserVo> list(SysUserBo user, PageQuery pageQuery) {
        return sysUserService.selectPageUserList(user, pageQuery);
    }

    /**
     * 根据用户ID获取用户信息
     * 需要检查数据权限
     */
    @GetMapping("/{userId}")
    public R<SysUserVo> getInfo(@PathVariable Long userId) {
        sysUserService.checkUserDataScope(userId);
        return R.ok(sysUserService.queryById(userId));
    }
}
```

### 2. 服务层中使用数据权限

```java
@Service
public class SysUserServiceImpl implements SysUserService {

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    public Page<SysUserVo> selectPageUserList(SysUserBo user, PageQuery pageQuery) {
        return buildQueryChain(user)
                .select(SysUser.class)
                .leftJoin(SysUser::getDeptId, SysDept::getDeptId)
                .returnType(SysUserVo.class)
                .paging(pageQuery.build());
    }

    @Override
    public void checkUserDataScope(Long userId) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }

        // 获取目标用户的部门信息
        SysUser targetUser = sysUserMapper.getById(userId);
        if (targetUser == null) {
            throw new ServiceException("用户不存在！");
        }

        // 检查是否有权限访问
        if (!hasUserDataScope(targetUser.getDeptId())) {
            throw new ServiceException("没有权限访问用户数据！");
        }
    }
}
```

### 3. 自定义数据权限查询

```java
@Component("sysDataScopeService")
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    @Override
    public List<Long> getRoleCustom(Long roleId) {
        // 查询角色的自定义部门权限
        return sysRoleDeptMapper.selectDeptIdsByRoleId(roleId);
    }

    @Override
    public List<Long> getDeptAndChild(Long deptId) {
        // 递归查询所有子部门
        List<Long> deptIds = new ArrayList<>();
        deptIds.add(deptId);
        deptIds.addAll(findChildDeptIds(deptId));
        return deptIds;
    }
}
```

## 表达式说明

数据权限使用自定义表达式解析器来动态生成SQL条件，完全兼容Solon框架。

### 内置变量

- `#user`: 当前登录用户对象
- `#sdss`: 系统数据权限服务
- 自定义变量：通过@DataColumn注解定义

### 表达式语法

- `#{variable}`: 变量替换
- `#{@bean.method(args)}`: Bean方法调用
- 支持字符串：`"string"`
- 支持数字：`123`
- 支持布尔值：`true/false`

### 表达式示例

```java
// 全部数据权限
ALL("1", "", "")

// 自定数据权限
CUSTOM("2", " #{#deptName} IN ( #{@sdss.getRoleCustom( #user.roleId )} ) ", " 1 = 0 ")

// 部门数据权限
DEPT("3", " #{#deptName} = #{#user.deptId} ", " 1 = 0 ")

// 部门及以下数据权限
DEPT_AND_CHILD("4", " #{#deptName} IN ( #{@sdss.getDeptAndChild( #user.deptId )} )", " 1 = 0 ")

// 仅本人数据权限
SELF("5", " #{#userName} = #{#user.userId} ", " 1 = 0 ")
```

## 配置说明

### 1. 拦截器配置

数据权限功能通过以下拦截器自动处理：

```java
@Configuration
public class XbatisConfig {

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // 添加Mybatis数据权限拦截器
            configuration.addInterceptor(new MybatisDataPermissionInterceptor());
            // 添加XBatis方法拦截器
            XbatisGlobalConfig.addMapperMethodInterceptor(new XbatisDataPermissionMethodInterceptor());
        };
    }
}
```

### 2. 用户权限配置

在登录用户信息中需要设置`dataScope`字段：

```java
public class LoginUser {
    private String dataScope; // 数据权限范围
    private Long roleId;      // 当前角色ID
    private Long deptId;      // 部门ID
}
```

## 注意事项

1. **循环调用问题**：在数据权限服务中避免调用标注了数据权限注解的方法，防止循环调用。

2. **性能考虑**：复杂的权限条件可能影响SQL性能，建议定期优化查询和索引。

3. **调试困难**：表达式解析错误时调试较困难，建议增加详细的日志输出。

4. **测试覆盖**：需要充分的单元测试覆盖各种权限场景。

5. **用户登录信息**：确保用户登录时正确设置dataScope和roleId字段。

## 最佳实践

1. **合理使用数据权限**：只在需要的方法上添加数据权限注解，避免过度使用。

2. **缓存优化**：对于频繁访问的权限数据，考虑使用缓存提高性能。

3. **日志监控**：添加适当的日志记录，便于排查权限问题。

4. **权限验证**：除了自动的数据权限过滤，关键操作还应进行显式的权限验证。

5. **错误处理**：为权限相关的异常提供友好的错误提示。