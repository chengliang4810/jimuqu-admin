---
title: 接口限流
description: IP、用户与全局维度的接口保护
---

限流模块支持 IP、用户和全局维度，并提供令牌桶、滑动窗口与固定窗口算法。

```java
@RateLimit(
    type = RateLimitType.IP,
    permitsPerSecond = 10,
    maxBurst = 100,
    window = 60,
    algorithm = RateLimitAlgorithm.TOKEN_BUCKET
)
```

## 注解参数

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用 |
| `key` | 空 | GLOBAL 模式的自定义键，空时使用方法全名 |
| `type` | `IP` | `IP`、`USER` 或 `GLOBAL` |
| `permitsPerSecond` | `10.0` | 令牌生成速率 |
| `maxBurst` | `100` | 最大突发数 |
| `window` | `60` | 时间窗口，单位秒 |
| `algorithm` | `TOKEN_BUCKET` | 限流算法 |
| `message` | 空 | 自定义拒绝消息 |

## 算法选择

- **令牌桶**：允许可控突发，适合大多数 API。
- **滑动窗口**：统计更平滑，适合严格频率控制。
- **固定窗口**：实现简单，适合验证码等明确周期限制。

验证码接口直接使用 `RateLimiter` 构造固定窗口规则；普通业务接口优先使用注解。

单实例或低风险接口可以使用本地缓存；多实例部署必须使用所有实例共享的 Redis 状态，否则每个实例都会独立计算额度。

完整配置、算法和场景示例见[限流模块完整指南](/reference/rate-limit-guide/)。
