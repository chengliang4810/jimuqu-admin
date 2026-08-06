---
title: 限流模块完整指南
description: 限流配置、算法、维度和场景参考
---

## 概述

`jimuqu-common-ratelimit` 模块提供了基于CacheService的统一限流功能，支持多种限流算法，用户可以通过配置CacheService来自由选择本地缓存、Redis或其他缓存实现。

## 功能特性

- **多种限流算法**：令牌桶、滑动窗口、固定窗口
- **统一缓存接口**：基于CacheService，支持本地缓存、Redis等
- **注解支持**：通过 `@RateLimit` 注解实现方法级限流
- **灵活配置**：支持配置文件和注解两种配置方式
- **多种维度限流**：支持基于IP、用户、方法等多维度限流

## 配置说明

### 1. 基础配置

在 `app.yml` 中添加以下配置：

```yaml
jimuqu:
  ratelimit:
    # 是否启用限流
    enabled: true
    # 以下为默认限流配置，注解未指定则使用默认配置
    # 限流类型：IP-IP限流，USER-用户限流，GLOBAL-全局限流
    type: IP
    # 每秒生成令牌数
    permitsPerSecond: 10.0
    # 最大突发请求数
    maxBurst: 100
    # 限流时间窗口（秒）
    window: 60
    # 限流算法：TOKEN_BUCKET, SLIDING_WINDOW, FIXED_WINDOW
    algorithm: TOKEN_BUCKET
    # 限流键前缀
    keyPrefix: "rate_limit:"
    # 限流失败时的错误消息
    errorMessage: "请求过于频繁，请稍后再试"
```

### 2. 缓存配置

限流器使用CacheService存储限流数据，可以根据需要配置不同的缓存实现：

#### 本地缓存配置：
```yaml
# 本地缓存
jimuqu.cache:
  driverType: "local" #缓存类型
  keyHeader: "jimuqu"
```

#### Redis缓存配置：
```yaml
jimuqu.cache:
  driverType: "redis" #驱动类型
  server: "localhost:6379"
  db: 0 #默认为 0，可不配置
  password: jimuqu
```

## 使用方式

### 1. 注解方式（推荐）

在需要限流的方法上添加 `@RateLimit` 注解：

```java
import com.jimuqu.common.ratelimit.annotation.RateLimit;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import com.jimuqu.common.ratelimit.enums.RateLimitAlgorithm;

@RestController
public class TestController {

    // IP限流（默认）：每个IP每秒10次请求
    @RateLimit(permitsPerSecond = 10)
    @GetMapping("/ip-test")
    public String ipTest() {
        return "ip rate limit test";
    }

    // 用户限流：每个用户每秒5次请求
    @RateLimit(type = RateLimitType.USER,
               permitsPerSecond = 5)
    @GetMapping("/user-test")
    public String userTest() {
        return "user rate limit test";
    }

    // 全局限流：所有用户总共每秒20次请求
    @RateLimit(type = RateLimitType.GLOBAL,
               permitsPerSecond = 20)
    @GetMapping("/global-test")
    public String globalTest() {
        return "global rate limit test";
    }

    // 自定义全局限流键
    @RateLimit(type = RateLimitType.GLOBAL,
               key = "custom_global_key",
               permitsPerSecond = 15)
    @GetMapping("/custom-global")
    public String customGlobal() {
        return "custom global rate limit test";
    }

    // 使用滑动窗口算法的IP限流
    @RateLimit(type = RateLimitType.IP,
               algorithm = RateLimitAlgorithm.SLIDING_WINDOW,
               permitsPerSecond = 8,
               maxBurst = 20)
    @GetMapping("/sliding-window")
    public String slidingWindow() {
        return "sliding window rate limit test";
    }

    // 使用固定窗口算法的用户限流
    @RateLimit(type = RateLimitType.USER,
               algorithm = RateLimitAlgorithm.FIXED_WINDOW,
               permitsPerSecond = 3,
               window = 60)
    @GetMapping("/fixed-window")
    public String fixedWindow() {
        return "fixed window rate limit test";
    }
}
```

### 2. 手动调用方式

```java
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.exception.RateLimitException;

@Service
public class TestService {

    @Inject
    private RateLimiter rateLimiter;

    public void businessMethod() {
        // 尝试获取令牌
        boolean acquired = rateLimiter.tryAcquire("business_key", 1);

        if (!acquired) {
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }

        // 执行业务逻辑
        // ...
    }
}
```

## 注解参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| key | String | "" | 限流键，当dimension为GLOBAL时使用 |
| type | RateLimitType | IP | 限流类型：IP-IP限流，USER-用户限流，GLOBAL-全局限流 |
| permitsPerSecond | double | 10.0 | 每秒生成令牌数 |
| maxBurst | int | 100 | 最大突发请求数 |
| window | long | 60 | 限流时间窗口（秒） |
| algorithm | RateLimitAlgorithm | TOKEN_BUCKET | 限流算法 |
| enabled | boolean | true | 是否启用限流 |
| message | String | "" | 自定义错误消息 |

## 缓存配置详解

### 缓存配置参数

```yaml
jimuqu.cache:
  driverType: "local"  # 缓存驱动类型
  keyHeader: "jimuqu"  # 缓存键前缀
```

#### 支持的缓存类型

| 缓存类型 | 适用场景 | 优势 | 注意事项 |
|---------|---------|------|---------|
| **local** | 单机应用，开发环境 | 性能高，无需外部依赖 | 不支持分布式，服务重启后数据丢失 |
| **redis** | 分布式系统，生产环境 | 支持分布式，数据持久化 | 需要配置Redis连接 |

### 缓存类型选择建议

#### 1. 单机应用
```yaml
# 推荐使用本地缓存
jimuqu.cache:
  driverType: "local"
  keyHeader: "jimuqu"
```

#### 2. 分布式应用
```yaml
# 推荐使用Redis缓存
jimuqu.cache:
  driverType: "redis" #驱动类型
  server: "localhost:6379"
  db: 0 #默认为 0，可不配置
  password: jimuqu
```

### 缓存键管理

限流器会自动在缓存键前添加前缀，格式为：
```
{keyHeader}:{keyPrefix}:{限流键}
```

例如：
- IP限流键：`jimuqu:rate_limit:ip:192.168.1.100`
- 用户限流键：`jimuqu:rate_limit:user:12345`
- 全局限流键：`jimuqu:rate_limit:global:com.example.controller.TestController:testMethod`

## 限流算法对比

### 1. 令牌桶算法（TOKEN_BUCKET）

- **优点**：允许突发流量，流量整形效果好
- **缺点**：实现相对复杂
- **适用场景**：需要处理突发请求的场景

### 2. 滑动窗口算法（SLIDING_WINDOW）

- **优点**：限流更精确，避免固定窗口的临界问题
- **缺点**：内存占用较大
- **适用场景**：对限流精度要求高的场景

### 3. 固定窗口算法（FIXED_WINDOW）

- **优点**：实现简单，性能好
- **缺点**：存在窗口临界问题
- **适用场景**：对限流精度要求不高的场景

## 异常处理

当请求被限流时，会抛出 `RateLimitException` 异常。建议在全局异常处理器中处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public R<Void> handleRateLimitException(RateLimitException e) {
        return R.fail(e.getMessage());
    }
}
```

## 限流维度

### 1. IP限流（默认）
- **限流键格式**：`ip:{client_ip}`
- **适用场景**：防止恶意IP频繁请求，如登录接口、短信验证码接口
- **示例**：每个IP每分钟最多5次登录尝试
- **优势**：有效防止DDoS攻击和恶意爬虫

### 2. 用户限流
- **限流键格式**：`user:{user_id}` 或 `user:anonymous`（未登录用户）
- **适用场景**：基于用户的操作频率控制，如发帖、评论、下单
- **示例**：每个用户每分钟最多发布3条评论
- **优势**：精细化控制用户行为，防止刷单

### 3. 全局限流
- **限流键格式**：`{class_name}:{method_name}` 或自定义key
- **适用场景**：接口整体频率控制，如API调用限制、保护服务稳定性
- **示例**：某个接口每秒最多处理100个请求
- **优势**：保护服务不被打满，保证系统稳定性

## 架构优势

1. **统一接口**：使用CacheService统一缓存接口，支持多种缓存实现
2. **配置灵活**：通过配置文件即可切换不同的缓存实现
3. **代码简洁**：精简架构，移除了多余的包装层
4. **扩展性强**：新增缓存实现时，限流器无需修改
5. **性能优化**：使用CAS乐观锁和时间分片提高并发性能
6. **兼容性好**：不依赖Lua脚本，适配更多缓存实现

## 性能建议

1. **缓存选择**：根据场景选择合适的CacheService实现
   - 单机应用：使用本地缓存，性能更高
   - 分布式系统：使用Redis，保证数据一致性
2. **算法选择**：
   - 令牌桶算法：推荐使用，平衡了性能和效果，支持突发流量
   - 滑动窗口算法：限流更精确，避免临界问题
   - 固定窗口算法：实现简单，性能最好
3. **限流键设计**：避免使用过长的键名，影响性能
4. **并发处理**：当前实现使用CAS乐观锁和重试机制保证原子性

## 并发安全性

由于CacheService不支持Lua脚本，当前实现采用了以下策略保证并发安全：

### 1. 令牌桶算法
- **CAS乐观锁机制**：通过版本号和重试策略避免竞态条件
- **重试次数**：最多重试3次，超过次数默认拒绝请求
- **数据结构**：使用带版本号的TokenBucket对象

### 2. 滑动窗口算法
- **时间分片**：将窗口分为多个时间分片，减少并发冲突
- **分片计数**：每个时间分片独立计数，避免全局竞争

### 3. 固定窗口算法
- **原子更新**：通过重试机制确保计数的原子性
- **窗口隔离**：不同时间窗口使用不同的键，避免冲突

## 注意事项

1. 限流器异常时会默认放行，确保业务可用性
2. 限流器依赖于CacheService的实现，请确保缓存服务正常运行
3. 用户限流需要配置相应的用户认证系统
4. 限流配置的变更需要重启应用才能生效
5. 不同缓存实现的性能和特性可能不同，请根据实际需求选择
6. 高并发场景下建议使用Redis缓存，本地缓存可能存在性能瓶颈

## 架构优势

1. **统一接口**：使用CacheService统一缓存接口，支持多种缓存实现
2. **配置灵活**：通过配置文件即可切换不同的缓存实现
3. **代码简洁**：移除了复杂的本地/Redis限流器选择逻辑
4. **扩展性强**：新增缓存实现时，限流器无需修改
5. **性能优化**：使用CAS乐观锁和时间分片提高并发性能
6. **兼容性好**：不依赖Lua脚本，适配更多缓存实现