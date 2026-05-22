# solon-server

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-server`

用途：Solon 底层 server 运行时实现集合，覆盖 HTTP server、WebSocket、SocketD、JSP add-on 与通用 server 配置。

## 直接子模块

- `solon-server` — Server 基础抽象与配置常量。
- `solon-server-jdkhttp` — JDK HTTP server，插件 `org.noear.solon.server.jdkhttp.integration.JdkHttpPlugin`。
- `solon-server-smarthttp` — SmartHttp server，插件 `org.noear.solon.server.smarthttp.integration.SmHttpPlugin`。
- `solon-server-netahttp` — NetaHttp server，插件 `org.noear.solon.server.netahttp.integration.NetaHttpPlugin`。
- `solon-server-jetty`、`tomcat`、`undertow`、`grizzly`、`vertx` — 对应容器/server 实现。
- `*-add-jsp` — 对应 server 的 JSP 扩展。
- `*-add-websocket` — 对应 server 的 WebSocket 扩展。
- `solon-server-websocket`、`solon-server-websocket-netty` — 独立 Java/Netty WebSocket 支持。
- `solon-server-socketd` — SocketD server 支持。

## 关键源码与配置

- `solon-server/src/main/java/org/noear/solon/server/ServerConstants.java`：SSL、gzip、请求限制、响应编码、signal 名配置常量。
- `solon-server/src/main/java/org/noear/solon/server/ServerProps.java`：请求体、文件大小、header size 等默认值和兼容逻辑。
- `solon-server/src/main/java/org/noear/solon/server/prop/impl/BaseServerProps.java`：`server.@@.*` 通用 signal 配置模板。
- 常见实际配置：`server.http.port`、`server.http.host`、`server.http.coreThreads`、`server.http.maxThreads`。
- 请求限制：`server.request.maxHeaderSize`、`server.request.maxBodySize`、`server.request.maxFileSize`、`server.request.fileSizeThreshold`、`server.request.encoding`。
- gzip：`server.http.gzip.enable`、`server.http.gzip.minSize`、`server.http.gzip.mimeTypes`。
- SSL：`server.ssl.*`。

## 依赖选择与坑点

- 通常只选一个 HTTP server 实现，避免多 server 插件同时监听导致冲突。
- JSP 需要同时考虑 `solon-view-jsp` 和对应 server 的 `*-add-jsp`。
- WebSocket 要按 server 类型选择 add-on 或独立实现。
- `server.@@.*` 是模板，实际要替换为 `server.http.*`、`server.websocket.*`、`server.socket.*`。
- `server.request.maxRequestSize` 是旧兼容配置，新代码优先用 `server.request.maxBodySize`。
