# 源码地图与模块

默认源码目录：`/Users/chengliang/code-repositories/xbatis`

## 顶层模块

根 `pom.xml` 显示当前源码版本为 `1.10.2`，模块包括：

- `xbatis-bom`：依赖版本管理。
- `xbatis-parent`：父 POM。
- `xbatis-annotation`：注解、枚举、分页接口等基础 API。
- `xbatis-sql-api`：SQL API 抽象。
- `xbatis-sql-api-impl`：SQL API 实现，包含条件、函数、SQL 构造、分页处理等。
- `xbatis-core`：MyBatis 集成、Mapper、链式执行器、反射元数据、逻辑删除、主键生成、结果映射等核心能力。

## 常看源码入口

- 链式查询：`xbatis-core/src/main/java/cn/xbatis/core/sql/executor/chain/QueryChain.java`
- 链式更新：`xbatis-core/src/main/java/cn/xbatis/core/sql/executor/chain/UpdateChain.java`
- 链式删除：`xbatis-core/src/main/java/cn/xbatis/core/sql/executor/chain/DeleteChain.java`
- Mapper 基础接口：
  - `xbatis-core/src/main/java/cn/xbatis/core/mybatis/mapper/BaseMapper.java`
  - `xbatis-core/src/main/java/cn/xbatis/core/mybatis/mapper/BasicMapper.java`
  - `xbatis-core/src/main/java/cn/xbatis/core/mybatis/mapper/MybatisMapper.java`
  - `xbatis-core/src/main/java/cn/xbatis/core/mybatis/mapper/mappers/BaseMapper.java`
- 实体元数据：`xbatis-core/src/main/java/cn/xbatis/core/db/reflect/*`
- MyBatis 配置与代理：`xbatis-core/src/main/java/cn/xbatis/core/mybatis/configuration/*`
- SQL API 实现：`xbatis-sql-api-impl/src/main/java/db/sql/api/impl/*`

## 注解入口

- `@Table`：`xbatis-annotation/src/main/java/cn/xbatis/db/annotations/Table.java`
- `@TableId`：`xbatis-annotation/src/main/java/cn/xbatis/db/annotations/TableId.java`
- `@TableField`：`xbatis-annotation/src/main/java/cn/xbatis/db/annotations/TableField.java`
- `@Condition`：`xbatis-annotation/src/main/java/cn/xbatis/db/annotations/Condition.java`
- `@ConditionTarget`：`xbatis-annotation/src/main/java/cn/xbatis/db/annotations/ConditionTarget.java`
- `@LogicDelete` / `@LogicDeleteTime`：逻辑删除。
- `@Version`：乐观锁。
- `@TenantId`：租户字段。
- `@SplitTable` / `@SplitTableKey` / `@TableSplitter`：分表。
- `@ResultEntity` / `@NestedResultEntity`：VO 与嵌套结果映射。

## 测试示例入口

- 查询：`xbatis-core/src/test/java/com/xbatis/core/test/testCase/query/*`
- 插入：`xbatis-core/src/test/java/com/xbatis/core/test/testCase/insert/*`
- 更新：`xbatis-core/src/test/java/com/xbatis/core/test/testCase/update/*`
- 删除/逻辑删除：`xbatis-core/src/test/java/com/xbatis/core/test/testCase/delete/*`
- 分页/XML 分页：`PagingTest.java`、`XmlPagingTestCase.java`
- 租户：`tenant/TenantTestCase.java`
- 分表：`splitTable/*`
- 多主键：`multiPk/MultiPkTestCase.java`
- 返回映射/嵌套：`query/NestedTestCase.java`、`query/OneToManyTest.java`

## 查证建议

- API 写法优先从 `QueryChain` / `UpdateChain` / `DeleteChain` 定义确认。
- SQL 结果行为优先看 testCase 中的断言，而不是只看 README。
- 项目集成层可能封装了 Xbatis 原生 Mapper；不要把项目封装误认为框架原生能力。
