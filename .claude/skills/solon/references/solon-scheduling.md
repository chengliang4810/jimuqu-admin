# solon-scheduling

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-scheduling`

用途：异步、定时、重试、命令调度核心与执行器实现。

## 直接子模块

- `solon-scheduling` — 调度注解与管理抽象。插件 `org.noear.solon.scheduling.integration.SchedulingPlugin`。
- `solon-scheduling-simple` — 轻量内置调度实现。插件 `org.noear.solon.scheduling.simple.integration.SchedulingSimplePlugin`。
- `solon-scheduling-quartz` — Quartz 调度实现。插件 `org.noear.solon.scheduling.quartz.integration.SchedulingQuartzPlugin`。

## 关键源码与 API

- 注解：`@EnableScheduling`、`@EnableAsync`、`@EnableRetry`、`@EnableCommand`。
- `@Scheduled` 支持 `name`、`cron`、`zone`、`initialDelay`、`fixedRate`、`fixedDelay`、`enable`。
- 配置覆盖形态：`solon.scheduling.job.{job name}.enable/cron/zone/fixedRate/fixedDelay/initialDelay`。
- `@Async`、`@Retry`、调度命令 `@Command` 也属于此分类。

## 依赖选择与坑点

- 只加核心模块不等于有执行器，需要选择 `simple` 或 `quartz`。
- 想用配置覆盖定时任务，必须给 job 设置 `name`。
- 简单本地任务选 `simple`；持久化/复杂调度再选 `quartz`。
