# Spring Boot 迁移到 Solon

迁移时不要做关键词级替换。先确认当前项目依赖、统一返回、异常处理、配置文件和 Bean 风格，再按 Solon 原生模式改写。

## 常见概念对应

| Spring Boot | Solon |
|---|---|
| `SpringApplication.run(App.class, args)` | `Solon.start(App.class, args)` |
| `@SpringBootApplication` | `@SolonMain` 加 Solon 扫描/导入配置 |
| `@RestController` | `@Controller`，返回对象由 Solon 渲染/序列化模块处理 |
| `@RequestMapping` | `@Mapping` |
| `@GetMapping` / `@PostMapping` | `@Get` + `@Mapping` / `@Post` + `@Mapping` |
| `@Autowired` | `@Inject` |
| `@Component` / `@Service` | `@Managed` 或 `@Component`，优先沿用项目现有风格 |
| `@ConfigurationProperties` | `@BindProps` + `@Configuration`，或 `@Inject("${xxx}")` |
| `HandlerInterceptor` | `RouterInterceptor` 或 `Filter`，按粒度选择 |
| `OncePerRequestFilter` | `Filter` |
| Spring AOP | `@Around` + `MethodInterceptor` |
| `@ControllerAdvice` | 查项目既有 Solon 异常/状态处理；不要默认照搬 |

## 迁移步骤

1. 先迁移入口和依赖，确保能启动。
2. 迁移一个 Controller 垂直链路：Controller → Service → 配置 → 返回 → 校验。
3. 用 Solon 注解重写路由和参数，不要保留 Spring MVC 注解。
4. 迁移拦截器时先判断语义：请求级用 `Filter`，路由级用 `RouterInterceptor`，方法级用 `@Around`。
5. 迁移配置时确认配置 key 和绑定对象；集合、嵌套对象、动态刷新要读源码或现有示例确认。
6. 迁移异常处理时先 grep 当前项目已有处理方式。

## 常见误区

- `@RestController` 不是 Solon 原生 Controller 写法。
- `@Autowired`、`@Value` 不应直接照搬；使用 `@Inject`。
- `@ConfigurationProperties` 不应照搬；使用 `@BindProps` 或配置表达式注入。
- Spring 的 `HandlerInterceptor`、`FilterRegistrationBean`、`ControllerAdvice` 不能默认套用。
- 不要假设 starter 名称；Solon 模块在 `solon-projects/` 下，依赖坐标应查当前项目或源码 POM。

## 输出迁移建议时的格式

1. 先给“Solon 原生写法”代码。
2. 列出“不能照搬 Spring 的点”。
3. 给最小验证命令或手动请求示例。
