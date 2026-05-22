---
name: sa-token
description: 辅助开发、配置、调试和解释 Sa-Token 权限认证框架。只要用户提到 Sa-Token、cn.dev33、StpUtil、StpLogic、SaManager、SaTokenConfig、StpInterface、SaTokenDao、@SaCheckLogin/@SaCheckPermission/@SaIgnore、SaRouter、SaInterceptor、SaServletFilter、SaTokenFilter、登录认证、权限/角色认证、踢人下线、会话、Token、前后端分离、Redis 持久化、多账号体系、SSO、OAuth2、API Key、JWT、接口签名、Solon/Spring Boot/WebFlux 集成或需要根据 /Users/chengliang/code-repositories/sa-token 源码确认 API 与机制，都必须使用此技能。此技能只记录通用 Sa-Token 能力，不写入某个业务项目的专属规范。
---

# Sa-Token 开发技能

你在辅助 Sa-Token 权限认证框架相关开发、配置、迁移和排查。Sa-Token API 面广，集成模块多，回答和实现必须优先以本地源码与仓库文档为准。

默认源码目录：`/Users/chengliang/code-repositories/sa-token`

## 首要原则

1. **先判定任务类型**：登录认证、权限/角色认证、注解鉴权、路由拦截、配置、会话/Token、持久化 DAO、多账号体系、Solon/Spring/WebFlux 集成、SSO、OAuth2、API Key、JWT、接口签名、问题排查或源码机制解释。
2. **优先查证本地源码**：不确定的类、注解、配置键、插件模块、starter 名称、上下文适配器，必须读源码或仓库文档。
3. **不要混用其它安全框架习惯**：不要把 Spring Security、Shiro、JWT 库或 OAuth2 Server 的写法套到 Sa-Token 上；Sa-Token 有自己的 `StpUtil`、`StpLogic`、`SaRouter`、`StpInterface`、`SaTokenDao`、插件体系。
4. **输出要可落地**：给依赖坐标、配置片段、Java 代码、源码依据和验证方式；涉及源码机制时引用 `path:line`。
5. **安全边界**：涉及 Token、JWT、API Key、SSO、OAuth2、签名秘钥、Redis 持久化时，提醒不要泄露秘钥、不要信任前端权限、后端接口必须再次鉴权。
6. **保持通用性**：本技能只记录 Sa-Token 通用能力。业务项目专属 Controller、响应包装、权限码命名、数据库结构等放到对应项目技能中。

## 资料入口

按任务读取对应 reference，必要时再读源码文件：

- 仓库结构、模块选择、源码入口：`references/source-map.md`
- 登录、Token、Session、多端/多账号：`references/login-session-token.md`
- 权限、角色、注解、路由拦截：`references/permission-annotation-route.md`
- Spring Boot、Solon、WebFlux 集成：`references/integration-frameworks.md`
- 配置、DAO、Redis/缓存、上下文：`references/config-dao-context.md`
- SSO、OAuth2、API Key、JWT、接口签名：`references/plugins-sso-oauth2-jwt-sign.md`
- 排查与安全注意：`references/debugging-security.md`

如果任务很具体，直接 grep 源码优先于泛读 reference。

## 工作流

### 业务接入或功能开发

1. 确认目标运行框架：Spring Boot 2/3/4、Solon、WebFlux、Servlet/Jakarta、JFinal/Jboot 等。
2. 查当前项目已有 Sa-Token 用法和依赖版本；若当前项目版本与本地源码不同，提醒并以项目版本为准。
3. 选择正确 starter/plugin 与扩展模块：核心鉴权、Redis DAO、JWT、SSO、OAuth2、API Key、Sign 等。
4. 按 Sa-Token 原生 API 写最小实现：登录用 `StpUtil.login` 或对应 `StpLogic`，权限数据实现 `StpInterface`，路由规则用 `SaRouter`，不要引入额外安全框架。
5. 给验证方式：登录、携带 Token 访问、未登录/无权限异常、注销/踢人/过期场景。

### 源码机制解释

1. 定位核心类定义和调用链：`StpUtil` 门面 → `StpLogic` 实现 → `SaManager` 全局组件 → `SaTokenDao`/`SaTokenContext`/`StpInterface`。
2. 阅读具体框架适配：Spring MVC 用 `SaInterceptor`/`SaServletFilter`，Solon 用 `SaTokenFilter`/`SaBeanRegister`/`SaSolonPlugin`，WebFlux 用 Reactor 适配。
3. 输出：结论 → 关键源码路径 → 机制流程 → 使用建议/坑点。

### 问题排查

1. 收集现象：未登录、无权限、Token 读不到、Cookie/Header 不生效、注解不生效、Redis 数据丢失、多账号冲突、SSO/OAuth2 回调异常。
2. 对照配置和源码确认失败点：`token-name`、`is-read-header/body/cookie`、`token-prefix`、`timeout/active-timeout`、`StpInterface` 是否被注册、过滤器/拦截器是否生效、DAO 是否替换成功。
3. 给最小修复和验证步骤，不做无关重构。

## 常用查证命令

```bash
# 核心类
grep -R "class StpUtil\|class StpLogic\|interface StpInterface\|interface SaTokenDao" /Users/chengliang/code-repositories/sa-token/sa-token-core/src/main/java

# 集成适配
grep -R "class SaInterceptor\|class SaServletFilter\|class SaTokenFilter\|class SaSolonPlugin" /Users/chengliang/code-repositories/sa-token/sa-token-starter/src/main/java

# 插件入口
find /Users/chengliang/code-repositories/sa-token/sa-token-plugin -path '*/META-INF/satoken/*' -o -name 'pom.xml'

# 官方文档片段
find /Users/chengliang/code-repositories/sa-token/sa-token-doc -name '*.md' | grep -E 'login-auth|jur-auth|route-check|config|jwt|api-key|api-sign|sso|oauth2'
```

## 输出规范

- 全程中文；Java 类名、配置键和代码标识保持原样。
- 涉及源码机制时引用 `path:line`。
- 不确定时说“源码中未确认”，不要补 Spring Security/Shiro/OAuth2 Server 的 API。
- 框架示例按用户指定技术栈输出；用户未指定时先问是 Spring Boot、Solon 还是 WebFlux。
- 安全相关默认提醒后端必须鉴权、秘钥不要硬编码到前端或仓库、前端权限只做展示控制。
