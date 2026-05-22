# 集成、启动流程与多数据源

## Spring Boot 集成

依赖：

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-spring-boot-starter</artifactId>
    <version>...</version>
</dependency>
```

启用：

```java
@EnableAutoTable
@SpringBootApplication
public class Application {
}
```

关键源码：`auto-table-spring-boot-starter/src/main/java/org/dromara/autotable/springboot/AutoTableAutoConfig.java`

启动时会：

1. 注入 `PropertyConfig`。
2. 设置默认 DataSource。
3. 如果 `@EnableAutoTable` 指定了 basePackages/classes，则覆盖 `modelPackage`/`modelClass`。
4. 注册注解扫描器、数据库策略、class 扫描器、ORM 元数据适配器、动态数据源处理器、SQL 记录处理器。
5. 注册拦截器和回调。
6. 注册 Java 类型到数据库类型转换器。
7. 非单元测试模式下调用 `AutoTableBootstrap.start()`。

## Solon 集成

依赖：

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-solon-plugin</artifactId>
    <version>...</version>
</dependency>
```

启用：

```java
@EnableAutoTable
@SolonMain
public class App {
}
```

关键源码：`auto-table-solon-plugin/src/main/java/org/dromara/autotable/solon/integration/AutoTablePlugin.java`

要点：

- 插件 `start(AppContext context)` 会检查启动类是否有 `@EnableAutoTable`；没有则直接返回。
- 通过 `AutoTablePropertiesRegister` 装配属性。
- 使用 `context.lifecycle(-100, () -> resourceLoadFinish(context))` 在资源加载完成后启动。
- 默认从 Solon 容器取 `DataSource`。
- 如果没有 `IDataSourceHandler`，注册 `SolonDataSourceHandler`。
- 收集 `IStrategy`、`AutoTableClassScanner`、`AutoTableMetadataAdapter`、`JavaTypeToDatabaseTypeConverter`、`RecordSqlHandler`、拦截器与回调 Bean。
- 最后调用 `AutoTableBootstrap.start()`。

## AutoTableBootstrap 流程

源码：`auto-table-core/src/main/java/org/dromara/autotable/core/AutoTableBootstrap.java`

主要步骤：

1. 读取 `AutoTableGlobalConfig.instance().getAutoTableProperties()`。
2. `mode=none` 或 `enable=false` 时返回。
3. 打印 banner。
4. 注册数据库策略与 database builder。
5. 扫描实体类。
6. 触发 `AutoTableReadyCallback`。
7. 按 `IDataSourceHandler.getDataSourceName` 对实体分组。
8. 同一数据源内检查重名表。
9. 切换数据源。
10. 查找实体上的 `dialect`；同一数据源下不能有多个不同实体方言。
11. 如果启用自动建库，尝试创建数据库。
12. 未指定实体方言时，从连接 `DatabaseMetaData.getDatabaseProductName()` 获取方言。
13. 根据方言获取 `IStrategy` 并执行。
14. 新建库时初始化库数据。
15. 清理数据源和当前策略。
16. 触发 finish 回调并清空全局配置。

## 多数据源

核心抽象：`IDataSourceHandler`。

- AutoTable 会按实体对应的数据源分组处理。
- 每组处理时调用 `useDataSource(dataSource)` 切换数据源。
- 处理完清理 `clearDataSource(dataSource)` 和 `DataSourceManager.cleanDatasourceName()`。
- 不同框架的数据源注解/路由机制由对应集成或自定义 `IDataSourceHandler` 适配。

## 方言策略

- 方言优先来自实体 `@AutoTable(dialect=...)`。
- 没指定时从 JDBC 连接元信息获取。
- `AutoTableBootstrap.findDialectOnEntity` 要求同一数据源下不能同时使用多个实体方言，否则抛异常。
- 如果找不到对应 `IStrategy`，会 warn 并跳过自动维护表结构。

## 常见集成问题

### 没有创建表

优先检查：

1. 是否添加对应 starter/plugin 依赖。
2. 是否启用 `@EnableAutoTable`。
3. `auto-table.enable` 是否为 true，`mode` 是否不是 none。
4. 实体是否标注 `@AutoTable`。
5. 实体是否在启动类包及子包下；多模块项目需要配置 `model-package` 或 `@EnableAutoTable(basePackages/classes)`。
6. 数据源是否在容器中。
7. 方言策略依赖是否存在、方言是否匹配。
8. 日志是否只显示 “AutoTable执行结束” 且耗时很短；这通常意味着没扫描到实体。

### 父类字段没有创建

优先检查：

- 父类字段是否 public/protected。
- `strict-extends` 是否为 true。
- 是否需要把字段修饰符改为 protected，或谨慎设置 `strict-extends=false`。
