# solon-detector

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-detector`

用途：健康检查与系统探测指标。

## 直接子模块

- `solon-health` — 健康检查端点 `/healthz`，插件 `org.noear.solon.health.integration.HealthPlugin`。
- `solon-health-detector` — 内置 detector 接入，插件 `org.noear.solon.health.detector.integration.DetectorPlugin`。

## 关键源码与 API

- `solon-health/src/main/java/org/noear/solon/health/integration/HealthPlugin.java`：注册 GET/HEAD `/healthz`，收集 `HealthIndicator` Bean。
- `solon-health/src/main/java/org/noear/solon/health/HealthHandler.java`：DOWN 返回 503，ERROR 返回 500，否则 200。
- `solon-health-detector/src/main/java/org/noear/solon/health/detector/integration/HealthConfigurator.java`：读取 `solon.health.detector`。

## 依赖选择与坑点

- 只需要健康端点用 `solon-health`。
- 需要 OS/JVM/QPS 等内置探测再加 `solon-health-detector` 并配置 `solon.health.detector`。
- 不配置 detector 时，detector 模块不会自动启用全部探测项。
