# Web 参数、校验、异常与返回

## 参数绑定

常用注解：

- `@Body`：请求体 JSON/表单对象。
- `@Param`：请求参数。
- `@Path`：路径变量。
- `@Header`：请求头。
- `@Cookie`：Cookie。

示例：

```java
@Mapping("/api/users")
@Controller
public class UserController {
    @Get
    @Mapping("/{id}")
    public User get(@Path Long id) {
        return userService.getById(id);
    }

    @Get
    @Mapping("")
    public List<User> list(@Param("keyword") String keyword) {
        return userService.list(keyword);
    }

    @Post
    @Mapping("")
    public User create(@Body UserCreateBo bo) {
        return userService.create(bo);
    }
}
```

如果参数绑定行为不确定，查：

- `solon/src/main/java/org/noear/solon/core/wrap/ParamWrap.java`
- `solon/src/main/java/org/noear/solon/core/handle/ActionArgumentResolver.java`
- `__test/src/main/java/webapp/demo2_mvc/`

## 校验

校验模块在：`solon-projects/solon-security/solon-security-validation/src/main/java/org/noear/solon/validation/`。

Controller 上常见 `@Valid`，参数或对象上使用校验注解：

```java
@Valid
@Mapping("/api/users")
@Controller
public class UserController {
    @Post
    @Mapping("")
    public User create(@Validated @Body UserCreateBo bo) {
        return userService.create(bo);
    }

    @Get
    @Mapping("/check")
    public String check(@NotEmpty String name) {
        return "OK";
    }
}
```

常见注解可从示例确认：`@NotNull`、`@NotEmpty`、`@NotBlank`、`@Email`、`@Pattern`、`@Min`、`@Max`、`@Size`、`@Length`、`@Validated` 等。

示例：`__test/src/main/java/webapp/demo2_mvc/ValidController.java`。

## 全局异常与状态处理

不要套用 Spring 的 `@ControllerAdvice`。Solon 常见处理点需要查当前项目既有写法或源码 API，例如：

- `SolonApp.onStatus(...)`
- 事件监听异常处理
- 项目自定义 Filter / Render / Handler
- 业务统一返回包装

在写全局异常前，先 grep 当前项目：

```bash
grep -R "onStatus\|Throwable\|Exception\|Validation" src */src/main/java
```

源码相关：

- `solon/src/main/java/org/noear/solon/exception/SolonException.java`
- `solon/src/main/java/org/noear/solon/core/exception/StatusException.java`
- `solon/src/main/java/org/noear/solon/core/handle/Context.java`

## 返回值

Solon Controller 可直接返回对象、字符串或项目包装类型。具体 JSON 序列化由项目引入的 serialization 模块决定。

如果当前项目有统一 `R<T>`、`Result<T>` 或错误码约定，必须沿用项目约定，不要新造返回格式。
