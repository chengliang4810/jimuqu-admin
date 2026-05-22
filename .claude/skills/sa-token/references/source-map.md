# 源码结构与模块选择

## 顶层模块

根 `pom.xml` 版本为 `1.45.0`，核心模块：

- `sa-token-core`：核心认证、授权、Session、DAO、上下文、路由、注解处理等。
- `sa-token-starter`：各 Web 框架集成适配，包括 Spring Boot、WebFlux、Solon、Servlet/Jakarta、JFinal、Jboot、LoveQQ Boot。
- `sa-token-plugin`：功能插件，包括 Redis/Redisson/Caffeine DAO、JWT、SSO、OAuth2、API Key、接口签名、RPC、模板方言、序列化等。
- `sa-token-dependencies`、`sa-token-special-dependencies`、`sa-token-bom`：依赖与 BOM 管理。
- `sa-token-demo`、`sa-token-test`、`sa-token-doc`：示例、测试和文档。

## 核心源码入口

- `sa-token-core/src/main/java/cn/dev33/satoken/stp/StpUtil.java`
  - 静态门面类，默认账号体系 `TYPE = "login"`。
  - 内部持有 `public static StpLogic stpLogic = new StpLogic(TYPE)`。
  - 大多数 API 转发到 `StpLogic`。
- `sa-token-core/src/main/java/cn/dev33/satoken/stp/StpLogic.java`
  - 核心逻辑实现：登录、Token 读写、Session、权限、踢人、顶人、二级认证、多端登录等。
  - 构造时通过 `loginType` 区分账号体系。
- `sa-token-core/src/main/java/cn/dev33/satoken/SaManager.java`
  - 全局组件管理：`SaTokenConfig`、`SaTokenDao`、`StpInterface`、`SaTokenContext`、JSON/HTTP/Serializer/Temp/Same 等。
  - 默认懒加载配置、DAO、上下文、权限接口。
- `sa-token-core/src/main/java/cn/dev33/satoken/config/SaTokenConfig.java`
  - 核心配置模型：`tokenName`、`timeout`、`activeTimeout`、`isConcurrent`、`isShare`、`tokenStyle`、`isReadHeader`、`isReadCookie`、`tokenPrefix`、`jwtSecretKey` 等。
- `sa-token-core/src/main/java/cn/dev33/satoken/stp/StpInterface.java`
  - 权限/角色数据源接口，业务侧实现 `getPermissionList`、`getRoleList`。
- `sa-token-core/src/main/java/cn/dev33/satoken/dao/SaTokenDao.java`
  - 持久层接口，负责 String/Object/SaSession 的读写、过期时间与搜索。
- `sa-token-core/src/main/java/cn/dev33/satoken/router/SaRouter.java`
  - 路由匹配工具，常配合全局拦截器/过滤器做路径鉴权。

## Starter 选择

### Spring Boot MVC

- Spring Boot 2：`sa-token-spring-boot-starter`
- Spring Boot 3/4：`sa-token-spring-boot3-starter`、`sa-token-spring-boot4-starter`
- 关键类：
  - `cn.dev33.satoken.interceptor.SaInterceptor`
  - `cn.dev33.satoken.filter.SaServletFilter`
  - `cn.dev33.satoken.spring.SaTokenContextForSpring`

### WebFlux / Reactor

- `sa-token-reactor-spring-boot-starter`
- `sa-token-reactor-spring-boot3-starter`
- `sa-token-reactor-spring-boot4-starter`
- 关键类：`SaReactorFilter`、`SaTokenContextForSpringReactor`

### Solon

- `sa-token-solon-plugin`
- 关键类：
  - `cn.dev33.satoken.solon.SaSolonPlugin`
  - `cn.dev33.satoken.solon.SaBeanRegister`
  - `cn.dev33.satoken.solon.integration.SaTokenFilter`
  - `cn.dev33.satoken.solon.model.SaContextForSolon`

## 插件模块选择

- Redis DAO：`sa-token-redis-template`、`sa-token-redis-template-jdk-serializer`、`sa-token-redisson`、`sa-token-redisson-spring-boot-starter`、`sa-token-redisx`。
- 本地缓存 DAO：`sa-token-caffeine`、`sa-token-hutool-timed-cache`。
- JWT：`sa-token-jwt`，提供 Simple、Mixin、Stateless 三种 `StpLogicJwt` 模式。
- 临时 JWT：`sa-token-temp-jwt`。
- SSO：`sa-token-sso`。
- OAuth2：`sa-token-oauth2`。
- API Key：`sa-token-apikey`。
- 接口签名：`sa-token-sign`。
- RPC：`sa-token-dubbo`、`sa-token-dubbo3`、`sa-token-grpc`。
- 序列化：`sa-token-jackson`、`sa-token-fastjson`、`sa-token-snack3/4` 等。

## 文档入口

- 登录认证：`sa-token-doc/use/login-auth.md`
- 权限认证：`sa-token-doc/use/jur-auth.md`
- 注解鉴权：`sa-token-doc/use/at-check.md`
- 路由拦截：`sa-token-doc/use/route-check.md`
- 配置：`sa-token-doc/use/config.md`
- DAO 扩展：`sa-token-doc/use/dao-extend.md`、`sa-token-doc/api/sa-token-dao.md`
- 多账号：`sa-token-doc/up/many-account.md`
- Solon 集成：`sa-token-doc/start/solon-example.md`
- WebFlux 集成：`sa-token-doc/start/webflux-example.md`
- JWT：`sa-token-doc/plugin/jwt-extend.md`
- API Key：`sa-token-doc/plugin/api-key.md`
- 接口签名：`sa-token-doc/plugin/api-sign.md`
- SSO：`sa-token-doc/sso/readme.md`
- OAuth2：`sa-token-doc/oauth2/readme.md`
