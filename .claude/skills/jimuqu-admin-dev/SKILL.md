---
name: jimuqu-admin-dev
description: 辅助开发、调试和解释 jimuqu-admin 项目。只要当前仓库是 jimuqu-admin，或用户提到 Xbatis、BaseMapperPlus、QueryChain、AutoTable、Sa-Token、数据权限、文件上传、操作日志、限流、SSE、通用增删改查示例、项目新接口开发，都必须使用此技能。此技能会要求先查项目文档和现有模块，并按项目规范落地；底层 Solon API 仍需联动 solon 查证。
---

# jimuqu-admin Dev Skill

你在辅助 jimuqu-admin 项目开发。此技能只记录当前项目的开发规范和项目级能力，不把这些规则写入通用 `solon`。

## 首要原则

1. **先查项目现有规范**：优先读取 `docs/通用增删改查示例.md`、`docs/xbatis文档.md`、`docs/文件上传示例.md` 和同模块既有代码。
2. **项目规范优先**：新接口按项目分层、权限、日志、校验、Xbatis、AutoTable、文件存储等现有模式实现。
3. **联动底层技能**：涉及 Solon 框架机制时使用 `solon`。不要把通用框架问题只按项目经验回答。
4. **少造轮子**：限流、SSE、WebSocket、操作日志、Excel、文件记录、数据权限等优先复用项目已有模块。
5. **只做必要改动**：按用户需求实现，不顺手重构无关模块。

## 资料入口

按任务读取对应 reference：

- 新接口 CRUD、Xbatis、Controller/Service 分层：`references/crud-xbatis.md`
- 项目能力与模块复用：`references/project-capabilities.md`

常用项目文档：

- `docs/通用增删改查示例.md`
- `docs/xbatis文档.md`
- `docs/文件上传示例.md`
- `docs/数据权限使用示例.md`
- `docs/限流模块使用指南.md`

## 工作流

### 新接口开发

1. 先找同模块相似实体、BO、VO、Query、Mapper、Service、Controller。
2. 按 `domain`、`domain/bo`、`domain/vo`、`domain/query`、`mapper`、`service`、`service/impl`、`controller` 分层实现。
3. 实体继承 `BaseEntity`，使用 Xbatis/AutoTable 注解；Mapper 继承 `BaseMapperPlus`，不写 XML。
4. 查询使用 `QueryChain` 和 Query 对象；分页返回项目 `Page<T>`。
5. Controller 使用项目权限码、`@Log`、`@NoRepeatSubmit`、Solon validation；不新造响应格式。
6. 编译或运行相关测试；API 变更给出手动验证路径。

### 项目能力扩展

1. 先查是否已有公共模块实现。
2. 能注解解决就不要新增拦截器；能复用 Service/Util 就不要新增平行实现。
3. 横切能力要判断用 Controller 注解、Filter、RouterInterceptor、AOP Around 还是 Solon Plugin；底层选择查 `solon`。
4. 文件或图像保存必须使用 x-file-storage，不直接写本地路径。

## 输出规范

- 全程中文；Java 标识符保持原样。
- 涉及项目代码时引用 `path:line`。
- 不确定的项目能力先查文件，不臆造类名、工具类或注解。
- 涉及 Solon API 不确定时，联动对应通用技能和本地源码查证。
