---
name: autotable
description: 辅助开发、配置、调试和解释 AutoTable 自动维护数据库表结构框架。只要用户提到 AutoTable、org.dromara.autotable、@AutoTable、@AutoColumn、@PrimaryKey、@Index、@TableIndex、@AutoColumns、多数据库 dialect、自动建表/改表/建库、mode validate/create/update/none、auto-drop-column/table/index、record-sql、init-data、Flyway 脚本、Solon 插件或 Spring Boot starter 集成、事件回调/拦截器、类型映射、字段排序或没有创建表等问题，都必须使用此技能并优先查证 /Users/chengliang/code-repositories/auto-table 源码。
---

# AutoTable Skill

你在辅助 AutoTable 项目开发。AutoTable 会自动维护数据库结构，存在 DDL 和数据丢失风险；回答必须以本地源码和官方文档为准，不能套用 JPA、Hibernate、MyBatis-Plus 或某个业务项目的习惯。

默认源码目录：`/Users/chengliang/code-repositories/auto-table`

## 首要原则

1. **先判定任务类型**：注解建表、运行模式、配置、Spring Boot/Solon 集成、多数据源、SQL 记录、数据初始化、生产部署、拦截器/回调、问题排查、数据库适配。
2. **优先查证源码**：不确定的注解属性、配置项、启动时机、回调接口、策略模块、危险开关必须 grep/read 源码或 docs。
3. **谨慎处理 DDL 风险**：`create`、`auto-drop-table`、`auto-drop-column`、`auto-drop-custom-index` 会带来数据或结构丢失风险；生产默认建议 `validate` 或关闭。
4. **区分通用框架与项目规范**：本技能只说明 AutoTable 通用能力。业务项目的目录结构、实体基类、ORM 注解组合、迁移流程以项目技能/文档为准。
5. **输出要可落地**：给 Java 注解、配置片段、依赖选择、验证方式；涉及源码机制时引用 `path:line`。

## 资料入口

按任务读取对应 reference，必要时再读源码：

- 仓库结构、模块与源码入口：`references/source-map.md`
- 注解建表、列、索引与方言：`references/annotations.md`
- 配置、运行模式与生产安全：`references/config-runmode-safety.md`
- 集成、启动流程、多数据源：`references/integration-lifecycle.md`
- SQL 记录、数据初始化、回调与拦截器、排查：`references/advanced-debugging.md`

如果任务很具体，直接 grep 源码优先于泛读 reference。

## 工作流

### 开发实体/表结构

1. 查当前项目是否已有 AutoTable 约定；如果有项目专属技能，先遵守项目规范。
2. 读取 `references/annotations.md`，确认 `@AutoTable`、`@AutoColumn`、主键、索引和数据库方言写法。
3. 多数据库字段差异用 `@AutoColumns` + `dialect`，不要只按当前数据库硬编码到所有环境。
4. 说明生成/变更的表、列、索引和潜在 DDL 风险。

### 配置与部署

1. 读取 `references/config-runmode-safety.md`。
2. 开发环境可用 `update`；测试初始化可用 `create`；生产优先 `validate` 或 `enable=false`。
3. 需要变更审计时开启 `record-sql`，可配合 Flyway/Liquibase 整理脚本。
4. 明确危险开关默认不要开，尤其生产环境。

### 集成与排查

1. Spring Boot 先查 `@EnableAutoTable`、starter、`AutoTableAutoConfig`。
2. Solon 先查 `auto-table-solon-plugin`、`@EnableAutoTable`、`AutoTablePlugin`。
3. “没有创建表”优先查：是否启用、mode、扫描包/类、实体是否标注、数据源是否存在、方言策略是否匹配。
4. 给最小修复，不做无关重构。

## 常用查证命令

```bash
# 核心注解
grep -R "@interface AutoTable\|@interface AutoColumn\|@interface PrimaryKey\|@interface Index\|@interface TableIndex" /Users/chengliang/code-repositories/auto-table/auto-table-annotation/src/main/java

# 配置与启动流程
grep -R "class PropertyConfig\|class AutoTableBootstrap\|class AutoTableAutoConfig\|class AutoTablePlugin" /Users/chengliang/code-repositories/auto-table

# 回调与拦截器
grep -R "interface .*Callback\|interface .*Interceptor" /Users/chengliang/code-repositories/auto-table/auto-table-core/src/main/java

# 测试示例
find /Users/chengliang/code-repositories/auto-table/auto-table-test -path '*/src/test/java/*' -type f
```

## 输出规范

- 用中文说明；Java 标识符和配置键保持原样。
- 涉及源码机制时必须引用 `path:line`。
- 不确定时说“源码中未确认”，不要补 JPA/Hibernate/MyBatis-Plus 语义。
- 区分 AutoTable 通用能力、Spring Boot 集成、Solon 集成和业务项目封装。
- 涉及危险 DDL 配置时必须标明风险和更安全的替代方案。
