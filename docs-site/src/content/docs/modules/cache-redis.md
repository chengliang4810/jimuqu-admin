---
title: 缓存与 Redis
description: 缓存驱动、键前缀、版本命名空间和 RedisUtils
---

项目通过 `jimuqu.cache.driverType` 选择缓存驱动，当前主配置默认使用 Redis。连接地址、数据库、密码和键前缀均可由环境变量覆盖。

```yaml
jimuqu.cache:
  driverType: redis
  server: "${JIMU_REDIS_SERVER:127.0.0.1:6379}"
  db: ${JIMU_REDIS_DB:0}
  password: ${JIMU_REDIS_PASSWORD:}
  keyHeader: "${JIMU_REDIS_PREFIX:jimuqu}"
```

`jimuqu-common-cache` 提供缓存配置和 `VersionedCacheNamespace`；`jimuqu-common-redis` 提供 `RedisUtils` 常用操作封装；`jimuqu-common-satoken` 使用带前缀的 Redisson DAO 保存登录态。

## 使用原则

- 所有业务键使用稳定前缀，避免不同环境或应用冲突。
- 缓存失效与数据库提交顺序必须明确。
- 多实例环境不能使用本地缓存保存共享锁、限流额度或登录态。
- 缓存监控接口只对管理员开放。
