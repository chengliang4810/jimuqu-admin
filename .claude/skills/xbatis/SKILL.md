---
name: xbatis
description: 辅助开发、迁移、调试和解释 Xbatis ORM / SQL API。只要用户提到 Xbatis、cn.xbatis、QueryChain、UpdateChain、DeleteChain、BaseMapper、MybatisMapper、@Table/@TableId/@TableField、@Condition/@ConditionTarget、逻辑删除、乐观锁、租户、分表、嵌套 VO、mapWithKey、SQL 模板、分页、AutoTable 组合使用，或需要根据 /Users/chengliang/code-repositories/xbatis 源码确认 API 与机制，都必须使用此技能。此技能只记录通用 Xbatis 能力，不写入某个业务项目的专属规范。
---

# Xbatis Skill

你在辅助 Xbatis 开发。Xbatis 公开资料相对少，回答和实现必须优先以本地源码为准，而不是套用 MyBatis-Plus、JPA 或通用 MyBatis 习惯。

默认源码目录：`/Users/chengliang/code-repositories/xbatis`

## 首要原则

1. **先判定任务类型**：实体映射、Mapper/DAO、链式查询、条件对象、分页、增删改、逻辑删除、乐观锁、租户/分表、VO 映射、SQL 模板、问题排查。
2. **优先查证本地源码**：不确定的注解、方法、参数语义、返回类型、模块依赖必须 grep/read 源码或测试示例。
3. **避免框架惯性**：不要默认使用 MyBatis-Plus 的 `Wrapper`、`IService`、`BaseMapper` 语义，也不要套 JPA 注解或 Spring Data 写法。
4. **区分通用框架与项目规范**：本技能只说明 Xbatis 通用能力。具体项目的目录结构、权限、返回包装、BaseMapperPlus 等以项目技能或项目文档为准。
5. **输出要可落地**：给 Java 代码、依赖坐标、查询链写法、注解写法和验证方式；机制解释要引用 `path:line`。

## 资料入口

按任务读取对应 reference，必要时再读源码：

- 仓库结构、模块与源码入口：`references/source-map.md`
- 实体、注解、主键、默认值：`references/entity-annotations.md`
- QueryChain / UpdateChain / DeleteChain 与 Mapper：`references/chain-and-mapper.md`
- 条件对象、分页、返回映射：`references/condition-paging-result.md`
- 高级能力与排查：`references/advanced-debugging.md`

如果任务很具体，直接 grep 源码优先于泛读 reference。

## 工作流

### 业务开发

1. 查当前项目已有 Xbatis 写法与封装；如果当前项目有专属技能，先遵守项目技能。
2. 读取本技能对应 reference，确认 Xbatis 原生 API。
3. 写实体/Mapper/查询链时用当前项目已有封装承接 Xbatis 能力；不要把某项目的封装当成 Xbatis 原生 API。
4. 验证：至少编译；涉及 SQL 行为时优先写或运行相关单测，或输出生成 SQL/预期条件。

### 源码机制解释

1. 定位源码：`grep -R "class QueryChain\|@interface Condition\|interface BaseMapper" /Users/chengliang/code-repositories/xbatis`。
2. 读取定义和测试用例，不只读 README。
3. 输出结构：结论 → 关键源码路径 → 机制流程 → 使用建议/坑点。

### 问题排查

1. 收集实体注解、Mapper 类型、链式调用、异常堆栈、数据库类型与依赖版本。
2. 对照源码确认失败点：实体未标 `@Table`、未指定 entityType、Query 对象 target/storey 错误、分页对象不兼容、逻辑删除/租户自动条件、返回类型自动 select 等。
3. 给最小修复，不做无关重构。

## 常用查证命令

```bash
# 核心链式 API 与 Mapper
grep -R "class QueryChain\|class UpdateChain\|class DeleteChain\|interface BaseMapper\|interface MybatisMapper" /Users/chengliang/code-repositories/xbatis/xbatis-core/src/main/java

# 核心注解
grep -R "@interface Table\|@interface TableId\|@interface TableField\|@interface Condition\|@interface ConditionTarget" /Users/chengliang/code-repositories/xbatis/xbatis-annotation/src/main/java

# 测试示例
grep -R "QueryChain.of\|UpdateChain.of\|DeleteChain.of\|mapWithKey\|returnType\|paging" /Users/chengliang/code-repositories/xbatis/xbatis-core/src/test/java
```

## 输出规范

- 用中文说明；Java 标识符保持原样。
- 涉及源码机制时必须引用 `path:line`。
- 涉及 API 不确定时，先查证再回答；查不到就明确说“源码中未确认”。
- 代码示例尽量短，优先贴近 Xbatis 官方源码和测试示例。
- 明确哪些是 Xbatis 原生能力，哪些是项目自定义封装。
