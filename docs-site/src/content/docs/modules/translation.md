---
title: 字段翻译
description: 字典、枚举与自定义字段翻译
---

翻译模块将存储值转换为用户可读文本，支持数据库字典、枚举和自定义翻译器。常见场景包括状态名称、用户名称、部门名称和枚举说明。

建议在 VO 字段上声明翻译规则，保持 Entity 存储模型纯粹；批量列表优先使用可批处理的翻译器，避免逐行查询造成 N+1 问题。

## 字典翻译

```java
/** 状态原始值。 */
private String status;

/** 状态显示名称。 */
@Trans(value = "sys_normal_disable", type = TransType.DICT, field = "status")
private String statusName;
```

## 枚举翻译

枚举实现 `TranslatableEnum` 后，在目标字段上指定 `type = TransType.ENUM` 和 `enumClass`。无法获得结果时可以通过 `defaultValue` 设置兜底文本。

## 自定义翻译

实现 `TranslationInterface` 并注册为 Solon Bean，即可扩展用户、部门等业务翻译。`GlobalRouterInterceptor` 在响应阶段统一处理对象、集合和 Map 中的翻译字段。

完整场景与扩展说明见[翻译模块完整指南](/reference/translation-guide/)。
