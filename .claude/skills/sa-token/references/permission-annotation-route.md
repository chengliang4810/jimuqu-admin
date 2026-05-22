# 权限、角色、注解与路由拦截

## 权限数据源：StpInterface

权限认证前必须让 Sa-Token 知道账号拥有哪些权限和角色。业务侧实现：

```java
public class StpInterfaceImpl implements StpInterface {
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of("user.add", "user.update", "art.*");
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of("admin", "super-admin");
    }
}
```

要点：

- `loginId` 是调用 `StpUtil.login(id)` 时写入的账号 id。
- `loginType` 是账号体系标识，多账号时必须据此区分。
- `StpInterface` 方法不是启动时执行，而是在调用鉴权 API 时执行。
- 框架默认不缓存权限/角色数据；如果来自数据库，是否缓存由业务侧决定。

## 权限 API

```java
List<String> permissions = StpUtil.getPermissionList();
boolean ok = StpUtil.hasPermission("user.add");
StpUtil.checkPermission("user.add");
StpUtil.checkPermissionAnd("user.add", "user.delete");
StpUtil.checkPermissionOr("user.add", "user.delete");
```

无权限抛 `NotPermissionException`。

支持通配符：

- `art.*` 匹配 `art.add`、`art.delete`。
- `*.delete` 匹配 `user.delete`、`art.delete`。
- `*` 是上帝权限，权限和角色同理。

## 角色 API

```java
List<String> roles = StpUtil.getRoleList();
boolean ok = StpUtil.hasRole("admin");
StpUtil.checkRole("admin");
StpUtil.checkRoleAnd("admin", "ops");
StpUtil.checkRoleOr("admin", "super-admin");
```

无角色抛 `NotRoleException`。

## 注解鉴权

常见注解：

```java
@SaCheckLogin
@SaCheckPermission("user:add")
@SaCheckRole("admin")
@SaCheckOr
@SaCheckSafe
@SaCheckDisable
@SaIgnore
```

多账号体系要指定 `type`：

```java
@SaCheckLogin(type = "user")
@SaCheckPermission(value = "article:add", type = "admin")
```

注意：注解是否生效取决于对应框架的注解处理/拦截器是否启用。Spring MVC 的 `SaInterceptor` 默认 `isAnnotation=true`；Solon 的 `SaTokenFilter` 也有 `isAnnotation` 开关。

## 路由拦截鉴权

`SaRouter` 通常在全局拦截器/过滤器里使用。

基础写法：

```java
SaRouter
    .match("/**")
    .notMatch("/user/doLogin")
    .check(r -> StpUtil.checkLogin());
```

按模块鉴权：

```java
SaRouter.match("/user/**", r -> StpUtil.checkPermission("user"));
SaRouter.match("/admin/**", r -> StpUtil.checkRoleOr("admin", "super-admin"));
```

可匹配内容：

- path：`match("/user/**")`、`notMatch("*.js")`
- HTTP 方法：`match(SaHttpMethod.GET)`、`matchMethod("POST")`
- boolean 条件：`match(StpUtil.isLogin())`
- lambda 条件：`match(r -> StpUtil.isLogin())`

控制流：

- `stop()`：停止匹配，继续进入 Controller。
- `back(value)`：停止匹配并直接返回结果。
- `free(...)`：打开独立作用域，让内部 `stop()` 只跳出当前作用域。

## Spring MVC：SaInterceptor 与 SaServletFilter

### SaInterceptor

源码：`sa-token-starter/sa-token-spring-boot-starter/src/main/java/cn/dev33/satoken/interceptor/SaInterceptor.java`

特点：

- 实现 Spring `HandlerInterceptor`。
- 提供注解鉴权和路由拦截鉴权。
- `preHandle` 中顺序：`beforeAuth` → 注解鉴权 → `auth`。
- 捕获 `StopMatchException` 进入 Controller；捕获 `BackResultException` 直接写响应。

示例：

```java
registry.addInterceptor(new SaInterceptor(handler -> {
    SaRouter.match("/**").notMatch("/user/doLogin").check(r -> StpUtil.checkLogin());
})).addPathPatterns("/**");
```

### SaServletFilter

源码：`sa-token-starter/sa-token-spring-boot-starter/src/main/java/cn/dev33/satoken/filter/SaServletFilter.java`

特点：

- Servlet Filter，默认优先级为 `SaTokenConsts.ASSEMBLY_ORDER`。
- 有 `includeList`、`excludeList`、`beforeAuth`、`auth`、`error`。
- `beforeAuth` 不受 include/exclude 限制，所有请求都会进入。

## Solon：SaTokenFilter

源码：`sa-token-starter/sa-token-solon-plugin/src/main/java/cn/dev33/satoken/solon/integration/SaTokenFilter.java`

特点：

- 实现 Solon `Filter`。
- 支持路由鉴权和注解鉴权。
- 注释明确：`SaTokenInterceptor` 和 `SaTokenFilter` 二选一，不要同时用。
- 会查找当前主处理器和网关目标，再执行 `beforeAuth`、注解处理、规则处理。

## 常见坑点

- 只写了注解但没有注册对应拦截器/过滤器，注解可能不生效。
- `@SaIgnore` 会影响注解/路由校验，排查“为什么放行”时要检查方法和类上是否存在。
- 前端按钮权限只是展示控制，后端接口必须再用 `checkPermission`、注解或路由拦截鉴权。
- `StpInterface` 返回空列表会导致权限/角色校验失败，不是框架扫描失败。
