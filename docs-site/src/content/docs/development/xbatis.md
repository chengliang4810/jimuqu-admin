---
title: Xbatis 查询
description: 项目中 QueryChain 的常用写法
---

## 基础查询

```java
List<SysUserVo> users = QueryChain.of(sysUserMapper)
    .where(where -> where.eq(SysUser::getStatus, "0"))
    .returnType(SysUserVo.class)
    .list();
```

## 终止方法

| 方法 | 用途 | 无结果时 |
| --- | --- | --- |
| `get()` | 单条数据 | `null` |
| `list()` | 列表 | 空列表 |
| `paging(PageQuery)` | 分页 | 空分页 |
| `count()` | 统计 | `0` |
| `exists()` | 判断存在 | `false` |

## 条件

常用条件包括 `eq`、`ne`、`like`、`in`、`gt`、`ge`、`lt`、`le`、`between`、`isNull` 与 `isNotNull`。复杂关系可通过 `exists`、`notExists`、`join` 和 `leftJoin` 表达。

## 约定

- 优先使用方法引用，避免字符串列名。
- `returnType()` 放在终止方法之前。
- 分页统一使用项目的 `PageQuery` 与 `PageResult`。
- 逻辑删除由框架处理，不重复添加条件。
- 复杂查询保留在 Mapper，业务判断保留在 Service。

更完整的注解、聚合、动态数据源与数据库函数说明，可继续整理仓库中的 `docs/xbatis文档.md`。
