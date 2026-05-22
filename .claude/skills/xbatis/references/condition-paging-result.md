# 条件对象、分页与返回映射

## 条件对象

`@Condition` 定义在 `xbatis-annotation/src/main/java/cn/xbatis/db/annotations/Condition.java`，可标在字段上。

常用属性：

- `value`：条件类型，默认 `EQ`。
- `target`：目标实体类；如果类上指定了 `@ConditionTarget`，字段可不写。
- `property`：目标属性名。
- `storey`：存储层级，默认 -1；相同实体类继承上层 storey，否则默认 1。
- `likeMode`：LIKE 模式，默认 `%xx%`。
- `toEndDayTime`：只支持 `LTE` 和 `BETWEEN` 的第 2 个参数；支持 LocalDate/Date/String/Long/LocalDateTime。
- `defaultValue`：支持基本类型默认值和动态默认值，例如 `{NOW}`、`{TODAY}`。
- `cast`：是否转换成实体类字段类型。

条件类型包括：

- `EQ`、`NE`
- `IN`、`NOT_IN`
- `LT`、`LTE`、`GT`、`GTE`
- `LIKE`、`NOT_LIKE`、`ILIKE`、`NOT_ILIKE`
- `BETWEEN`
- `NULL`、`NOT_NULL`
- `BLANK`、`NOT_BLANK`
- `IGNORE`

`@ConditionTarget` 定义在 `ConditionTarget.java`：

- `value`：目标实体类。
- `storey`：存储层级，默认 1。
- `logic`：逻辑符号，默认 `Logic.AND`。

示例：

```java
@ConditionTarget(SysUser.class)
public class SysUserQuery implements ObjectConditionLifeCycle {
    @Condition(Condition.Type.EQ)
    private Long id;

    @Condition(value = Condition.Type.LIKE, likeMode = Condition.LikeMode.DEFAULT)
    private String userName;

    @Override
    public void beforeBuildCondition() {
    }
}
```

## ObjectConditionLifeCycle

定义在 `xbatis-core/src/main/java/cn/xbatis/core/sql/ObjectConditionLifeCycle.java`：

- `beforeBuildCondition()`：条件构建前执行。
- `afterBuildCondition(ConditionChain conditionChain)`：条件构建后执行。

适合做查询对象内部的条件预处理或补充，不要把 Controller/业务权限逻辑硬塞进通用 Query 对象，除非项目规范明确这么做。

## 分页

分页终止方法：`QueryChain.paging(P pager)`，其中 `P extends IPager<T>`。

`BaseMapper` 也有：

```java
<T, P extends IPager<T>> P paging(BaseQuery<? extends BaseQuery, T> query, P pager);
```

不同项目会封装自己的 Page/PageQuery；通用 Xbatis 只要求分页对象实现/适配 `IPager`。

README 提到 Xbatis 会优化分页 count：去除无效 order by、无效 left join，并替换 select 为 count 形式。具体行为以 `PagingTest.java`、`XmlPagingTestCase.java` 和源码为准。

## 返回映射

- `returnType(Vo.class)`：指定查询返回类型。
- `@ResultEntity`：声明 VO 对应实体。
- `@ResultEntityField` / `@ResultField`：字段映射。
- `@NestedResultEntity`：嵌套结果映射。
- `returnMap()`：返回 Map。
- `mapWithKey(...)`：把查询结果按指定 key 转 Map。

README 说明 Xbatis 支持多层嵌套 VO 自动映射以及自动 select 所需列；实际复杂映射优先查 `NestedTestCase.java`、`OneToManyTest.java`。

## 坑点

- Query 对象的 `target`、`property`、`storey` 写错会导致条件绑定到错误表/层级。
- `toEndDayTime` 不是所有条件都支持，只支持 `LTE` 和 `BETWEEN` 的第二个参数。
- 分页对象不是固定类；不要臆造项目不存在的 `PageQuery`。
- 复杂 VO 自动 select 有边界；遇到嵌套、聚合、计算字段必须查测试示例。
