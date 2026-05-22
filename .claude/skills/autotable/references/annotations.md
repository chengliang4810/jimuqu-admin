# 注解建表、列、索引与方言

## 表级注解

`@AutoTable` 定义在 `auto-table-annotation/src/main/java/org/dromara/autotable/annotation/AutoTable.java`：

- `value`：表名，为空默认取类名。
- `schema`：表 schema。
- `comment`：表注释，为空默认取类名。
- `dialect`：自定义数据库方言，默认从数据源连接获取。
- `initSql`：初始化 SQL 文件路径。

示例：

```java
@AutoTable(value = "sys_user", comment = "用户表")
public class User {
}
```

## 列级注解

`@AutoColumn` 定义在 `AutoColumn.java`，可标在字段或注解类型上：

- `value`：列名。
- `type`：字段类型；不填则根据 Java 属性类型转换，转换失败的字段不会添加。
- `length`：字段长度，默认 -1；小于 0 相当于 null。
- `decimalLength`：小数位数，默认 -1。
- `notNull`：是否不允许为 null，默认 false。
- `defaultValue`：默认值。
- `defaultValueType`：默认值类型，默认 `DefaultValueEnum.UNDEFINED`。
- `comment`：列注释。
- `sort`：字段排序，1 表示第一个，-1 表示最后一个；并非所有数据库都适用。
- `dialect`：指定数据库方言。

常用简化注解：

- `@ColumnName`：列名。
- `@ColumnType`：列类型、长度、小数位。
- `@ColumnNotNull`：非空。
- `@ColumnDefault`：默认值。
- `@ColumnComment`：列注释。
- `@Ignore`：忽略字段或实体。

## 主键和自增

`@PrimaryKey` 可标在字段、类型或注解类型上：

- `autoIncrement`：是否自增，默认 false。

`@AutoIncrement` 可声明自增字段；常见写法仍是：

```java
@PrimaryKey(autoIncrement = true)
private Long id;
```

## 索引

字段级索引 `@Index`：

- `name`：索引名；不设置时自动生成。
- `type`：索引类型，默认 `IndexTypeEnum.NORMAL`。
- `method`：索引方法，如 BTREE、HASH、GIN 等，取决于数据库。
- `comment`：索引注释。

表级组合索引 `@TableIndex`：

- `name`：索引名。
- `type`：索引类型。
- `method`：索引方法。
- `fields`：字段名数组，顺序会影响组合索引左匹配。
- `indexFields`：带排序的字段配置，优先级高于 `fields`。
- `comment`：索引注释。

源码注释提醒：`fields` 与 `indexFields` 必须配置一个，否则不生效。

索引名规则：默认前缀为 `auto_idx_`；超长时会使用字段名 hash 规避数据库索引名长度限制。

## 多数据库适配

同一个字段可用 `@AutoColumns` 配多个 `@AutoColumn`：

```java
@AutoColumns({
    @AutoColumn(type = "longtext", dialect = "MySQL"),
    @AutoColumn(type = "text", dialect = "PostgreSQL"),
    @AutoColumn(type = "clob", dialect = "Oracle")
})
private String content;
```

未指定 `dialect` 的配置作为默认值。框架会根据实体 `dialect` 或数据源方言选择匹配配置。

## 数据库专属注解

MySQL 常见：

- `@MysqlEngine`
- `@MysqlCharset`
- `@MysqlColumnCharset`
- `@MysqlColumnUnsigned`
- `@MysqlColumnZerofill`

Doris、PostgreSQL 等能力以对应 strategy 模块和文档为准；不要把 MySQL 专属能力无差别套到所有数据库。

## 坑点

- AutoTable 注解只维护数据库结构，不替代 ORM 查询注解。
- `type` 不填会走 Java 类型到数据库类型转换；转换失败的字段不会添加，遇到特殊类型先查类型映射或自定义 converter。
- 字段排序并非所有数据库都支持。
- 组合索引字段顺序很重要，影响左匹配。
- 多数据库项目不要只写 MySQL 类型；使用 `@AutoColumns` 或数据库专属注解隔离差异。
