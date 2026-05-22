# solon-net

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-net`

用途：WebSocket/Socket.D 路由与 HTTP 工具集合。

## 直接子模块

- `solon-net` — `@ServerEndpoint`、WebSocketRouter、SocketdRouter 基础能力。插件 `org.noear.solon.net.integration.NetPlugin`。
- `solon-net-httputils` — HTTP 客户端工具，插件 `org.noear.solon.net.http.integration.NetHttpPlugin`。
- `solon-net-stomp` — STOMP broker/client 支持。

## 关键源码与 API

- `solon-net/src/main/java/org/noear/solon/net/annotation/ServerEndpoint.java`：`@ServerEndpoint(value="**")` 标注类型。
- `solon-net/src/main/java/org/noear/solon/net/integration/NetPlugin.java`：支持 Bean 类型 `WebSocketListenerSupplier`、`WebSocketListener` 或 Socket.D `Listener`。

## 依赖选择与坑点

- 只加 `@ServerEndpoint` 不够，类还必须实现源码支持的 Listener/Supplier 类型。
- `solon-net-stomp` 是协议能力模块，不等同自动装配插件。
- HTTP 客户端工具用 `solon-net-httputils`。
