# Convert、Bean、集合与 Map

## Convert 类型转换

源码：

- `hutool-core/src/main/java/cn/hutool/core/convert/Convert.java`
- `hutool-core/src/main/java/cn/hutool/core/convert/ConverterRegistry.java`
- `hutool-core/src/main/java/cn/hutool/core/convert/impl/*Converter.java`

常见用法：

```java
String str = Convert.toStr(value);
Integer i = Convert.toInt(value, 0);
Long l = Convert.toLong(value);
Boolean b = Convert.toBool(value, false);
Date date = Convert.toDate(value);
List<String> list = Convert.toList(String.class, value);
```

源码行为要点：

- 很多 `toXxx(value, defaultValue)` 内部走 `convertQuietly`，转换失败返回默认值，不抛错。
- 无默认值重载通常默认值为 null。
- 支持数组、集合、Map、Bean、枚举、时间、URI/URL/UUID 等转换。
- 泛型复杂场景使用 `TypeReference`。

示例：

```java
List<Long> ids = Convert.convert(new TypeReference<List<Long>>() {}, source);
```

坑点：

- “转换失败静默返回默认值”适合容错，不适合必须严格报错的业务边界。
- 数字、日期、布尔转换要确认输入格式，避免把脏数据吞掉。
- 自定义转换器要查 `ConverterRegistry`。

## BeanUtil

源码：

- `hutool-core/src/main/java/cn/hutool/core/bean/BeanUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/bean/copier/BeanCopier.java`
- `hutool-core/src/main/java/cn/hutool/core/bean/copier/CopyOptions.java`

常见用法：

```java
UserVo vo = BeanUtil.copyProperties(user, UserVo.class);
BeanUtil.copyProperties(source, target);
Map<String, Object> map = BeanUtil.beanToMap(bean);
User user = BeanUtil.toBean(map, User.class);
Object value = BeanUtil.getProperty(bean, "name");
BeanUtil.setProperty(bean, "name", "张三");
```

Bean 判定：

- `isBean(Class<?>)`：有 setter 或 public 字段。
- `isReadableBean(Class<?>)`：有 getter/is 方法或 public 字段。
- `Dict.class` 在 `hasSetter` 中被排除。

CopyOptions 常见配置：

```java
CopyOptions options = CopyOptions.create()
    .setIgnoreNullValue(true)
    .setIgnoreError(true)
    .setIgnoreCase(true);
BeanUtil.copyProperties(source, target, options);
```

坑点：

- 默认是否忽略 null、是否忽略错误、是否忽略大小写，要按具体重载和 `CopyOptions` 确认。
- Bean 拷贝不是业务校验；字段名不一致时需要配置映射或手动处理。
- 嵌套对象和集合的深拷贝不要想当然，先查 `BeanCopier` 实现或写测试。
- 反射属性读写不要暴露给不可信字段名。

## CollUtil

常见用法：

```java
CollUtil.isEmpty(list);
CollUtil.isNotEmpty(list);
CollUtil.newArrayList("a", "b");
CollUtil.join(list, ",");
CollUtil.distinct(list);
CollUtil.filter(list, predicate);
```

注意：

- 区分“返回新集合”还是“修改原集合”，具体方法要查源码。
- 对大集合执行去重、过滤、笛卡尔积、排列组合等操作要考虑内存和复杂度。

## MapUtil

常见用法：

```java
MapUtil.isEmpty(map);
MapUtil.isNotEmpty(map);
Map<String, Object> map = MapUtil.of("key", value);
MapUtil.getStr(map, "name");
MapUtil.getInt(map, "age");
```

Hutool 还提供多种 Map 实现和封装：

- 大小写不敏感 Map。
- TableMap。
- WeakMap。
- MapWrapper。

注意：

- `getStr/getInt` 等会做转换，转换失败/缺失值行为要按源码确认。
- 不要用 Map 替代所有 DTO；复杂业务参数建议用明确 Bean。

## Bean / Map / JSON 转换关系

常见链路：

```java
Map<String, Object> map = BeanUtil.beanToMap(bean);
User user = BeanUtil.toBean(map, User.class);
JSONObject obj = JSONUtil.parseObj(bean);
User user2 = JSONUtil.toBean(obj, User.class);
```

建议：

- Bean 与 Map 之间用 `BeanUtil`。
- JSON 字符串/JSON 对象与 Bean 之间用 `JSONUtil`。
- 复杂泛型集合用 `TypeReference` 或明确类型方法。

## 常见排查

- 字段没有拷贝：检查 getter/setter、字段名、大小写、是否 public、是否 static、CopyOptions。
- null 覆盖了目标值：使用 `setIgnoreNullValue(true)`。
- 字符串转数字失败但没报错：检查是否使用了带默认值或 quiet 转换。
- Map key 大小写问题：检查是否使用忽略大小写 Map 或 CopyOptions。
