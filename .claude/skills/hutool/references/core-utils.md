# Core 常用工具

## 字符串：StrUtil / CharSequenceUtil

源码：

- `hutool-core/src/main/java/cn/hutool/core/util/StrUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/text/CharSequenceUtil.java`

要点：

- `StrUtil` 继承 `CharSequenceUtil` 并实现 `StrPool`。
- `isBlank`：判断 null、空串、空白字符。
- `isEmpty`：判断 null 或长度为 0，不把空白字符串当空。
- `isBlankIfStr(Object)`：对象为字符串时判断 blank；`null` 返回 true；非字符串且非 null 返回 false。
- `isEmptyIfStr(Object)`：对象为字符串时判断 empty；`null` 返回 true；非字符串且非 null 返回 false。
- `utf8Str(Object)` / `str(Object, Charset)`：byte 数组、ByteBuffer、数组等转字符串。

常见用法：

```java
StrUtil.isBlank(name);
StrUtil.isNotBlank(name);
StrUtil.emptyToDefault(value, "default");
StrUtil.format("hello {}", name);
StrUtil.splitTrim(tags, ',');
```

坑点：

- blank 与 empty 不同：`"   "` 是 blank，不是 empty。
- `str(Object, Charset)` 对普通对象最终调用 `toString()`。
- 编码相关转换要显式指定 `CharsetUtil.CHARSET_UTF_8`，避免平台默认字符集差异。

## 对象：ObjectUtil

常见用途：

```java
ObjectUtil.isNull(obj);
ObjectUtil.isNotNull(obj);
ObjectUtil.defaultIfNull(value, defaultValue);
ObjectUtil.defaultIfEmpty(value, defaultValue);
ObjectUtil.equals(a, b);
ObjectUtil.clone(obj);
```

注意：

- `defaultIfEmpty` 的“空”含义要按具体类型理解，涉及集合、字符串时先查源码。
- 克隆能力依赖对象是否支持序列化、Cloneable 或 Hutool 内部策略，复杂对象不要盲目假定深拷贝。

## 数组：ArrayUtil

常见用途：

```java
ArrayUtil.isEmpty(arr);
ArrayUtil.isNotEmpty(arr);
ArrayUtil.contains(arr, value);
ArrayUtil.join(arr, ",");
ArrayUtil.toString(arr);
ArrayUtil.filter(arr, predicate);
```

注意：

- 原始类型数组和对象数组处理不同，Hutool 有 `PrimitiveArrayUtil`。
- 拼接、转换前注意 null 行为。

## 数字：NumberUtil

常见用途：

```java
NumberUtil.add(a, b);
NumberUtil.sub(a, b);
NumberUtil.mul(a, b);
NumberUtil.div(a, b);
NumberUtil.round(value, 2);
NumberUtil.isNumber(str);
```

建议：

- 金额计算优先用 `BigDecimal` 或 Hutool 的精确计算方法，避免 double 误差。
- 除法要明确精度和舍入策略。

## 正则：ReUtil

常见用途：

```java
ReUtil.isMatch(regex, value);
ReUtil.get(regex, content, groupIndex);
ReUtil.findAll(regex, content, groupIndex);
ReUtil.replaceAll(content, regex, replacement);
```

安全注意：

- 不可信正则可能造成 ReDoS，避免把用户输入直接作为复杂正则。
- 大文本匹配要考虑性能。

## 反射：ReflectUtil

常见用途：

```java
ReflectUtil.newInstance(clazz);
ReflectUtil.getFieldValue(obj, "name");
ReflectUtil.setFieldValue(obj, "name", value);
ReflectUtil.invoke(obj, "methodName", args);
```

注意：

- 反射会绕过部分封装边界，业务代码不要对不可信字段名/方法名开放反射能力。
- GraalVM/native image、模块化、强封装环境可能限制反射。

## ID：IdUtil

常见用途：

```java
IdUtil.fastUUID();
IdUtil.simpleUUID();
IdUtil.getSnowflake(workerId, datacenterId).nextId();
```

注意：

- 雪花 ID 需要正确规划 workerId/datacenterId，避免多实例冲突。
- UUID 用途不同：带横线、无横线、快速生成各有取舍。

## 断言：Assert

常见用途：

```java
Assert.notNull(obj, "对象不能为空");
Assert.notBlank(str, "字符串不能为空");
Assert.isTrue(condition, "条件不满足");
```

注意：

- `Assert` 适合内部前置条件，不要滥用来替代业务边界校验和可控错误响应。

## Dict

`Dict` 是 Hutool 的字典结构，常用于动态键值和轻量数据对象：

```java
Dict.create().set("name", "张三").set("age", 18);
```

注意：

- 动态结构缺少类型约束，核心业务对象仍建议使用明确 DTO/Bean。
