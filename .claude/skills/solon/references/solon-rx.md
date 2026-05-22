# solon-rx

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-rx`

用途：响应式 Web 链与响应式数据访问扩展。

## 直接子模块

- `solon-rx` — 响应式基础抽象。
- `solon-web-rx` — Web 响应式 filter/返回值处理。插件 `org.noear.solon.web.rx.integration.WebRxPlugin`。
- `solon-data-rx-sqlutils` — 响应式 SQLUtils/R2DBC 支持。插件 `org.noear.solon.data.rx.sql.integration.RxSqlUtilsPlugin`。

## 关键源码与 API

- `solon-web-rx/src/main/java/org/noear/solon/web/rx/integration/WebRxPlugin.java`：收集 `RxFilter` Bean 到 `RxChainManager`，注册 `RxReturnValueHandler`。

## 依赖选择与坑点

- 普通 Web 项目不需要引入。
- 响应式 Web 过滤器/返回处理选 `solon-web-rx`。
- 数据侧需要响应式数据库依赖，`solon-data-rx-sqlutils` 不是 JDBC 的直接替代。
