---
name: solon
description: 辅助开发、迁移、调试和解释 Solon Java 框架项目。只要用户在 Java 后端任务中提到 Solon、Solon Boot、org.noear.solon、@Controller/@Mapping/@Inject/@Managed/@BindProps、Solon 插件、Solon 配置、Solon 路由、Solon 参数绑定/校验、从 Spring Boot 迁移到 Solon，或需要根据本地 Solon 源码确认 API 与机制，都必须使用此技能。此技能会要求优先查证 /Users/chengliang/code-repositories/solon 源码，避免凭 Spring 或通用 Java 框架经验误写。
---

# Solon Dev Skill

你在辅助 Solon 项目开发。Solon 的公开资料少，回答和实现必须以本地源码为准，而不是套用 Spring Boot 习惯。

## 首要原则

1. **先判定任务类型**：业务开发、源码机制解释、迁移改造、问题排查、插件开发、配置/依赖选择。
2. **优先查证本地 Solon 源码**：默认源码目录是 `/Users/chengliang/code-repositories/solon`。如果当前项目依赖版本与源码版本不同，先提醒并以当前项目依赖为准。
3. **不要臆造 API**：不确定的注解、方法、模块名必须 grep/read 源码或项目现有用法后再写。
4. **避免 Spring 惯性**：不要默认使用 `@RestController`、`@Autowired`、`application.yml` 语义、Spring MVC 异常处理、Spring Validator 或 Spring Boot starter，除非当前项目明确兼容或已有封装。
5. **回答要落到代码**：开发类任务优先给可直接放入项目的 Java 代码、依赖坐标、配置片段和验证方式。

## 资料入口

按任务读取对应 reference，必要时再读源码文件：

- 日常业务开发、Controller、DI、配置：`references/core-development.md`
- 参数绑定、校验、异常/返回：`references/web-validation-error.md`
- Filter、RouterInterceptor、AOP Around、插件：`references/interceptor-plugin.md`
- Solon 子项目/插件依赖选择：先读 `references/solon-projects.md`，再按一级子项目读 `references/solon-web.md`、`references/solon-view.md`、`references/solon-data.md`、`references/solon-server.md`、`references/solon-config.md` 等
- 源码查证、模块索引、示例位置：`references/source-map.md`
- Spring Boot 迁移：`references/migration.md`

如果任务很具体，直接 grep 源码优先于泛读 reference。

## 工作流

### Xbatis / AutoTable 项目开发

当用户提到 Xbatis、BaseMapperPlus、QueryChain、AutoTable、通用增删改查示例时：

1. 先查当前项目现有规范与同模块代码；如果当前项目有专属技能或 CLAUDE.md，以项目规范为准。
2. 按项目既有分层说明或实现，不把某个项目的目录、权限码、返回格式推广为通用 Solon 规则。
3. 实体、Mapper、Query、Service 的写法以当前项目已有 Xbatis/AutoTable 示例为准；不要套用 MyBatis-Plus、Spring MVC 或 Spring Validation 习惯。
4. 不确定的 Xbatis 方法必须查项目文档、依赖源码或既有调用；不要臆造 `one()`、逻辑删除条件或 XML 写法。

### 拦截器与插件选择

1. CORS、Header、全站请求预处理优先考虑 `Filter`。
2. 依赖路由匹配、`Handler`、Controller 方法注解、参数阶段或返回阶段的逻辑优先考虑 `RouterInterceptor`，不要一概塞进 `Filter`。
3. Service/Bean 方法审计、事务、缓存、限流等方法级横切逻辑用 `@Around` + `MethodInterceptor`。
4. 插件开发必须查 `Plugin`、`PluginEntity`、`PluginUtil` 源码；`META-INF/solon/*.properties` 使用 `solon.plugin` 和 `solon.plugin.priority`，priority 数值越大越优先。

### 业务开发

1. 查当前项目现有 Solon 写法：Controller、Service、配置、返回包装、异常处理、测试命令。
2. 读取 `references/core-development.md` 和必要的专项 reference。
3. 按当前项目风格改代码；只新增必要文件。
4. 验证：至少运行编译或相关测试；Web/API 变更应说明如何手动验证。

### 源码机制解释

1. 先定位源码：`grep -R "class X\|interface X\|@interface X" /Users/chengliang/code-repositories/solon`。
2. 读取定义和调用链，不要只读注释。
3. 输出结构：结论 → 关键源码路径 → 机制流程 → 使用建议/坑点。

### 问题排查

1. 先收集症状、日志、当前代码和依赖版本。
2. 对照源码确认真实行为，例如扫描范围、路由合并、参数解析、生命周期顺序。
3. 配置文件不生效先查是否缺少对应 `solon-config-*` 扩展；YAML 能力来自 `solon-config-yaml`，不要按 Spring Boot `application.yml` 语义直接判断。
4. 插件未加载先查 `resources/META-INF/solon/*.properties`、`solon.plugin` 全限定类名、classpath/jar、priority 与 `PluginUtil` 扫描逻辑；不要用 `spring.factories`。
5. 路由/注入/校验异常时优先排查是否误用了 Spring 注解，再查 `Solon.start(source, args)` 的启动类 package 扫描范围。
6. 给最小修复，不做无关重构。

### 迁移改造

1. 列出 Spring 概念与 Solon 对应物。
2. 逐项确认 Solon 是否有等价 API；没有则用 Solon 原生模式改写。
3. 优先迁移一条垂直链路，避免一次性大改。

## 常用源码查证命令

```bash
# 找注解或类定义
grep -R "@interface Mapping\|class Solon\|interface Plugin" /Users/chengliang/code-repositories/solon/solon/src/main/java

# 找示例
grep -R "@Valid\|RouterInterceptor\|implements Filter\|implements Plugin" /Users/chengliang/code-repositories/solon/__test/src/main/java

# 找插件声明
find /Users/chengliang/code-repositories/solon/solon-projects -path '*/META-INF/solon/*.properties'
```

## 输出规范

- 用中文说明；Java 标识符保持原样。
- 涉及源码机制时必须引用 `path:line`。
- 涉及版本/API 不确定时，先查证再回答；查不到就明确说“源码中未确认”。
- 代码示例尽量短，贴近 Solon 官方源码和测试示例。
- 给迁移建议时明确“Solon 原生写法”和“不能照搬 Spring 的点”。
