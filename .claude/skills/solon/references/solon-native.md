# solon-native

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-native`

用途：GraalVM/native-image AOT 元信息生成支持。

## 直接子模块

- `solon-aot` — AOT/native 支撑库，包含 native metadata、hint、processor 等。

## 关键源码与 API

- `solon-aot/src/main/java/org/noear/solon/aot/RuntimeNativeRegistrar.java`：AOT 阶段注册 native 运行时元信息的扩展点。
- 未发现普通运行时 `META-INF/solon/*.properties` 自动插件入口。

## 依赖选择与坑点

- 不要当作普通运行时 starter 引入。
- 仅在构建 native-image 或插件需要注册反射、资源、JDK proxy hint 时使用。
