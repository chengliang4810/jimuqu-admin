# Solon Projects 插件索引

本索引对应源码目录：`/Users/chengliang/code-repositories/solon/solon-projects`。

使用规则：

- 只按 `solon-projects` 一级子项目建立索引文件，例如 `solon-web.md`、`solon-view.md`。
- 每个文件只列直接子模块和发现到的 `META-INF/solon/*.properties` 插件类，不继续细分到更深文件类别。
- 需要具体 API、配置项或生命周期时，再进入对应模块源码查证。

## 一级子项目

- [`nami`](nami.md) — Nami RPC/HTTP 客户端与编码器生态，用于远程调用通道、序列化编码扩展。
- [`solon-base`](solon-base.md) — Solon 基础能力扩展，包括 MVC、代理、国际化、热插拔、状态机等基础设施。
- [`solon-boot`](solon-boot.md) — Solon 启动与运行时聚合模块，组合不同 HTTP Server、WebSocket、SocketD、JSP 支持。
- [`solon-config`](solon-config.md) — 配置加载与配置增强模块，包括 YAML、banner、snack 配置等。
- [`solon-data`](solon-data.md) — 数据访问与缓存模块，包括缓存实现、动态数据源、SQL 工具等。
- [`solon-detector`](solon-detector.md) — 健康检查与探测模块，用于服务健康、检测器集成。
- [`solon-docs`](solon-docs.md) — 接口文档与 OpenAPI/Knife4j/Swagger 生态模块。
- [`solon-faas`](solon-faas.md) — 函数式/脚本化服务扩展模块。
- [`solon-logging`](solon-logging.md) — 日志抽象与日志实现适配模块。
- [`solon-native`](solon-native.md) — AOT/native image 相关支持模块。
- [`solon-net`](solon-net.md) — 网络客户端、HTTP 工具、STOMP 等网络能力模块。
- [`solon-rx`](solon-rx.md) — 响应式扩展模块，包含 Web 与数据访问的 RX 支持。
- [`solon-scheduling`](solon-scheduling.md) — 调度任务模块，包括 simple 与 quartz 实现。
- [`solon-security`](solon-security.md) — 安全相关模块，包括认证、校验、加密/国密、Web 安全、密钥保险箱。
- [`solon-serialization`](solon-serialization.md) — 序列化模块，适配 JSON/XML/二进制等多种编码实现。
- [`solon-server`](solon-server.md) — 底层 Server 适配模块，包括 Jetty、Undertow、Tomcat、Vert.x、WebSocket 等。
- [`solon-shell`](solon-shell.md) — 命令行 Shell 支持模块。
- [`solon-testing`](solon-testing.md) — 测试支持模块，包括 JUnit4/JUnit5。
- [`solon-tool`](solon-tool.md) — 构建与配置处理工具模块。
- [`solon-view`](solon-view.md) — 视图模板模块，包括 Freemarker、Thymeleaf、JSP、Beetl、Enjoy、Velocity。
- [`solon-web`](solon-web.md) — Web 扩展模块，包括 session state、静态资源、SSE、CORS、WebDAV、WebServices 等。
