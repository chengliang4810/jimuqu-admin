# 配置、DAO、缓存与上下文

## SaManager 全局组件

源码：`sa-token-core/src/main/java/cn/dev33/satoken/SaManager.java`

`SaManager` 管理 Sa-Token 的全局组件：

- `SaTokenConfig`：全局配置。
- `SaTokenDao`：持久层。
- `StpInterface`：权限/角色数据源。
- `SaTokenContext`：请求上下文适配。
- `SaTempTemplate`：临时 Token。
- `SaJsonTemplate`、`SaHttpTemplate`、`SaSerializerTemplate`。
- `SaSameTemplate`、`SaTotpTemplate` 等。

默认行为：

- 配置为空时用 `SaTokenConfigFactory.createConfig()` 懒加载。
- DAO 为空时用 `SaTokenDaoDefaultImpl`。
- `StpInterface` 为空时用 `StpInterfaceDefaultImpl`。
- 上下文为空时用 `SaTokenContextForThreadLocal`。

## 配置方式

常见 YAML：

```yaml
sa-token:
  token-name: satoken
  timeout: 2592000
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: true
```

核心配置项见 `SaTokenConfig`：

- Token：`tokenName`、`timeout`、`activeTimeout`、`tokenStyle`、`tokenPrefix`。
- 读取/写入：`isReadBody`、`isReadHeader`、`isReadCookie`、`isWriteHeader`。
- 并发登录：`isConcurrent`、`isShare`、`maxLoginCount`、`overflowLogoutMode`。
- Cookie：`cookie.domain`、`path`、`secure`、`httpOnly`、`sameSite`。
- JWT：`jwtSecretKey`。
- Same-Token：`sameTokenTimeout`、`checkSameToken`。
- 日志：`isLog`、`logLevel`、`isColorLog`。

代码配置：

```java
SaTokenConfig config = new SaTokenConfig();
config.setTokenName("satoken");
config.setTimeout(30 * 24 * 60 * 60);
config.setIsConcurrent(true);
SaManager.setConfig(config);
```

在具体框架中更推荐按框架 Bean 机制注入配置，避免绕开自动装配。

## SaTokenDao

源码：`sa-token-core/src/main/java/cn/dev33/satoken/dao/SaTokenDao.java`

职责：

- String 读写：`get`、`set`、`update`、`delete`、`getTimeout`、`updateTimeout`。
- Object 读写：`getObject`、`setObject`、`updateObject`、`deleteObject`。
- `SaSession` 读写。
- `searchData` 会话搜索。
- 生命周期：`init()`、`destroy()`。

过期常量：

- `NEVER_EXPIRE = -1`：永不过期。
- `NOT_VALUE_EXPIRE = -2`：不存在。

## DAO 模块选择

默认 DAO 为内存实现，只适合单机和开发测试。生产或多实例部署通常需要 Redis/Redisson 等共享存储。

可选插件：

- `sa-token-redis-template`：Spring RedisTemplate。
- `sa-token-redis-template-jdk-serializer`：RedisTemplate + JDK 序列化。
- `sa-token-redisson`、`sa-token-redisson-spring-boot-starter`：Redisson。
- `sa-token-redisx`：Redisx。
- `sa-token-caffeine`：本地 Caffeine。
- `sa-token-hutool-timed-cache`：Hutool 定时缓存。
- `sa-token-alone-redis`：独立 Redis，将权限缓存与业务缓存分离。

## 上下文 SaTokenContext

`SaTokenContext` 是 Sa-Token 读取请求、响应、存储、Cookie 的抽象。不同框架必须有对应适配：

- Spring MVC：`SaTokenContextForSpring`
- Spring Boot 3/4 Jakarta：`SaTokenContextForSpringInJakartaServlet`
- WebFlux：`SaTokenContextForSpringReactor`
- Solon：`SaContextForSolon`
- 默认/测试：`SaTokenContextForThreadLocal`

Token 读取失败、响应头/Cookie 写入失败、异步线程取不到上下文时，优先查当前框架的 `SaTokenContext` 是否注册和传播正确。

## 权限缓存

`StpInterface` 文档明确：框架默认不对权限/角色数据做缓存。如果权限来自数据库：

- 需要业务侧自行缓存权限列表。
- 权限变更后要考虑清理缓存。
- 不要误以为 `SaTokenDao` 会自动缓存 `StpInterface` 查询结果。

## 常见坑点

- 多实例仍用默认内存 DAO：登录状态、踢人、Session 不共享。
- Redis 序列化模块选错：Session/Object 反序列化失败。
- `dataRefreshPeriod=-1` 会关闭默认 DAO 的过期清理，谨慎使用。
- 自定义 `SaTokenDao` 要正确处理 timeout：`0` 或小于等于 `-2` 通常表示不存储。
- 框架上下文缺失会导致 `SaHolder.getRequest()`、`StpUtil.getTokenValue()` 异常或取不到 Token。
