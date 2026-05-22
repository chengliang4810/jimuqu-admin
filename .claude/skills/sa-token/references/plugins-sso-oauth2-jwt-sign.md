# 插件：SSO、OAuth2、API Key、JWT、接口签名

## JWT

依赖：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
    <version>1.45.0</version>
</dependency>
```

配置：

```yaml
sa-token:
  jwt-secret-key: your-secret-key
```

三种模式：

- `StpLogicJwtForSimple`：Simple 简单模式，Token 风格替换为 JWT，登录数据仍可存 Redis。
- `StpLogicJwtForMixin`：Mixin 混入模式，部分数据进 Token，Session 等仍依赖服务端。
- `StpLogicJwtForStateless`：Stateless 无状态模式，完全舍弃 Redis，只用 JWT。

Spring 示例：

```java
@Bean
public StpLogic getStpLogicJwt() {
    return new StpLogicJwtForSimple();
}
```

多账号 JWT 需要手动给自定义 `StpLogic` 注入对应 JWT 实现：

```java
StpUserUtil.setStpLogic(new StpLogicJwtForSimple(StpUserUtil.TYPE));
```

注意：Stateless 模式下踢人、顶人、Session、active-timeout、id 反查 Token 等能力受限。

## API Key

依赖：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-apikey</artifactId>
    <version>1.45.0</version>
</dependency>
```

用途：第三方插件或外部调用按 scope 最小化授权，避免把用户账号密码或完整会话 Token 交给第三方。

创建与保存：

```java
ApiKeyModel akModel = SaApiKeyUtil.createApiKeyModel(10001).setTitle("test");
akModel.addScope("userinfo", "chat");
SaApiKeyUtil.saveApiKey(akModel);
```

校验：

```java
SaApiKeyUtil.checkApiKey(apiKey);
SaApiKeyUtil.checkApiKeyScope(apiKey, "userinfo");
SaApiKeyUtil.checkApiKeyLoginId(apiKey, 10001);
```

注解：

```java
@SaCheckApiKey
@SaCheckApiKey(scope = "userinfo")
@SaCheckApiKey(scope = {"userinfo", "chat"}, mode = SaMode.OR)
```

默认获取方式：请求参数或请求头名 `apikey`，也支持 Basic 参数提交。

默认缓存模式重启会丢数据；如需数据库模式，实现 `SaApiKeyDataLoader`。

## 接口签名 sa-token-sign

依赖：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-sign</artifactId>
    <version>1.45.0</version>
</dependency>
```

解决问题：

- 请求身份伪造。
- 请求参数篡改。
- 抓包重放攻击。

设计要点：

- 参数按字典序参与签名，secretKey 不直接传输。
- 增加 `nonce` 防止同一请求重复使用。
- 增加 `timestamp` 限制请求有效时间，避免 nonce 永久存储。
- 服务端需要缓存已使用 nonce 或结合时间窗口校验。

常见注解/工具：

```java
@SaCheckSign
SaSignUtil
SaSignTemplate
```

## SSO

依赖：`sa-token-sso`

三种模式：

| 场景 | 模式 | 简介 |
|---|---|---|
| 前端同域 + 后端同 Redis | 模式一 | 共享 Cookie 同步会话 |
| 前端不同域 + 后端同 Redis | 模式二 | URL 重定向传播会话 |
| 前端不同域 + 后端不同 Redis | 模式三 | HTTP 请求获取会话 |

核心模块：

- `SaSsoServerUtil`、`SaSsoClientUtil`
- `SaSsoServerProcessor`、`SaSsoClientProcessor`
- `SaSsoServerConfig`、`SaSsoClientConfig`
- `SaSsoMessage` 消息推送与单点注销相关处理

安全要点：

- 域名校验、ticket 校验、秘钥校验必须配置清楚。
- 模式三链路更复杂，先用官方 demo 打通再接入业务。
- SSO 本质仍组合 Sa-Token 的登录、会话、路由和跨系统通信能力。

## OAuth2

依赖：`sa-token-oauth2`

四种授权模式：

- 授权码：标准模式，server 下发 code，client 用 code 换 access_token。
- 隐藏式：备用选择，通过 URL 重定向直接下放 access_token。
- 密码式：client 拿用户账号密码换 access_token。
- 客户端凭证：client 级别授权，代表应用自身。

关键类：

- `SaOAuth2Manager`
- `SaOAuth2ServerConfig`
- `SaOAuth2Template`、`SaOAuth2Util`
- `SaOAuth2ServerProcessor`
- `SaOAuth2DataLoader`：加载 client、scope 等业务数据。
- `AuthorizationCodeGrantTypeHandler`、`PasswordGrantTypeHandler`、`RefreshTokenGrantTypeHandler`

SSO 与 OAuth2 区分：

- SSO 强调多个互信系统间一次登录、处处访问。
- OAuth2 强调第三方应用授权范围控制，弱化系统间会话同步。

## 插件加载

很多插件包含：

```text
META-INF/satoken/cn.dev33.satoken.plugin.SaTokenPlugin
```

对应 `SaTokenPluginForXxx` 注册插件能力。排查插件不生效时检查：依赖是否引入、插件入口文件是否在 classpath、框架 BeanRegister/BeanInject 是否执行。

## 安全注意

- JWT `jwt-secret-key`、SSO/OAuth2/Sign secret 不要提交仓库或下发前端。
- API Key 要支持有效期、scope、撤销和审计。
- 接口签名不能只做 sign，还要处理 timestamp 与 nonce 防重放。
- OAuth2 scope 必须服务端校验，不能只信前端授权页展示。
