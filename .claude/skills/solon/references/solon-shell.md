# solon-shell

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-shell`

用途：应用启动后交互式 Shell/REPL 与命令扫描。

## 直接子模块

- `solon-shell` — Shell 支持模块。插件 `org.noear.solon.shell.integration.ShellPluginJansi`。

## 关键源码与 API

- `solon-shell/src/main/java/org/noear/solon/shell/integration/ShellPluginJansi.java`：注册 `@Command` 方法提取器、默认 `HelpCommand`，并在应用加载结束后启动 REPL。
- `solon-shell/src/main/java/org/noear/solon/shell/annotation/Command.java`：Shell 命令注解，`value` 为命令名，`description` 用于帮助信息。

## 依赖选择与坑点

- 会启动交互式 REPL，服务端/容器环境慎用。
- 命令名重复会抛异常。
- 注意它的 `@Command` 与 `solon-scheduling` 的 `@Command` 包名不同。
