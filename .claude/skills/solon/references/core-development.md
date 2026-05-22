# 核心开发模式

## 启动类

Solon 应用常见入口：

```java
@SolonMain
public class App {
    public static void main(String[] args) {
        Solon.start(App.class, args);
    }
}
```

源码入口：`solon/src/main/java/org/noear/solon/Solon.java`。

`Solon.start(source, args, initialize)` 的 `source` 用于确定主应用包与扫描范围。源码会检测启动类不能为 `null`，并且必须有 package。

## Controller 与路由

常见写法：

```java
@Mapping("/api/users")
@Controller
public class UserController {
    @Inject
    private UserService userService;

    @Get
    @Mapping("/{id}")
    public User get(@Path Long id) {
        return userService.getById(id);
    }

    @Post
    @Mapping("")
    public User create(@Body User user) {
        return userService.create(user);
    }
}
```

关键点：

- `@Controller` 标记 Web 控制器，通常与类级 `@Mapping` 配合。
- `@Mapping` 可放类和方法上，支持 `value/path`、`method`、`consumes`、`produces`、`multipart`、`version`、`name`、`description`、`headers`。
- HTTP 方法注解包括 `@Get`、`@Post`、`@Put`、`@Delete`、`@Patch`。
- 路径变量可用 `@Path`，请求体可用 `@Body`，普通参数可用 `@Param` 或按名称绑定。
- `@Mapping` 支持通配：`*` 一段、`**` 多段、`{id}` 路径变量；示例见 `__test/src/main/java/webapp/demo2_mvc/MappingController.java`。

## Bean 与注入

Solon 使用自身 IoC，不要默认写 Spring 注解。

常用注解：

- `@Managed`：托管 Bean；源码示例大量使用。
- `@Component`：组件 Bean。
- `@Configuration`：配置类。
- `@Bean` / `@Managed` 方法：注册方法返回值为 Bean。
- `@Inject`：字段、参数、配置值注入。

示例：

```java
@Managed
public class UserService {
    @Inject
    private UserRepository userRepository;

    @Inject("${app.name}")
    private String appName;
}

@Configuration
public class AppConfig {
    @Managed
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
```

`@Inject` 源码支持：

- `value`：Bean 名或 `${配置表达式}`。
- `required`：默认 `true`。
- `autoRefreshed`：配置注入时有效，适合单例 Bean 的动态刷新配置。

集合配置注入示例见 `__test/src/main/java/webapp/Config.java`。

## 配置绑定

`@BindProps(prefix = "xxx")` 配合 `@Configuration` 或 Bean 方法使用，用于把配置集绑定到对象，也用于 APT 生成配置元信息。

```java
@BindProps(prefix = "database")
@Configuration
public class DatabaseConfig {
    private String url;
    private String username;
    private String password;
}
```

关键点：

- `@BindProps` 是对象属性集绑定，配合 `@Configuration` 或 Bean 方法使用；不要把它当成字段注入注解。
- 单个配置值字段注入使用 `@Inject("${app.name}")` 这类配置表达式。
- 源码里 `AppContext` 对 `@BindProps` 的 `doInject` 标注“不支持注入”，`doFill` 才调用 `cfg().getProp(prefix).bindTo(obj)` 填充对象。

直接读取配置：

```java
String name = Solon.cfg().get("app.name");
int port = Solon.cfg().getInt("server.port", 8080);
boolean debug = Solon.cfg().getBool("debug", false);
```

## 初始化回调

可以在 `Solon.start(..., app -> { ... })` 中注册 filter、事件、静态资源、管理器配置等：

```java
Solon.start(App.class, args, app -> {
    app.filter((ctx, chain) -> {
        chain.doFilter(ctx);
    });

    app.onEvent(AppInitEndEvent.class, event -> {
        // 初始化后动作
    });
});
```

完整示例见 `__test/src/main/java/webapp/App.java`。
