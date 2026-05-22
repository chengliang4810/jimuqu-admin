# 排查与安全注意

## 未登录 / Token 读不到

优先检查：

1. 前端是否提交 Token。
2. 提交名是否等于 `token-name`，默认 `satoken`。
3. 提交位置是否被允许：`isReadHeader`、`isReadCookie`、`isReadBody`。
4. 配置了 `tokenPrefix` 时，提交格式是否包含前缀。
5. Cookie 场景是否受跨域、SameSite、domain、path、secure、httpOnly 影响。
6. 后端是否用了正确框架上下文：Spring/Solon/WebFlux 适配是否注册。
7. 查看 `NotLoginException.getType()`，区分 `NOT_TOKEN`、`INVALID_TOKEN`、`TOKEN_TIMEOUT`、`BE_REPLACED`、`KICK_OUT`、`TOKEN_FREEZE`、`NO_PREFIX`。

## 注解不生效

检查：

1. 是否引入正确 starter/plugin。
2. 是否注册了对应拦截器/过滤器。
3. 注解鉴权开关是否开启：如 `SaInterceptor.isAnnotation`、`SaTokenFilter.isAnnotation`。
4. Controller 方法或类上是否有 `@SaIgnore`。
5. 多账号体系注解是否指定了正确 `type`。
6. 当前框架是否支持对应注解处理链；不要把 Spring 的注解合并机制套到 Solon。

## 权限/角色总是失败

检查：

1. 是否实现并注册 `StpInterface`。
2. `getPermissionList` / `getRoleList` 是否在调用鉴权时执行。
3. 返回权限码是否与 `checkPermission` 完全一致，通配符是否符合预期。
4. 多账号时是否根据 `loginType` 返回不同数据。
5. 权限缓存是否过期或未刷新。
6. 是否误以为前端按钮权限能替代后端鉴权。

## Redis / DAO 问题

检查：

1. 生产多实例是否仍使用默认内存 DAO。
2. 是否引入正确 DAO 插件：RedisTemplate、Redisson、Redisx、Caffeine 等。
3. 序列化方式是否与已有数据兼容。
4. 自定义 `SaTokenDao` 是否正确处理 timeout 与 `SaSession`。
5. 踢人、注销、Session 查询是否跨实例共享。
6. 独立 Redis 场景是否用了 alone-redis 模块，而不是混用业务 Redis 配置。

## 前后端分离

建议：

- 明确 Token 传输方式：Header 优先，Cookie 需处理跨域和 SameSite。
- 登录后如需前端保存 Token，可开启 `isWriteHeader` 或手动返回 `StpUtil.getTokenValue()`。
- 后端仍必须 `checkLogin` / `checkPermission`。
- 不要把 Token、JWT、API Key 放在 URL 中长期传播，避免日志和 Referer 泄露。

## JWT 排查

检查：

1. 是否引入 `sa-token-jwt`。
2. 是否配置 `jwt-secret-key`。
3. 是否注入了三种 `StpLogicJwt` 之一。
4. 多账号体系是否给自定义 `StpLogic` 单独注入 JWT 实现。
5. 当前模式是否支持所需能力：Stateless 不支持服务端踢人、顶人、完整 Session 等。
6. Hutool JWT 版本兼容性，文档提示 `sa-token-jwt` 显式依赖 hutool-jwt 5.7.14，避免冲突版本。

## SSO 排查

检查：

1. 当前架构属于模式一、二、三哪一种。
2. Cookie domain、系统主域名、前端跨域是否匹配。
3. 后端是否共享 Redis。
4. 模式三 HTTP 获取会话时 server/client 地址、secret、ticket 是否正确。
5. 是否配置域名校验，避免 ticket 劫持。
6. 登录成功后原 URL 参数是否完整保留。
7. 单点注销消息推送链路是否打通。

## OAuth2 排查

检查：

1. grant_type 是否符合使用场景。
2. clientId/clientSecret 是否能通过 `SaOAuth2DataLoader` 加载。
3. redirect_uri/domain/scope 是否通过服务端校验。
4. code/access_token/refresh_token 是否过期或已使用。
5. scope 是否在资源接口二次校验。

## API Key 排查

检查：

1. 请求是否按 `apikey` 参数或请求头提交。
2. API Key 是否存在、有效、未过期、未撤销。
3. scope 是否满足注解或工具校验。
4. 默认缓存模式是否因重启丢失数据；需要持久化时实现 `SaApiKeyDataLoader`。

## 接口签名排查

检查：

1. 客户端和服务端参数排序是否一致。
2. secretKey 是否一致且未明文传输。
3. `nonce` 是否唯一且已缓存防重放。
4. `timestamp` 是否在允许窗口内，双方服务器时间是否偏差过大。
5. sign 参数自身是否被排除在签名原文外。

## 安全底线

- 前端权限只做 UI 展示，后端必须鉴权。
- 不要把 secret、jwt key、API Key 默认值提交到仓库。
- Token 泄露后应支持注销、踢人、撤销 API Key 或更换秘钥。
- 使用 `*` 上帝权限要非常谨慎。
- `timeout=-1` 永不过期只适合极少数可信场景。
