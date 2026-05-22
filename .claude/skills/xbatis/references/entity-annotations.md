# 实体、注解、主键与默认值

## 实体注解

`@Table` 定义在 `xbatis-annotation/src/main/java/cn/xbatis/db/annotations/Table.java`：

- `value`：表名。
- `schema`：数据库 schema。
- `columnNameRule`：列名规则，默认 `ColumnNameRule.IGNORE`。
- `databaseCaseRule`：数据库大小写规则。

常见实体：

```java
@Table("sys_user")
public class SysUser {
    @TableId
    private Long id;

    @TableField("user_name")
    private String userName;
}
```

## 字段注解

`@TableField` 定义在 `xbatis-annotation/src/main/java/cn/xbatis/db/annotations/TableField.java`：

- `value`：列名。
- `select`：是否参与 select，默认 true；大字段可设 false。
- `insert`：是否参与 insert，默认 true。
- `update`：是否参与实体/Model 更新，默认 true；对 `UpdateChain` 不生效。
- `neverUpdate`：永远不修改，即使强制也不生效。
- `exists`：是否为表字段，默认 true。
- `jdbcType`、`typeHandler`：JDBC 类型和 MyBatis TypeHandler。
- `defaultValue`、`defaultValueFillAlways`：插入默认值与是否总是填充。
- `updateDefaultValue`、`updateDefaultValueFillAlways`：更新默认值与是否总是填充。
- `definition`：列定义，给未来表生成使用，不是 ORM 操作逻辑。

默认值说明：`defaultValue` / `updateDefaultValue` 中 `""` 表示 NULL；需要表达空字符串时用 `{BLANK}`。README 提到内置动态默认值如 `{NOW}`、`{TODAY}`，具体支持点遇到任务时查 `DefaultValue*` 测试。

## 主键注解

`@TableId` 定义在 `xbatis-annotation/src/main/java/cn/xbatis/db/annotations/TableId.java`：

- `value`：`IdAutoType`，默认 `AUTO`。
- `dbType`：数据库类型，例如 `DbType.Name.MYSQL`。
- `generator`：自增器名字，可用 `cn.xbatis.core.incrementer.Generators` 常量。
- `generatorName`：已废弃，后续使用 `generator` 替代。
- `sql`：`IdAutoType.SQL` 时必须填。

自定义生成器需要实现 `cn.xbatis.core.incrementer.Generator`，并注册到 `GeneratorFactory.register(name, generator)`。

## 常用高级注解

- `@LogicDelete`：逻辑删除字段。
- `@LogicDeleteTime`：逻辑删除时间。
- `@Version`：乐观锁。
- `@TenantId`：租户字段。
- `@SplitTable`、`@SplitTableKey`、`@TableSplitter`：分表。
- `@TypeHandler` 或 `@TableField(typeHandler=...)`：特殊类型处理。
- `@ResultEntity`、`@ResultEntityField`、`@NestedResultEntity`：返回 VO 映射。
- `@OnInsert`、`@OnUpdate`：插入/更新监听。

## 坑点

- 不要把 AutoTable 的 `@AutoColumn` 当作 Xbatis 原生注解；它可与 Xbatis 配合，但属于另一个库。
- `@TableField(update=false)` 只对实体类、Model 类修改方法生效；源码注释明确 `UpdateChain` 等不会生效。
- `generatorName` 已废弃，新代码优先用 `generator`。
- 多数据库主键策略、逻辑删除、租户、分表必须查对应测试用例后再写示例。
