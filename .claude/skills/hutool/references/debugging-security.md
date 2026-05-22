# 排查与安全注意

## API 找不到或行为不一致

优先检查：

1. Hutool 版本：本地源码为 `5.8.44`，项目可能不是此版本。
2. 模块是否引入：`hutool-core` 不包含 JSON、HTTP、Crypto、POI、JWT 等扩展模块。
3. 是否使用了旧版本 API：Hutool 方法有弃用和迁移，按当前源码确认。
4. IDE 自动导包是否导错，例如同名 JSON、HTTP、Date 工具类。
5. 是否使用 `hutool-all` 与单模块版本混搭导致依赖冲突。

## 空值行为

Hutool 很多工具对 null 友好，但不代表所有方法都返回默认值。

排查方式：

- 查源码：null 是返回 null、空集合、false，还是抛异常。
- 查测试：`src/test/java` 通常有边界样例。
- 对业务边界，不要完全依赖工具类静默容错；该报错就报错。

典型例子：

- `StrUtil.isBlank(null)` 为 true。
- `StrUtil.isEmpty("   ")` 为 false。
- `Convert.toInt(value, 0)` 转换失败返回 0。
- `FileUtil.ls(null)` 返回 null；非目录抛 `IORuntimeException`。

## 日期时间问题

常见原因：

- 秒级/毫秒级时间戳混淆：`DateUtil.date(long)` 是毫秒级。
- 默认时区差异导致日期偏移。
- 自动解析格式不符合输入。
- `Date`、`LocalDateTime`、`Instant` 互转时 ZoneId 不明确。

建议：

- 外部输入显式指定日期格式。
- API 输出统一时区和格式。
- 新代码优先使用 `java.time` + `LocalDateTimeUtil`。

## Bean 拷贝问题

常见原因：

- getter/setter 不符合 JavaBean 规范。
- 字段名大小写不一致。
- null 覆盖目标值。
- 类型转换失败。
- 嵌套对象/集合不是深拷贝。

建议：

```java
CopyOptions options = CopyOptions.create()
    .setIgnoreNullValue(true)
    .setIgnoreError(false);
BeanUtil.copyProperties(source, target, options);
```

需要严格转换时不要设置忽略错误。

## JSON 问题

常见原因：

- JSON 字符串与 Bean/Map 转换路径不同。
- `ignoreNullValue` 对 JSON 字符串本身不改变。
- 泛型集合类型丢失。
- 日期格式和时区不符合预期。
- 大 JSON 输入导致内存压力。

建议：

- 明确使用 `JSONConfig`。
- 对外部 JSON 做大小限制。
- 复杂泛型转换写测试确认。

## HTTP 问题

常见原因：

- URL 编码/解码策略不一致。
- 没设置 timeout。
- 全局 CookieManager 导致请求串 Cookie。
- HTTPS 证书或代理配置问题。
- 服务端返回压缩/编码和客户端解析不一致。

安全注意：

- 用户可控 URL 必须防 SSRF。
- 禁止访问内网、云 metadata、localhost 等敏感地址。
- 不要在 URL 中传 token/secret。
- 下载文件限制大小和落盘路径。

## Crypto/JWT 安全

- MD5/SHA1 不适合密码存储。
- JWT payload 不是加密，不要放敏感信息。
- 加密必须明确算法、模式、填充、IV、字符集和秘钥管理。
- 生产不要使用示例秘钥或硬编码秘钥。
- 校验 JWT 不只校验签名，还要校验过期和业务 claims。

## Excel 安全

导入：

- 限制文件大小、行数、列数和 sheet 数。
- 不要信任单元格类型，读取后做业务校验。
- 大文件流式处理。

导出：

- 防公式注入：用户可控文本如果以 `=`, `+`, `-`, `@` 开头，导出前转义。
- 及时关闭 writer。

## 反射与脚本

- `ReflectUtil` 不要对外暴露任意类名、方法名、字段名调用。
- `ScriptUtil` 不要执行不可信脚本。
- 动态编译/脚本能力应默认视作高风险能力，需要沙箱和权限边界。

## 依赖选择建议

- 只用字符串、日期、Bean、文件：`hutool-core`。
- 只用 JSON：`hutool-json`。
- 只用 HTTP：`hutool-http`。
- 只用加密：`hutool-crypto`。
- 只用 Excel：`hutool-poi`。
- 工具脚手架或内部应用不敏感：`hutool-all`。
