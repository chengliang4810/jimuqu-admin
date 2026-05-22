# solon-web

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-web`

用途：Web 层扩展集合，覆盖 SessionState、CORS、SSE、静态资源、服务停止、版本过滤、Servlet/Vert.x/WebDAV/WebServices 桥接等能力。

## 直接子模块

- `solon-sessionstate-local` — 本地 SessionState。插件 `org.noear.solon.sessionstate.local.integration.SessionstateLocalPlugin`。
- `solon-sessionstate-jwt` — JWT SessionState。插件 `org.noear.solon.sessionstate.jwt.integration.SessionstateJwtPlugin`。
- `solon-sessionstate-jedis` — Redis/Jedis SessionState。插件 `org.noear.solon.sessionstate.jedis.integration.SessionstateJedisPlugin`。
- `solon-sessionstate-redisson` — Redisson SessionState。插件 `org.noear.solon.sessionstate.redisson.integration.SessionstateRedissonPlugin`。
- `solon-web-cors` — CORS 支持，核心注解 `org.noear.solon.web.cors.annotation.CrossOrigin`。
- `solon-web-sdl` — Web SDL 工具模块。
- `solon-web-servlet` — Servlet 环境适配/桥接。
- `solon-web-sse` — SSE 返回与渲染支持。插件 `org.noear.solon.web.sse.integration.WebSsePlugin`。
- `solon-web-staticfiles` — 静态资源支持。插件 `org.noear.solon.web.staticfiles.integration.WebStaticfilesPlugin`。
- `solon-web-stop` — Web 停止服务能力。插件 `org.noear.solon.web.stop.integration.WebStopPlugin`。
- `solon-web-version` — Web 版本过滤/处理能力。
- `solon-web-vertx` — Web 与 Vert.x 桥接能力。
- `solon-web-webdav` — WebDAV 支持。
- `solon-web-webservices` — WebServices 支持。插件 `org.noear.solon.web.webservices.integration.WebServicePlugin`。

## 关键源码与配置

- `solon-web-cors/src/main/java/org/noear/solon/web/cors/annotation/CrossOrigin.java`：`origins` 默认 `*`，`maxAge` 默认 `3600`，`credentials` 默认 `true`。
- `solon-web-staticfiles/src/main/java/org/noear/solon/web/staticfiles/StaticConfig.java`：配置 `solon.staticfiles.enable`、`solon.staticfiles.cacheMaxAge`、`solon.staticfiles.mappings`，兼容旧 `solon.staticfiles.maxAge`。
- 静态资源默认位置：`static/`、`WEB-INF/static/`。
- `solon-sessionstate-jwt/src/main/java/org/noear/solon/sessionstate/jwt/JwtSessionProps.java`：配置前缀 `server.session.state.jwt`。
- JWT Session 配置：`name`、`secret`、`prefix`、`allowExpire`、`allowAutoIssue`、`allowUseHeader`。
- `solon-sessionstate-jedis/src/main/java/org/noear/solon/sessionstate/jedis/JedisSessionStateFactory.java`：实际读取 `server.session.state.redis`。
- `solon-web-sse/src/main/java/org/noear/solon/web/sse/integration/WebSsePlugin.java`：注册 SSE render 和 return handler。

## 依赖选择与坑点

- SessionState 按存储方式选一个：local、JWT、Jedis、Redisson。
- `solon-sessionstate-jwt` 必须显式配置 `secret`。
- Jedis Session 实际读取 `server.session.state.redis`，不要被旧错误提示误导。
- `solon-web-staticfiles` 默认启用；debug 模式缓存为 0，生产默认 600 秒。
- CORS 用 Solon 的 `@CrossOrigin`，不要套用 Spring 的注解。
- SSE 源码里渲染 key 需以源码为准，不要凭语义臆造。
