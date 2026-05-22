# 链式 API 与 Mapper

## Mapper 类型

Xbatis core 中存在多层 Mapper 接口：

- `cn.xbatis.core.mybatis.mapper.BaseMapper`：底层动态执行能力，包含 `get`、`exists`、`save`、`update`、`delete`、`list`、`cursor`、`count`、`paging`、`mapWithKey` 等方法。
- `cn.xbatis.core.mybatis.mapper.BasicMapper`：组合基础 get/exists/count/list/cursor/insert/update/delete 等能力。
- `cn.xbatis.core.mybatis.mapper.MybatisMapper`：面向实体的 Mapper，常用于 `QueryChain.of(mapper)`。
- `cn.xbatis.core.mybatis.mapper.mappers.BaseMapper<T>`：泛型 Mapper 基础，提供 `getEntityType()`、`getTableInfo()`、`getBasicMapper()`。

项目里可能封装 `BaseMapperPlus` 等接口；那是项目封装，不是 Xbatis 原生接口。

## QueryChain

源码：`xbatis-core/src/main/java/cn/xbatis/core/sql/executor/chain/QueryChain.java`

构造入口：

```java
QueryChain.of(mybatisMapper)
QueryChain.of(mybatisMapper, where)
QueryChain.of(baseMapper, Entity.class)
QueryChain.of(baseMapper, Entity.class, where)
QueryChain.create().withMapper(mapper)
```

源码提示：`create()` 不是常规入口；使用后后续执行查询前要调用一次 `withMapper(...)`。

常用终止方法：

- `get()`：单条记录。
- `getOptional()`：`Optional<T>`。
- `list()`：列表。
- `cursor()`：游标。
- `count()`：数量。
- `exists()`：是否存在，会 `limit(1)`。
- `paging(P pager)`：分页，`P extends IPager<T>`。
- `mapWithKey(...)`：按字段转 `Map`。
- `returnType(Class<T2>)`：指定返回类型。
- `returnMap()`：返回 `Map`。

示例：

```java
List<SysUser> list = QueryChain.of(sysUserMapper)
        .forSearch()
        .eq(SysUser::getId, 1)
        .like(SysUser::getUserName, " admin ")
        .list();
```

`QueryChain.setDefault(...)` 会在没有 select/from/returnType 时自动设置：

- 非 count 查询默认 select 实体或 returnType 所需字段。
- 没有 from 时使用实体类型。
- 没有 returnType 时使用实体类型。

若 `QueryChain.of(BaseMapper, Class<T>)` 没传 entityType，或者用 `create()` 后没 `withMapper(...)`，可能出现无法确定实体类型的问题。

## UpdateChain / DeleteChain

更新和删除使用对应链：

```java
UpdateChain.of(mapper)
        .set(SysUser::getUserName, "newName")
        .eq(SysUser::getId, 1)
        .update();

DeleteChain.of(mapper)
        .eq(SysUser::getId, 1)
        .delete();
```

具体方法名和 returning 支持必须按源码/测试确认；不要把 MyBatis-Plus 的 `updateWrapper`/`remove` 语义套进来。

## BaseMapper 动态执行能力

`BaseMapper` 源码可确认：

- `get(BaseQuery)` 返回单个对象。
- `list(BaseQuery)` 返回列表。
- `paging(BaseQuery, IPager)` 返回分页对象。
- `mapWithKey(String, BaseQuery)` 返回 Map。
- `updateAndGet(UpdateChain)` / `updateAndList(UpdateChain)` 支持 update returning。
- `deleteAndReturning(DeleteChain)` / `deleteAndReturningList(DeleteChain)` 支持 delete returning。

## 坑点

- Xbatis 的单条查询终止方法是 `get()`，不是 MyBatis-Plus 风格的 `one()`。
- `returnType(Vo.class)` 会影响自动 select；复杂 VO、嵌套 VO 要查 `ResultEntity` / `NestedResultEntity` 示例。
- `mapWithKey` 需要字段名或 GetterFun；value 自动 select 有限制，复杂字段会抛异常。
- count 查询会走专门默认 select count 逻辑；不要自己拼 count SQL，除非是在 SQL 模板/XML 场景。
