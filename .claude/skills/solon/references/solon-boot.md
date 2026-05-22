# solon-boot

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-boot`

用途：应用启动/server 依赖聚合层，为项目选择内嵌 server、WebSocket、SocketD、JSP 等运行时组合。

## 直接子模块

- `solon-boot` — 默认聚合到 `solon-server`。
- `solon-boot-jdkhttp`、`solon-boot-jetty`、`solon-boot-smarthttp`、`solon-boot-undertow`、`solon-boot-vertx` — 对应 HTTP server 聚合依赖。
- `solon-boot-socketd` — SocketD server 聚合。
- `solon-boot-websocket`、`solon-boot-websocket-netty` — WebSocket server 聚合。
- `solon-boot-jetty-add-jsp`、`solon-boot-undertow-add-jsp` — JSP 附加能力。
- `solon-boot-jetty-add-websocket` — Jetty WebSocket 附加能力。

## 关键入口

- 本层主要是 POM 聚合，通常没有自己的 `META-INF/solon/*.properties`。
- 实际插件入口位于对应 `solon-server-*` 模块。

## 依赖选择与坑点

- 应用一般只选一个 boot/server 组合。
- JSP、WebSocket 是附加模块，不等于完整 HTTP server。
- 避免同时混入多个 server 聚合，除非明确知道插件加载和端口监听行为。
