---
title: 幂等与操作日志
description: 防重复提交和业务操作审计
---

## 防重复提交

`@RepeatSubmit` 由 `RepeatSubmitInterceptor` 拦截，通过请求特征与缓存判断是否在限定时间内重复提交。

```java
/** 创建业务数据。 */
@Post
@RepeatSubmit
public R<Void> add(@Body BizDemoBo bo) {
    return R.ok();
}
```

它适合表单提交和普通写接口，但不能替代支付、库存等业务幂等键。关键交易应由客户端请求号和数据库唯一约束共同保证。

## 操作日志

`@Log` 记录业务标题、操作类型和执行状态，支持新增、修改、删除、导入、导出等 `BusinessType`。日志事件与业务逻辑解耦，但敏感字段仍需在写日志前过滤。
