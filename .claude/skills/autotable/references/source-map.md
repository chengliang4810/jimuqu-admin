# 源码地图与模块

默认源码目录：`/Users/chengliang/code-repositories/auto-table`

根 `pom.xml` 显示当前源码版本为 `2.5.17`，groupId 为 `org.dromara.autotable`。

## 顶层模块

- `auto-table-annotation`：核心注解和枚举。
- `auto-table-core`：启动流程、配置、扫描、元数据构建、策略调度、SQL 记录、数据初始化、回调和拦截器。
- `auto-table-spring-boot-starter`：Spring Boot starter 与 `@EnableAutoTable` 集成。
- `auto-table-solon-plugin`：Solon 插件与 `@EnableAutoTable` 集成。
- `auto-table-strategy`：数据库策略模块。
  - `auto-table-strategy-mysql`
  - `auto-table-strategy-pgsql`
  - `auto-table-strategy-oracle`
  - `auto-table-strategy-dm`
  - `auto-table-strategy-kingbase`
  - `auto-table-strategy-h2`
  - `auto-table-strategy-sqlite`
  - `auto-table-strategy-doris`
  - `auto-table-strategy-all`
- `auto-table-support`：扩展支持，例如 SpringDoc。
- `auto-table-test`：核心、Spring Boot、Solon、多数据源、各数据库策略测试。
- `auto-table-doc`：官方文档源码。

## 核心源码入口

- 启动总流程：`auto-table-core/src/main/java/org/dromara/autotable/core/AutoTableBootstrap.java`
- 全局配置：`auto-table-core/src/main/java/org/dromara/autotable/core/AutoTableGlobalConfig.java`
- 配置模型：`auto-table-core/src/main/java/org/dromara/autotable/core/config/PropertyConfig.java`
- 类扫描：`auto-table-core/src/main/java/org/dromara/autotable/core/AutoTableClassScanner.java`
- 注解查找：`auto-table-core/src/main/java/org/dromara/autotable/core/AutoTableAnnotationFinder.java`
- 元数据处理：`auto-table-core/src/main/java/org/dromara/autotable/core/utils/TableMetadataHandler.java`
- 数据源处理：`auto-table-core/src/main/java/org/dromara/autotable/core/dynamicds/*`
- 策略接口：`auto-table-core/src/main/java/org/dromara/autotable/core/strategy/IStrategy.java`
- SQL 记录：`auto-table-core/src/main/java/org/dromara/autotable/core/recordsql/*`
- 数据初始化：`auto-table-core/src/main/java/org/dromara/autotable/core/initdata/*`

## 集成入口

Spring Boot：

- `auto-table-spring-boot-starter/src/main/java/org/dromara/autotable/springboot/EnableAutoTable.java`
- `auto-table-spring-boot-starter/src/main/java/org/dromara/autotable/springboot/AutoTableAutoConfig.java`
- `auto-table-spring-boot-starter/src/main/java/org/dromara/autotable/springboot/AutoTableImportRegister.java`
- `auto-table-spring-boot-starter/src/main/java/org/dromara/autotable/springboot/AutoTablePropertiesRegister.java`

Solon：

- `auto-table-solon-plugin/src/main/java/org/dromara/autotable/solon/annotation/EnableAutoTable.java`
- `auto-table-solon-plugin/src/main/java/org/dromara/autotable/solon/integration/AutoTablePlugin.java`
- `auto-table-solon-plugin/src/main/java/org/dromara/autotable/solon/properties/AutoTablePropertiesRegister.java`
- `auto-table-solon-plugin/src/main/java/org/dromara/autotable/solon/adapter/SolonDataSourceHandler.java`

## 文档入口

- 配置：`auto-table-doc/docs/API参考/配置项.md`
- 注解速查：`auto-table-doc/docs/API参考/注解速查.md`
- 定义表/列/索引：`auto-table-doc/docs/使用指南/*`
- 运行模式：`auto-table-doc/docs/核心概念/运行模式.md`
- 多数据源：`auto-table-doc/docs/核心概念/多数据源.md`
- 工作原理：`auto-table-doc/docs/核心概念/工作原理.md`
- 类型映射：`auto-table-doc/docs/核心概念/类型映射.md`
- 生产部署：`auto-table-doc/docs/最佳实践/生产环境部署.md`
- 常见问题：`auto-table-doc/docs/常见问题/*`

## 测试入口

- 注解建表实体：`auto-table-test/auto-table-test-core/src/test/java/org/dromara/autotable/test/core/entity/*`
- 效果测试：`auto-table-test/auto-table-test-core/src/test/java/org/dromara/autotable/test/core/effect/*`
- 单元构建测试：`auto-table-test/auto-table-test-core/src/test/java/org/dromara/autotable/test/core/unit/*`
- 回调/拦截器：`auto-table-test/auto-table-test-core/src/test/java/org/dromara/autotable/test/core/extension/*`
- Solon 插件测试：`auto-table-test/auto-table-test-solon-plugin/src/test/java/*`
- Spring Boot 测试：`auto-table-test/auto-table-test-spring-boot/src/test/java/*`

## 启动流程要点

`AutoTableBootstrap.start()` 会：

1. 读取 `PropertyConfig`。
2. 如果 `mode=none` 或 `enable=false`，直接返回。
3. 注册数据库策略和 database builder。
4. 扫描实体类。
5. 触发 ready 回调。
6. 按数据源分组处理实体。
7. 检查同一数据源下重复表名。
8. 确定方言：实体 `dialect` 或连接 `DatabaseMetaData`。
9. 执行策略建表/改表/校验。
10. 必要时初始化数据。
11. 触发 finish 回调并清理全局配置。
