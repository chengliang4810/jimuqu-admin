---
title: 数据访问与自动建表
description: Xbatis、分页、实体基类与 AutoTable
---

`jimuqu-common-mybatis` 提供项目数据层基线：`BaseEntity`、`BoBaseEntity`、`BaseMapperPlus`、`Page`、`PageQuery`、数据权限规则和 AutoTable 适配。

## 实体约定

- Entity 继承 `BaseEntity`。
- 使用 `@Table` 指定表名。
- 使用 `@TableId` 声明主键策略。
- 使用 `@AutoColumn` 描述列注释、长度、空值与索引信息。
- VO 与 Entity 分离，Mapper 使用 `BaseMapperPlus<Entity, Vo>`。

## 分页

Controller 接收分页参数后构造 `PageQuery`，Mapper 或 Service 返回 `Page<T>`，最终转换为统一的 `PageResult<T>`。

## AutoTable

当前配置启用自动建库、结构更新和 SQL 文件记录，但关闭自动删列与删索引。首次创建空库后执行 `classpath:sql/{dialect}/jimuqu.sql` 初始化数据，结构变更 SQL 记录到 `./db/sql`。

:::caution[生产结构变更]
AutoTable 能自动维护结构，但不替代生产迁移评审。涉及重命名、类型缩窄、数据回填或大表索引时，应先生成并审查迁移方案。
:::
