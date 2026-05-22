# Solon 源码地图

默认源码目录：`/Users/chengliang/code-repositories/solon`

## 根目录

- `solon/`：核心框架，包含启动、IoC、配置、路由、处理链、AOP 抽象、插件 SPI。
- `solon-projects/`：生态模块，包含 web、server、data、security、serialization、scheduling、view、net、tool 等。一级子项目索引见 `solon-projects.md`，例如 `solon-web.md`、`solon-view.md`。
- `solon-shortcuts/`：快捷依赖聚合。
- `__test/`：大量测试与示例，是写代码时最有价值的用法参照。
- `solon-projects/__doc/`：配置元数据文档。
- `README_CN.md`、`V3.0.md`、`UPDATE_LOG.md`：框架说明与版本信息。

## 核心入口

- `solon/src/main/java/org/noear/solon/Solon.java`：全局入口，`Solon.start(...)`、`Solon.app()`、`Solon.cfg()`、`Solon.context()`。
- `solon/src/main/java/org/noear/solon/SolonApp.java`：应用对象，持有 `AppContext`、`Router`、处理链、渲染/序列化/转换管理器。
- `solon/src/main/java/org/noear/solon/SolonProps.java`：配置加载与访问。
- `solon/src/main/java/org/noear/solon/core/AppContext.java`：应用容器上下文。

## 注解

核心注解在 `solon/src/main/java/org/noear/solon/annotation/`：

- 启动：`@SolonMain`
- 组件：`@Managed`、`@Component`、`@Configuration`、`@Bean`
- 注入：`@Inject`
- 配置绑定：`@BindProps`
- 生命周期：`@Init`、`@Destroy`
- Web：`@Controller`、`@Mapping`、`@Get`、`@Post`、`@Put`、`@Delete`、`@Patch`
- 参数：`@Body`、`@Param`、`@Path`、`@Header`、`@Cookie`
- AOP：`@Around`

注意：源码中大量示例使用 `@Managed`，它是 Solon 管理 Bean 的常见方式；不要机械替换为 Spring 的 `@Component`/`@Service`。

## Web 与路由

- `solon/src/main/java/org/noear/solon/core/route/Router.java`
- `solon/src/main/java/org/noear/solon/core/route/RouterDefault.java`
- `solon/src/main/java/org/noear/solon/core/route/RouterWrapper.java`
- `solon/src/main/java/org/noear/solon/core/handle/Action.java`
- `solon/src/main/java/org/noear/solon/core/handle/ActionLoader.java`
- `solon/src/main/java/org/noear/solon/core/handle/Context.java`
- `solon/src/main/java/org/noear/solon/core/handle/Filter.java`

示例：

- `__test/src/main/java/webapp/App.java`：启动、初始化回调、filter、事件、静态资源、WebSocket 等。
- `__test/src/main/java/webapp/demo2_mvc/`：Controller、Mapping、参数、JSON、上传、跨域等。
- `__test/src/main/java/webapp/demo1_handler/`：Handler/Gateway 示例。

## IoC / AOP / 生命周期

- `solon/src/main/java/org/noear/solon/core/BeanContainer.java`
- `solon/src/main/java/org/noear/solon/core/BeanWrap.java`
- `solon/src/main/java/org/noear/solon/core/BeanInjector.java`
- `solon/src/main/java/org/noear/solon/core/aspect/Interceptor.java`
- `solon/src/main/java/org/noear/solon/core/aspect/MethodInterceptor.java`
- `solon/src/main/java/org/noear/solon/core/aspect/Invocation.java`
- `solon/src/main/java/org/noear/solon/core/event/`：事件定义。

示例：

- `__test/src/main/java/webapp/Config.java`：配置注入、`@Managed` Bean 方法。
- `__test/src/main/java/webapp/demo0_bean/`：Bean 条件、构造、注入示例。
- `__test/src/main/java/webapp/demoa_interceptor/`：Filter 与 Around 示例。

## 插件

- `solon/src/main/java/org/noear/solon/core/Plugin.java`：插件接口，`start(AppContext)`、`preStop()`、`stop()`。
- `solon/src/main/java/org/noear/solon/core/PluginEntity.java`
- `solon/src/main/java/org/noear/solon/core/util/PluginUtil.java`
- 插件声明：各模块 `src/main/resources/META-INF/solon/*.properties`。

典型示例：

- `solon-projects/solon-data/solon-cache/src/main/java/org/noear/solon/data/cache/integration/CachePlugin.java`
- `solon-projects/solon-data/solon-cache/src/main/resources/META-INF/solon/solon-cache.properties`

## 校验

- `solon-projects/solon-security/solon-security-validation/src/main/java/org/noear/solon/validation/`
- 示例：`__test/src/main/java/webapp/demo2_mvc/ValidController.java`

## 文档与配置元数据

- `solon-projects/__doc/solon-configuration-metadata.md`
- `solon-projects/__doc/solon-configuration-metadata.json`
