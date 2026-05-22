# solon-testing

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-testing`

用途：Solon 测试启动、HTTP 测试、Mockito、MockWebServer、AOT 测试支持。

## 直接子模块

- `solon-test` — 核心测试库。
- `solon-test-junit5` — JUnit5 适配/聚合。
- `solon-test-junit4` — JUnit4 适配/聚合。

## 关键源码与 API

- `solon-test/src/main/java/org/noear/solon/test/SolonTest.java`：JUnit5 扩展注解，参数包括 `classes/value`、`delay`、`env`、`args`、`properties`、`debug`、`scanning`、`enableHttp`、`enableWebSocket`。
- `solon-test/src/main/java/org/noear/solon/test/HttpTester.java`：提供 `path(...)`、`http(...)` 等 HTTP 测试辅助。

## 依赖选择与坑点

- JUnit5 项目用 `solon-test-junit5` 或直接 `solon-test`。
- JUnit4 项目用 `solon-test-junit4`。
- `enableHttp` 默认 false，Web/API 集成测试需显式开启。
