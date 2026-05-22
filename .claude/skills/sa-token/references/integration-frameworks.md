# 框架集成：Spring Boot、Solon、WebFlux

## Spring Boot MVC

依赖按版本选择：

```xml
<!-- Spring Boot 2 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.45.0</version>
</dependency>

<!-- Spring Boot 3 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.45.0</version>
</dependency>

<!-- Spring Boot 4 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot4-starter</artifactId>
    <version>1.45.0</version>
</dependency>
```

路由拦截示例：

```java
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            SaRouter.match("/**").notMatch("/user/doLogin").check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
```

关键源码：

- `sa-token-starter/sa-token-spring-boot-starter/src/main/java/cn/dev33/satoken/interceptor/SaInterceptor.java`
- `sa-token-starter/sa-token-spring-boot-starter/src/main/java/cn/dev33/satoken/filter/SaServletFilter.java`
- `sa-token-starter/sa-token-spring-boot-starter/src/main/java/cn/dev33/satoken/spring/SaTokenContextForSpring.java`
- Spring Boot 3/4 Jakarta 对应 `sa-token-spring-boot-webmvc-v3v4-common`。

## Solon

依赖：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-solon-plugin</artifactId>
    <version>1.45.0</version>
</dependency>
```

配置示例：

```yaml
sa-token:
  token-name: satoken
  timeout: 2592000
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: true
```

登录示例：

```java
@Mapping("/user/")
@Controller
public class UserController {
    @Mapping("doLogin")
    public String doLogin(String username, String password) {
        if ("zhang".equals(username) && "123456".equals(password)) {
            StpUtil.login(10001);
            return "登录成功";
        }
        return "登录失败";
    }

    @Mapping("isLogin")
    public String isLogin() {
        return "当前会话是否登录：" + StpUtil.isLogin();
    }
}
```

关键源码：

- `sa-token-starter/sa-token-solon-plugin/src/main/java/cn/dev33/satoken/solon/SaSolonPlugin.java`
  - `start(AppContext context)` 中注册 `SaBeanRegister`、`SaBeanInject`、SSO、OAuth2、API Key、Sign 的 BeanRegister/BeanInject。
- `sa-token-starter/sa-token-solon-plugin/src/main/java/cn/dev33/satoken/solon/SaBeanRegister.java`
  - 注册 `SaTokenConfig`，通过 `@Inject("${sa-token}")` 读取配置。
  - 重写 `SaStrategy.instance.routeMatcher` 为 Solon `PathAnalyzer`。
  - 注册上下文过滤器、CORS 过滤器、防火墙过滤器。
- `sa-token-starter/sa-token-solon-plugin/src/main/java/cn/dev33/satoken/solon/integration/SaTokenFilter.java`
  - Solon 过滤式鉴权，支持静态文件、网关、注解和规则处理。

Solon 注意点：

- 不要照搬 Spring 的 `@RestControllerAdvice`、`WebMvcConfigurer` 写法。
- Solon 下使用 `@Controller`、`@Mapping`、`@Inject`、Solon `Filter`。
- `SaTokenFilter` 和 `SaTokenInterceptor` 二选一，不要同时用。

## WebFlux / Reactor

依赖按版本选择：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-reactor-spring-boot-starter</artifactId>
    <version>1.45.0</version>
</dependency>
```

Spring Boot 3/4 使用对应 `sa-token-reactor-spring-boot3-starter`、`sa-token-reactor-spring-boot4-starter`。

关键源码：

- `sa-token-starter/sa-token-spring-boot-reactor-v2v3v4-common/src/main/java/cn/dev33/satoken/reactor/filter/SaReactorFilter.java`
- `sa-token-starter/sa-token-spring-boot-reactor-v2v3v4-common/src/main/java/cn/dev33/satoken/reactor/spring/SaTokenContextForSpringReactor.java`

WebFlux 注意点：

- 使用 Reactor 上下文适配，不能直接套 Servlet Filter 思路。
- 如果排查上下文丢失，优先查 `SaTokenContextForSpringReactor` 与 filter 是否生效。

## 集成排查优先级

1. 依赖是否选对框架和版本。
2. 配置前缀是否为 `sa-token`，配置键是否与 `SaTokenConfig` 对应。
3. 上下文组件是否被正确注册：Spring/Solon/WebFlux 的 `SaTokenContext` 适配。
4. 路由拦截器或过滤器是否注册，注解鉴权开关是否开启。
5. Token 是否能从 Header/Cookie/Body 读取，`token-name` 是否一致。
6. 异常是否被框架全局异常处理吞掉或转换。
