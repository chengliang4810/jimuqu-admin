# 登录、Token、Session 与多账号

## 登录认证主链路

基础 API 来自 `StpUtil`，其静态方法转发到默认 `StpLogic`。

常见写法：

```java
StpUtil.login(userId);
StpUtil.checkLogin();
boolean login = StpUtil.isLogin();
Object loginId = StpUtil.getLoginId();
String token = StpUtil.getTokenValue();
StpUtil.logout();
```

要点：

- `StpUtil.login(Object userId)` 的参数应是账号唯一标识，建议 `long`、`int`、`String`，不要传复杂对象。
- 默认 Cookie 模式下，`login` 会把 Token 写入响应 Cookie；前后端分离项目常通过 Header 返回/提交 Token，可配置 `isWriteHeader`、`isReadHeader`、`token-name`。
- `StpUtil.checkLogin()` 未登录时抛 `NotLoginException`。
- `StpUtil.getLoginId()` 未登录时抛异常；若需要未登录返回 null，用 `getLoginIdDefaultNull()`。

## Token 配置关键项

源码：`SaTokenConfig`

- `tokenName`：Token 名称，同时也是 Cookie 名称、提交参数名、存储 key 前缀，默认 `satoken`。
- `timeout`：Token 有效期，单位秒，默认 30 天，`-1` 永久有效。
- `activeTimeout`：最低活跃频率，超时会冻结，默认 `-1` 不限制。
- `dynamicActiveTimeout`：动态 activeTimeout，默认 false。
- `isConcurrent`：是否允许同账号多地同时登录，默认 true。
- `isShare`：同账号多人登录是否共用一个 Token，默认 false。
- `maxLoginCount`：同账号最大登录数量，仅在 `isConcurrent=true` 且 `isShare=false` 时有意义。
- `isReadBody`、`isReadHeader`、`isReadCookie`：是否从请求体、Header、Cookie 读取 Token。
- `isWriteHeader`：登录后是否把 Token 写入响应头，默认 false。
- `tokenPrefix`：Token 前缀，例如 `Bearer`；提交格式为 `satoken: Bearer xxxx`。
- `cookieAutoFillPrefix`：Cookie 模式是否自动填充 Token 前缀。
- `autoRenew`：是否自动续签 activeTimeout，默认 true。

## NotLoginException 场景值

`NotLoginException` 可区分未登录原因：

- `NOT_TOKEN`：未读取到有效 Token。
- `INVALID_TOKEN`：Token 无效。
- `TOKEN_TIMEOUT`：Token 已过期。
- `BE_REPLACED`：Token 已被顶下线。
- `KICK_OUT`：Token 已被踢下线。
- `TOKEN_FREEZE`：Token 已被冻结。
- `NO_PREFIX`：未按配置前缀提交 Token。

排查未登录问题时不要只返回“未登录”，应看 `nle.getType()`。

## Session

Sa-Token 的 Session 常见概念：

- Account-Session：账号维度 Session。
- Token-Session：Token 维度 Session。
- Custom-Session：自定义 Session。

常见 API：

```java
SaSession session = StpUtil.getSession();
session.set("name", "zhang");
Object name = session.get("name");
```

配置项：

- `rightNowCreateTokenSession`：登录时是否立即创建 Token-Session。
- `tokenSessionCheckLogin`：获取 Token-Session 时是否必须登录。
- `isLogoutKeepTokenSession`：注销 Token 后是否保留 Token-Session。

## 踢人、顶人、注销

常见 API：

```java
StpUtil.logout();
StpUtil.kickout(loginId);
StpUtil.replaced(loginId);
StpUtil.logoutByTokenValue(tokenValue);
StpUtil.kickoutByTokenValue(tokenValue);
```

配置影响：

- `logoutRange`：`StpUtil.logout()` 只注销当前 Token，还是注销当前账号所有客户端会话。
- `isConcurrent=false` 时新登录会触发顶人策略。
- `replacedLoginExitMode` 决定旧设备下线还是新设备登录失败。
- `replacedRange` 决定顶人范围是当前设备类型还是全部设备类型。

## 多账号体系

`StpUtil` 默认账号体系为 `TYPE = "login"`。多账号通过不同 `loginType` 的 `StpLogic` 隔离。

推荐理解：

```java
public class StpKit {
    public static final StpLogic DEFAULT = StpUtil.stpLogic;
    public static final StpLogic ADMIN = new StpLogic("admin");
    public static final StpLogic USER = new StpLogic("user");
}
```

使用：

```java
StpKit.ADMIN.login(10001);
StpKit.USER.login(10001);
StpKit.ADMIN.checkPermission("article:add");
```

注解鉴权默认针对 `StpUtil` 账号体系；多账号注解要指定 `type`：

```java
@SaCheckLogin(type = "user")
@SaCheckPermission(value = "article:add", type = "admin")
```

## 常见坑点

- 前端不提交 Token：检查 `token-name`、Header 名、Cookie 是否跨域、`isReadHeader/isReadCookie`。
- 配置了 `tokenPrefix`：前端必须按前缀格式提交，否则可能触发 `NO_PREFIX`。
- 前后端分离：仅依赖 Cookie 可能因跨域、SameSite、浏览器策略失效，通常明确用 Header 传 Token。
- 多账号：不要只给 loginId 加字符串前缀凑合，优先用不同 `StpLogic/loginType`。
- JWT stateless 模式下踢人、顶人、会话管理等能力受限，见插件参考。
