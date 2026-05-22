# solon-logging

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-logging`

用途：Solon 日志抽象、日志级别/附加器配置，以及 Logback、Log4j2、Simple 实现适配。

## 直接子模块

- `solon-logging` — 核心日志插件，插件 `org.noear.solon.logging.integration.LoggingPlugin`。
- `solon-logging-simple` — 简单日志实现。
- `solon-logging-logback` — Logback 适配，插件 `ch.qos.logback.solon.integration.LogbackPlugin`。
- `solon-logging-log4j2` — Log4j2 适配，插件 `org.apache.logging.log4j.solon.integration.Log4j2Plugin`。

## 关键源码与配置

- `solon-logging/src/main/java/org/noear/solon/logging/integration/LoggingPlugin.java`：读取 `solon.logging.appender`，初始化 logger level，并注册 MDC 清理 filter。
- `solon-logging/src/main/java/org/noear/solon/logging/LogOptions.java`：读取 `solon.logging.logger.<expr>.level`，`root.level` 用于根级别。
- `solon.logging.appender.<name>.class` 可注册自定义 Appender。

## 依赖选择与坑点

- 选择一个主日志实现即可，避免同时引入多套 SLF4J binding。
- Logback/Log4j2 插件 priority 早于核心 logging，排查日志初始化时注意顺序。
