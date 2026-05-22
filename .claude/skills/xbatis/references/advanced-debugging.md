# 高级能力与排查

## 高级能力索引

Xbatis README 与源码/测试覆盖的常见高级能力：

- 动态默认值：`DefaultValue*` 测试、`@TableField(defaultValue/updateDefaultValue)`。
- 不同数据库 ID 自增：`@TableId(dbType=..., value=..., generator=...)`。
- 逻辑删除：`@LogicDelete`、`@LogicDeleteTime`、`LogicDeleteSwitch`、`LogicDeleteUtil`。
- 自定义 SQL / SQL 模板：`cmdTemplete/CmdTemplateTestCase.java`、`db.sql.api.impl.cmd.basic.CmdTemplate`。
- `mapWithKey`：`query/MapWithKeyTest.java`。
- 部分字段新增/修改：insert/update 测试中 force / not force 场景。
- 枚举支持：`@PutEnumValue`、`TestEnum`、相关测试。
- XML 和 `@Select` 自动分页：`XmlPagingTestCase.java`。
- 嵌套 VO 自动映射：`NestedTestCase.java`、`OneToManyTest.java`。
- 乐观锁：`@Version`、`version/VersionTestCase.java`。
- 多租户：`@TenantId`、`tenant/TenantTestCase.java`。
- 分表：`@SplitTable`、`@SplitTableKey`、`splitTable/*`。
- 多主键：`multiPk/MultiPkTestCase.java`。
- TypeHandler：`typeHandler/*`、`@TableField(typeHandler=...)`。
- Mapper 方法拦截器：`mapperMethodInterceptor/*`。

## 排查清单

### QueryChain 报实体类型错误

重点查：

- 是否使用了 `QueryChain.create()` 但没调用 `withMapper(...)`。
- 是否用 `QueryChain.of(BaseMapper, Class<T>)` 时漏传 entityType。
- Mapper 是否是 `MybatisMapper<T>`，能否通过 `getEntityType()` 推断实体。

### 条件对象不生效

重点查：

- Query 类是否标 `@ConditionTarget(Entity.class)`。
- 字段是否标 `@Condition`。
- `target`、`property`、`storey` 是否匹配当前查询实体/关联层级。
- 字段值是否为 null/空字符串且被 `forSearch()` 过滤。
- 是否需要 `beforeBuildCondition()` 做预处理。

### 查询字段缺失或 VO 映射异常

重点查：

- 是否使用 `returnType(Vo.class)`。
- VO 是否有 `@ResultEntity` 或相关映射注解。
- 嵌套 VO 是否使用 `@NestedResultEntity`。
- 是否调用了 `disableAutoSelect()` 导致自动 select 关闭。
- 大字段是否被 `@TableField(select=false)` 排除。

### 更新字段没生效

重点查：

- 实体字段是否 `@TableField(update=false)` 或 `neverUpdate=true`。
- 是否使用 `UpdateChain`：源码注释说明 `@TableField(update=false)` 只对实体类/Model 修改方法生效，对 `UpdateChain` 等不会生效；`neverUpdate` 即使强制也不生效。
- 默认值是否只在 insert/update 时填充，是否设置了 `defaultValueFillAlways` / `updateDefaultValueFillAlways`。

### 逻辑删除/租户条件异常

重点查：

- 实体是否配置对应注解。
- 是否启用了或关闭了相关 Switch/上下文。
- delete 是否为逻辑删除还是物理删除，优先查 `LogicDeleteTestCase.java`。
- 多租户字段是否自动拼入查询/删除条件，优先查 `TenantTestCase.java`。

### 分页 count 不符合预期

重点查：

- 是否使用 Xbatis `paging(IPager)`。
- join/order by 是否被优化移除。
- XML 或 `@Select` 自动分页场景是否正确标注/配置。
- 对 count SQL 有强依赖时查 `PagingTest.java` 和 `XmlPagingTestCase.java`。

## 不要做的事

- 不要把 MyBatis-Plus 的 `LambdaQueryWrapper`、`IService`、`selectOne`、`Page<T>` 语义套到 Xbatis。
- 不要把项目封装的 `BaseMapperPlus` 当作 Xbatis 原生 API。
- 不要手写逻辑删除、租户、乐观锁条件，除非源码和项目配置确认自动能力不适用。
- 不要只凭 README 写复杂功能；高级能力必须读测试用例。
