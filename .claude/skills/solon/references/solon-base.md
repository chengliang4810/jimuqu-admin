# solon-base

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-base`

用途：基础能力扩展，包括代理、MVC/handle、国际化、热插拔、状态机等。

## 直接子模块

- `solon-proxy` — 基于 ASM 的代理能力。
- `solon-handle` — Action/处理执行扩展基础。
- `solon-mvc` — MVC 兼容/扩展层，依赖 `solon-handle`。
- `solon-i18n` — 国际化模块，插件 `org.noear.solon.i18n.integration.I18nPlugin`。
- `solon-hotplug` — 外接 jar/小程序插件管理。
- `solon-statemachine` — 状态机 API，带 Preview 标记。

## 关键源码与 API

- `solon-i18n/src/main/java/org/noear/solon/i18n/integration/I18nPlugin.java`：注册 `@I18n` 拦截器、`I18nBundleFactory`、`LocaleResolver` 和 `I18nFilter`。
- `solon-i18n/src/main/java/org/noear/solon/i18n/annotation/I18n.java`：国际化注解，包含 `value`、`bundle`。
- `solon-hotplug/src/main/java/org/noear/solon/hotplug/PluginManager.java`：读取 `solon.hotplug.*`，支持 add/load/unload/start/stop。
- `solon-statemachine/src/main/java/org/noear/solon/statemachine/StateMachine.java`：`addTransition`、`sendEvent` 等状态机 API。

## 依赖选择与坑点

- 国际化选 `solon-i18n`。
- 热插拔不是普通自动装配能力，使用前要读 `PluginManager`。
- `solon-statemachine` 是 Preview，不建议作为稳定公共 API 重度依赖。
