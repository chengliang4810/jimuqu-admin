# solon-config

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-config`

用途：配置文件加载、配置对象转换、Banner 输出等配置增强能力集合。

## 直接子模块

- `solon-config-yaml` — YAML / properties 配置加载扩展，支持 `.properties`、`.yml`、`.yaml`。
- `solon-config-snack3` — 基于 Snack3 的属性对象转换扩展。
- `solon-config-snack4` — 基于 Snack4 的属性对象转换扩展。
- `solon-config-plus` — 配置增强模块，依赖 Snack3 配置能力。
- `solon-config-banner` — Banner 输出插件。

## 关键源码与 API

- `solon-config-yaml/src/main/java/org/noear/solon/extend/impl/PropsLoaderExt.java`：支持 `.properties`、`.yml`、`.yaml`，并可根据文本内容选择 properties 或 YAML 解析。
- `solon-config-snack4/src/main/java/org/noear/solon/extend/impl/PropsConverterExt.java`：属性转 Bean，优先尝试 `Properties` 构造器，否则用 Snack4 bind。
- `solon-config-banner/src/main/java/org/noear/solon/banner/integration/BannerPlugin.java`：Banner 插件入口。
- `solon-config-banner/src/main/resources/META-INF/solon/solon.banner.properties`：声明 `org.noear.solon.banner.integration.BannerPlugin`，priority 为 `999999`。

## 配置与行为

Banner 配置：

- `solon.banner.enable`：默认 `true`。
- `solon.banner.mode`：默认 `console`，支持 `log`、`both`、`console`。
- `solon.banner.path`：默认 `banner.txt`。

配置加载扩展：

- `.properties` 使用 `Properties.load(reader)`。
- `.yml` / `.yaml` 使用 YAML 加载。
- 文本以 `{...}` 或 `[...]` 开头按 YAML 解析；`=` 早于 `:` 时按 properties 解析。

## 依赖选择与坑点

- 需要 YAML 配置文件能力时选 `solon-config-yaml`。
- 配置对象转换能力与 Snack 版本有关，按项目 Snack 版本选择 `snack3` 或 `snack4`。
- 并不是所有 config 子模块都有 `META-INF/solon` 插件声明，部分通过 `org.noear.solon.extend.impl.*Ext` 扩展类生效。
- Banner 插件输出逻辑在构造阶段，priority 极高，不要当作普通业务启动回调处理。
