# solon-view

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-view`

用途：视图渲染与模板引擎适配集合。

## 直接子模块

- `solon-view` — 视图基础模块，提供通用配置和视图路径约定。
- `solon-view-beetl` — Beetl 适配。插件 `org.noear.solon.view.beetl.integration.ViewBeetlPlugin`。
- `solon-view-enjoy` — Enjoy 适配。插件 `org.noear.solon.view.enjoy.integration.ViewEnjoyPlugin`。
- `solon-view-freemarker` — Freemarker 适配。插件 `org.noear.solon.view.freemarker.integration.ViewFreemarkerPlugin`。
- `solon-view-jsp` — JSP 适配。插件 `org.noear.solon.view.jsp.integration.ViewJspPlugin`。
- `solon-view-thymeleaf` — Thymeleaf 适配。插件 `org.noear.solon.view.thymeleaf.integration.ViewThymeleafPlugin`。
- `solon-view-velocity` — Velocity 适配。插件 `org.noear.solon.view.velocity.integration.ViewVelocityPlugin`。

## 关键源码与配置

- `solon-view/src/main/java/org/noear/solon/view/ViewConfig.java`：基础视图配置。
- 默认视图位置：`/templates/`、`/WEB-INF/view/`、`/WEB-INF/templates/`。
- 配置项：`solon.output.meta`、`solon.view.prefix`。
- 各模板插件通过 `context.app().renders().register(...)` 注册渲染器。

## 依赖选择与坑点

- 通常只选择一种模板引擎适配模块，除非明确需要多渲染器并存。
- JSP 还需要匹配 server add-on：如 `solon-server-tomcat-add-jsp`、`solon-server-jetty-add-jsp`、`solon-server-undertow-add-jsp`。
- `solon.view.prefix` 既可能是文件路径，也可能是 classpath/资源路径，具体处理以 `ViewConfig` 源码为准。
