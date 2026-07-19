# 运行时接口覆盖

集成测试启动 Solon 后创建一次覆盖快照：

```java
RuntimeRouteCoverage coverage = RuntimeRouteCoverage.snapshotApplicationRoutes(Set.of());
```

每个真实 HTTP 用例完成响应和数据库断言后，再记录实际请求：

```java
coverage.record("GET", "/system/user/list");
```

套件结束调用 `coverage.assertComplete()`。分母来自 `Solon.app().router().findAll()` 中的
`com.jimuqu` Controller 或自定义 Handler 路由；新接口会自动进入分母。框架自动路由如需排除，必须通过
`snapshotApplicationRoutes` 的显式 `RouteKey` 集合排除，禁止按目录或前缀静默过滤。

该统计只代表 HTTP 操作覆盖率。下载、SSE 和 WebSocket 仍需独立协议断言，不能用普通
JSON 成功响应代替。
