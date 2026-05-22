# solon-docs

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-docs`

用途：接口文档、OpenAPI 与 Knife4j UI 支持集合。

## 直接子模块

- `solon-docs` — 文档基础模块，插件 `org.noear.solon.docs.integration.DocsPlugin`，核心 API `DocDocket`。
- `solon-docs-openapi2` — OpenAPI 2 文档生成。
- `solon-docs-openapi2-javadoc` — 基于 Javadoc 的 OpenAPI 2 增强，插件 `com.layjava.docs.javadoc.solon.integration.OpenApi2JavadocPlugin`。
- `solon-docs-openapi3` — OpenAPI 3 文档生成。
- `solon-openapi2-knife4j` — OpenAPI 2 + Knife4j UI，插件 `com.github.xiaoymin.knife4j.solon.integration.Knife4jPlugin`。
- `solon-openapi3-knife4j` — OpenAPI 3 + Knife4j UI，插件同为 `Knife4jPlugin`。
- `solon-swagger2-knife4j` — Swagger2/Knife4j 兼容模块，依赖 `solon-openapi2-knife4j`。

## 关键源码与 API

- `solon-docs/src/main/java/org/noear/solon/docs/DocDocket.java`：链式 API 包括 `enable`、`version`、`host`、`schemes`、`groupName`、`basePath`、`basicAuth`、`apis`、`info`。
- `solon-docs/src/main/java/org/noear/solon/docs/integration/properties/DocsProperties.java`：包含 `discover` 和 `routes`。
- `solon-docs/src/main/java/org/noear/solon/docs/integration/properties/DiscoverProperties.java`：包含 discover 开关、uri/contextPath pattern、basicAuth、服务 include/exclude。
- `solon-openapi3-knife4j/src/main/java/com/github/xiaoymin/knife4j/solon/integration/Knife4jController.java`：确认路由 `v3/api-docs/swagger-config`、`swagger/v3`。

## 依赖选择与坑点

- 只要文档抽象用 `solon-docs`。
- 输出 OpenAPI JSON：按规格选 `solon-docs-openapi2` 或 `solon-docs-openapi3`。
- 需要 UI 再叠加对应 Knife4j 模块。
- Knife4j 模块依赖 `solon-web-staticfiles`，禁用静态资源可能影响 UI。
- 不要套用 Springfox/Springdoc 配置模型；Solon 以 `DocDocket` 与 docs properties 为准。
