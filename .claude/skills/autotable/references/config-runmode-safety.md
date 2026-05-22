# 配置、运行模式与生产安全

配置前缀：`auto-table`。

核心配置源码：`auto-table-core/src/main/java/org/dromara/autotable/core/config/PropertyConfig.java`

## 基础配置

- `enable`：是否启用自动维护表功能，默认 true。
- `show-banner`：是否显示 banner，默认 true。
- `mode`：运行模式，默认 `update`。
- `model-package`：model 包路径，多个路径可用分号或逗号分隔，支持类似 `com.bz.**.entity`。
- `model-class`：指定 model 类，适合精细化控制或测试。
- `index-prefix`：索引前缀，默认 `auto_idx_`。

## 运行模式

文档列出模式：`none`、`validate`、`add`、`create`、`update`；源码 `PropertyConfig` 默认 `RunMode.update`。

常见含义：

- `none`：不执行任何处理，类似 `enable=false`。
- `validate`：只校验数据库结构与实体是否匹配，不执行 DDL；不一致则启动失败。
- `create`：先删除实体声明的表，再根据实体重建；会清空数据。
- `update`：表不存在则创建；表存在则比对差异并增量更新。默认不会删除字段。
- `add`：遇到该模式时先查 `RunMode`、文档和测试确认具体行为。

## 危险配置

这些配置可能造成不可逆损失：

- `mode=create`：会删表重建。
- `auto-drop-table=true`：删除没有声明的表，强烈不建议开启。
- `auto-drop-column=true`：删除数据库中多余列，列数据会丢失。
- `auto-drop-custom-index=true`：删除不以 `indexPrefix` 开头的自定义索引。
- `auto-build-database=true`：自动建库，需要额外权限；validate 模式下数据库不存在会抛异常。

更安全替代：

- 生产环境使用 `mode=validate` 或 `enable=false`。
- 需要删除字段时优先使用 `logic-drop-column-prefix` 逻辑重命名，而不是物理删除。
- 开启 `record-sql` 记录变更，再整理成 Flyway/Liquibase 脚本。

## auto-drop-table 保护项

- `auto-drop-table-prefix`：只删除指定前缀匹配的表。
- `auto-drop-table-ignores`：跳过指定表。

源码执行流程中，`AutoTableBootstrap.deleteUnregisterTables` 还会跳过 SQL 记录表，并剔除本次声明过的表。

## 逻辑删除列

- `logic-drop-column-prefix`：未开启 `autoDropColumn` 时，如果数据库中存在但实体中不存在的列，会重命名为“前缀 + 原列名”，而不是物理删除。
- `autoDropColumn` 优先级高于该配置；同时开启时仍执行物理删除。

## 父类字段

- `strict-extends`：默认 true，只继承 public、protected 字段；父类 private 字段默认不会创建。
- `super-insert-position`：父类字段排序位置，`after` 或 `before`，默认 `after`。

## SQL 记录

`record-sql`：

- `enable`：默认 false。
- `record-type`：`db`、`file`、`custom`，默认 `db`。
- `version`：建议指定，会体现在数据库字段或文件名上。
- `table-name`：数据库记录方式下表名。
- `folder-path`：文件记录方式必须设置，目录不存在会自动创建。
- `datasource`：可为数据库记录方式指定独立数据源。

推荐流程：开发环境 `update` → 记录 SQL → 整理迁移脚本 → 生产通过 Flyway/Liquibase 执行 → 生产 `validate` 校验。

## 初始化数据配置

`init-data`：

- `enable`：是否开启初始化数据，默认 true。
- `base-path`：默认 `classpath:sql`，支持 `{dialect}` 占位。
- `default-init-file-name`：默认 `_init_`。

## 生产建议

推荐：

```yaml
auto-table:
  mode: validate
  show-banner: false
```

或完全禁用：

```yaml
auto-table:
  enable: false
```

生产环境不要开启 `create`、`auto-drop-table`、`auto-drop-column`；`update` 也要谨慎，通常只放开发/测试环境。
