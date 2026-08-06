---
title: 定时任务
description: 系统任务、动态任务与执行日志
---

定时任务模块同时管理 Solon `@Scheduled` 系统任务和页面创建的动态任务。动态任务只能选择后端通过 `@ScheduledJobHandler` 加入白名单的无参方法，不能输入任意类名、SQL 或脚本。

## 系统任务

```java
@Component
public class DataSyncTask {
    /** 周期同步业务数据。 */
    @Scheduled(name = "dataSync", cron = "0 0/5 * * * ? *", zone = "Asia/Shanghai")
    public void execute() {
        // 执行业务同步
    }
}
```

任务名称用于持久化配置、集群锁和执行记录，发布后不要随意修改。

## 动态处理器

```java
@Component
public class DataSyncService {
    /** 提供给后台任务编排的白名单方法。 */
    @ScheduledJobHandler(key = "data.sync", description = "同步业务数据")
    public void sync() {
        // 执行业务同步
    }
}
```

处理器必须是 Solon Bean 中的 `public`、非 `static`、无参数、`void` 方法。新增动态任务固定为停用状态，必须由独立启用操作开启。

## 执行、并发与重试

一次触发及其重试共享 `executionId`，尝试次数为 `1 + maxRetries`。启用重试时并发策略必须为 `FORBID`。执行状态包括 `SUCCESS`、`FAILED`、`RETRY` 和 `SKIPPED`。

立即执行是异步提交，接口成功仅表示进入本节点队列，最终结果以执行日志为准。单节点手动任务并发上限由 `JIMU_SCHEDULING_MANUAL_MAX_CONCURRENT` 控制，默认 16。

## 多实例协调

MySQL 保存任务定义和运行配置，Redis 用于周期认领、互斥锁、服务端时间、变更通知和故障接管。Redis 不可用时任务失败关闭，不会退化为每个节点各自执行。

集群提供至少一次语义，不是严格一次。任务处理器必须通过唯一约束、状态机或外部幂等请求号避免重复副作用。

管理接口位于 `/monitor/job`，并分别使用列表、新增、编辑、删除、启停、执行与日志权限标识。

完整的补偿、租约、代际隔离和 Quartz 替换说明见[定时任务完整说明](/reference/scheduled-jobs-complete/)。
