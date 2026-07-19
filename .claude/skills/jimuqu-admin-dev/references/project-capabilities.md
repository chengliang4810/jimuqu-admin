# 项目能力与模块复用

开发前先查项目已有公共能力，避免重复实现。

## 权限与认证

- 后台接口权限使用 Sa-Token 的 `@SaCheckPermission`。
- 登录用户、用户 id、超级管理员判断优先使用 `LoginHelper`。
- 登录客户端、授权类型和会话策略优先复用 `SysClient` 与现有认证策略。

## 操作日志与审计

- Controller 变更类接口使用 `@Log(title = "...", businessType = BusinessType.X)`。
- 项目操作日志横切由 `jimuqu-common-log` 的 `LogAspect` 处理，它基于 Solon `RouterInterceptor` 读取 `ctx.action()`、`@Log`、请求参数、响应和耗时后发布 `OperLogEvent`。
- 新增“审计明细查询”这类需求时，应围绕已有日志落库模型设计查询，不要重复拦截同一份 Controller 数据。

## 限流

- 优先使用 `jimuqu-common-ratelimit` 的 `@RateLimit`。
- `RateLimitFilter` 会从 `ctx.action()` 获取 Controller 方法和注解，支持 IP、USER、GLOBAL 类型。
- 接口级限流优先用注解；跨接口统一策略再考虑扩展 Filter/Interceptor。

## SSE 与消息推送

- 系统推送优先复用 `jimuqu-common-sse`：`SseEmitterManager`、`SseMessageUtil`、`SseController`。
- 不要自己维护第二套用户连接 Map。
- 给指定用户/全员推送前，先确认登录态、token、用户 id 和连接关闭逻辑。

## 文件上传与文件记录

- 文件或图片保存必须使用 `x-file-storage`：`FileStorageService` + Solon `UploadedFile`。
- 参考 `docs/文件上传示例.md` 和 `SysFileController`。
- 需要关联业务对象时设置 objectId/objectType 或在业务表保存文件 id/url；不要直接写固定本地路径。

## Excel 与导出

- 导出前先查 `jimuqu-common-excel`、项目现有导出工具和同类导出接口。
- 大数据导出优先使用项目已有大文件/异步导出能力，避免 Controller 同步长时间阻塞。
- 导出文件如需留存，应纳入文件记录体系。

## 数据权限

- 涉及部门、角色、用户隔离时，先查 `@DataPermission`、数据权限文档和同类 Service。
- 非超级管理员只能访问自己/授权范围内数据这类规则应在 Service 查询链或项目数据权限机制中落实，不只在 Controller 判断。

## 横切机制选择

项目级规则决定“复用哪个模块”，Solon 机制细节用 `solon` 查证：

- 原始 HTTP 链路：Filter。
- 依赖 Controller 方法注解、路由信息、返回结果：RouterInterceptor。
- Service/Bean 方法级增强：`@Around` + MethodInterceptor。
- 框架插件化注册：Solon Plugin + `META-INF/solon/*.properties`。

不要使用 Spring `HandlerInterceptor`、`OncePerRequestFilter`、Spring AOP 或 `spring.factories`。
