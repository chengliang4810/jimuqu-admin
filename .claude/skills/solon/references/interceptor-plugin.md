# Filter、RouterInterceptor、AOP Around 与插件

## 三类拦截机制怎么选

- **Filter**：HTTP 请求处理链级别，适合跨所有请求的 CORS、Header、安全头、原始请求日志等不依赖路由方法元信息的处理。接口：`org.noear.solon.core.handle.Filter`。
- **RouterInterceptor**：路由匹配前后与参数阶段，能拿到 `Handler`、可参与路由调用链，也可在 `postArguments` 里调整参数；适合依赖 Controller 方法注解、路由结果、参数或返回值的鉴权与审计。
- **AOP Around / MethodInterceptor**：Bean 方法调用级别，适合事务、缓存、Service 审计、限流等业务方法横切逻辑。注解：`@Around`，拦截器接口优先按源码使用 `org.noear.solon.core.aspect.MethodInterceptor` 与 `Invocation`。

避免把三者混用成 Spring 模型：不要用 `OncePerRequestFilter`、Spring `HandlerInterceptor`、Spring AOP 或 Spring Boot auto-configuration 替代 Solon 原生机制。

## Filter

源码接口：

```java
@FunctionalInterface
public interface Filter {
    void doFilter(Context ctx, FilterChain chain) throws Throwable;
}
```

注册方式示例：

```java
Solon.start(App.class, args, app -> {
    app.filter((ctx, chain) -> {
        chain.doFilter(ctx);
    });
});
```

示例位置：

- `__test/src/main/java/webapp/App.java`
- `__test/src/main/java/webapp/dso/AppFilterImpl.java`
- `__test/src/main/java/webapp/demoa_interceptor/BeforeHandler.java`

## RouterInterceptor

示例：

```java
@Managed(index = 1)
public class TraceRouterInterceptor implements RouterInterceptor {
    @Override
    public void doIntercept(Context ctx, @Nullable Handler handler, RouterInterceptorChain chain) throws Throwable {
        chain.doIntercept(ctx, handler);
    }
}
```

示例位置：`__test/src/main/java/webapp/dso/RouterInterceptorImpl.java`。

## AOP Around

`@Around` 可放在类或方法上，value 是 `Interceptor` 类型，`index` 控制顺位。

```java
public class AuditInterceptor implements MethodInterceptor {
    @Override
    public Object doIntercept(Invocation inv) throws Throwable {
        return inv.invoke();
    }
}

@Around(value = AuditInterceptor.class, index = 10)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
}
```

示例位置：

- `solon/src/main/java/org/noear/solon/annotation/Around.java`
- `__test/src/main/java/webapp/demoa_interceptor/AroundInterceptor.java`

## 插件开发

插件接口：`solon/src/main/java/org/noear/solon/core/Plugin.java`。

```java
public class DemoPlugin implements Plugin {
    @Override
    public void start(AppContext context) throws Throwable {
        context.app().filter((ctx, chain) -> {
            chain.doFilter(ctx);
        });
    }

    @Override
    public void preStop() throws Throwable {
    }

    @Override
    public void stop() throws Throwable {
    }
}
```

插件声明文件放在：`src/main/resources/META-INF/solon/*.properties`。

```properties
solon.plugin=com.example.DemoPlugin
solon.plugin.priority=30
```

典型源码示例：

- `solon-projects/solon-data/solon-cache/src/main/java/org/noear/solon/data/cache/integration/CachePlugin.java`
- `solon-projects/solon-data/solon-cache/src/main/resources/META-INF/solon/solon-cache.properties`

插件中可使用 `AppContext` 注册 Bean、生命周期、拦截器等。复杂注册前先查现有插件源码。

生命周期与排序要点：

- `start(AppContext context)`：按源码注释是在应用容器启动前执行，可用 `context.app()` 注册 `Filter`、`RouterInterceptor` 等。
- `preStop()`：停止阶段的预停止回调，适合先拒绝新任务或做轻量状态切换。
- `stop()`：停止阶段释放资源。
- `solon.plugin.priority` 数值越大越优先；源码 `PluginEntity.compareTo` 明确“越大越优”。
