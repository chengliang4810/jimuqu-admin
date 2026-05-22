# SQL 记录、数据初始化、回调与拦截器、排查

## 数据初始化

支持三种方式：

1. 自动匹配 SQL 脚本。
2. `@AutoTable(initSql = "...")` 手动指定 SQL。
3. `@InitDataList` 静态方法返回初始化数据。

默认目录：`classpath:sql`。

常见结构：

```text
src/main/resources/sql/
├── user.sql
├── role.sql
└── _init_.sql
```

多数据源场景可按数据源分目录；`base-path` 和 `initSql` 支持 `{dialect}` 占位：

```java
@AutoTable(initSql = "classpath:sql/{dialect}/user.sql")
public class User {
}
```

`@InitDataList` 要求：

- 标在实体类中的静态方法上。
- 返回 `List<Entity>`。
- 只在相关表创建完成时执行。

## SQL 记录

配置：

```yaml
auto-table:
  record-sql:
    enable: true
    record-type: file
    folder-path: ./sql-history
    version: "1.0.0"
```

用途：

- 记录表结构维护过程中执行的 SQL。
- 生成变更审计记录。
- 整理为 Flyway/Liquibase 迁移脚本。

`record-type` 支持：`db`、`file`、`custom`。自定义记录方式实现 `RecordSqlHandler`，并在对应框架容器中注册或设置到 `AutoTableGlobalConfig`。

## 拦截器

AutoTable 提供 4 类拦截器：

- `AutoTableAnnotationInterceptor`：扫描实体前，可修改 include/exclude 注解集合。
- `BuildTableMetadataInterceptor`：表元数据构建后，可修改 `TableMetadata`。
- `CreateTableInterceptor`：CREATE TABLE SQL 执行前。
- `ModifyTableInterceptor`：表结构比对完成后、ALTER TABLE SQL 执行前。

典型用途：

- 增加自定义建表注解。
- 按环境修正表/字段注释。
- CREATE/ALTER 前做审计或阻断。
- 动态补充租户字段、审计字段等元数据。

## 回调

常见回调：

- `AutoTableReadyCallback`：配置就绪后、执行前。
- `CreateDatabaseFinishCallback`：自动建库完成后。
- `RunBeforeCallback`：单表执行前。
- `ValidateFinishCallback`：validate 模式校验完成后。
- `DeleteTableFinishCallback`：删表完成后。
- `CreateTableFinishCallback`：建表完成后。
- `CompareTableFinishCallback`：表结构比对完成后。
- `ModifyTableFinishCallback`：改表完成后。
- `RunAfterCallback`：单表执行后。
- `AutoTableFinishCallback`：全部处理完成后。

Spring Boot 中注册为 Bean；Solon 中注册到容器，插件会收集对应类型。

## 常见排查

### 没有创建表

检查：

1. 是否启用 `@EnableAutoTable`。
2. 是否引入正确 starter/plugin。
3. `enable` 是否 true，`mode` 是否不是 none。
4. 实体是否有 `@AutoTable`。
5. 实体是否在扫描范围；多模块要配置 `model-package`、basePackages 或 classes。
6. 数据源是否可用。
7. 方言策略是否存在。
8. 日志若只有 “AutoTable执行结束。耗时：xxms”，通常是未扫描到实体。

### 字段没有创建

检查：

- 字段是否被 `@Ignore` 忽略。
- Java 类型是否能映射到数据库类型；特殊类型需要指定 `@AutoColumn(type=...)` 或自定义类型映射。
- 父类字段是否因 `strict-extends=true` 且为 private 被排除。
- `@AutoColumn(dialect=...)` 是否与当前方言匹配。

### 字段删除或改名

- `update` 模式默认不会删除字段；字段改名通常被认为是新增字段。
- 物理删除需要 `auto-drop-column=true`，有数据丢失风险。
- 更安全方式是配置 `logic-drop-column-prefix` 做逻辑重命名。

### 索引未变化

- `auto-drop-index` 默认只删除以 `indexPrefix` 开头的 AutoTable 索引。
- 自定义索引删除需 `auto-drop-custom-index=true`，谨慎使用。
- `@TableIndex` 的 `fields` 与 `indexFields` 至少配置一个。
- `indexFields` 优先级高于 `fields`。

### 方言不匹配

- 同一数据源下实体 `@AutoTable(dialect=...)` 不能混用多个不同方言。
- 未指定实体方言时从 JDBC 元信息获取。
- 找不到策略时会 warn “没有找到对应的数据库方言策略”。
- 检查是否引入对应 strategy 模块，或使用 `auto-table-strategy-all`。

### 生产部署风险

- 不要在生产使用 `create`。
- 不要在生产开启 `auto-drop-table`、`auto-drop-column`。
- 生产推荐 `validate` 或 `enable=false`。
- 表结构变更推荐通过 `record-sql` + Flyway/Liquibase 审核执行。
