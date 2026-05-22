# solon-tool

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-tool`

用途：构建期工具，包括 Maven/Gradle 打包、AOT、配置元数据处理。

## 直接子模块

- `solon-maven-plugin` — Maven 打包与 AOT 插件。
- `solon-gradle-plugin` — Gradle 插件，插件 id `org.noear.solon`。
- `solon-configuration-processor` — 配置元数据注解处理器。

## 关键源码与 API

- `solon-maven-plugin/src/main/java/org/noear/solon/maven/plugin/RepackageMojo.java`：`repackage`，默认 `PACKAGE` 阶段，支持 `mainClass`、`jvmArguments`、`classifier`、includes/excludes。
- `solon-maven-plugin/src/main/java/org/noear/solon/maven/plugin/ProcessAotMojo.java`：`process-aot`，默认 `PREPARE_PACKAGE` 阶段，支持 `solon.aot.main-class` 等参数。
- `solon-gradle-plugin/build.gradle.kts`：Gradle 插件 id 与实现类。
- `solon-configuration-processor/src/main/java/org/noear/solon/configurationprocessor/ConfigurationMetadataAnnotationProcessor.java`：处理 `@BindProps`，识别 `@Inject`。

## 依赖选择与坑点

- Maven 项目用 `solon-maven-plugin`。
- Gradle 项目用 `solon-gradle-plugin`。
- 配置元数据生成只认 Solon 的 `@BindProps`，不要按 Spring `@ConfigurationProperties` 预期。
