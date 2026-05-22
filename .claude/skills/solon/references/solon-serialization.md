# solon-serialization

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-serialization`

用途：Solon 输出渲染与编解码适配集合，覆盖 JSON、XML、Properties、二进制/RPC 序列化格式。

## 直接子模块

- `solon-serialization` — 基础抽象与序列化名称定义。
- `solon-serialization-snack3` / `solon-serialization-snack4` — Snack JSON 适配。
- `solon-serialization-jackson` / `solon-serialization-jackson3` — Jackson JSON 适配。
- `solon-serialization-fastjson` / `solon-serialization-fastjson2` — Fastjson JSON 适配。
- `solon-serialization-gson` — Gson JSON 适配。
- `solon-serialization-jackson-xml` — XML 适配。
- `solon-serialization-properties` — Properties 格式适配。
- `solon-serialization-fury`、`kryo`、`abc`、`hessian`、`protostuff`、`javabin` — 二进制或 RPC 编解码适配。

## 关键源码与 API

- `solon-serialization/src/main/java/org/noear/solon/serialization/SerializerNames.java`：定义 `@json`、`@type_json`、`@xml`、`@type_xml`、`@properties`、`@fury`、`@kryo`、`@abc`、`@hessian`、`@protobuf`。
- `solon-serialization/src/main/java/org/noear/solon/serialization/SerializationConfig.java`：读取 `solon.output.meta`。
- 各具体模块在 `src/main/resources/META-INF/solon/*.properties` 声明插件，多数 priority 为 `18`。
- JSON 插件通常注册 `SerializerNames.AT_JSON` 和 `SerializerNames.AT_JSON_TYPED`。
- `solon-serialization-jackson-xml` 注册 XML / typed XML。
- `solon-serialization-properties` 注册 `@properties`，priority 为 `1`。

## 配置与行为

- `solon.output.meta > 0` 时，序列化输出可带 meta 相关行为。
- 多个 JSON 序列化模块都可能注册 `@json` 和 `@type_json`。

## 依赖选择与坑点

- JSON 实现建议只选一个主实现：Snack、Jackson、Fastjson、Gson 等不要无意识混引。
- 同时引入多个 JSON 插件时，多数 priority 相同，可能出现覆盖或顺序敏感问题。
- XML 选择 `solon-serialization-jackson-xml`。
- Properties 选择 `solon-serialization-properties`。
- RPC/二进制场景按协议选择 Fury/Kryo/ABC/Hessian/Protostuff/JavaBin。
