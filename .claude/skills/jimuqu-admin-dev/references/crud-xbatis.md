# CRUD 与 Xbatis 开发规范

## 目录结构

新接口按项目现有分层：

- `domain`：数据库实体类。
- `domain/bo`：业务对象，用于 Controller/Service 入参和校验。
- `domain/vo`：视图对象，用于返回。
- `domain/query`：查询条件对象。
- `mapper`：数据库操作接口。
- `service`：业务接口。
- `service/impl`：业务实现。
- `controller`：接口入口。

优先参考：`docs/通用增删改查示例.md` 和同模块已有代码，例如 `SysConfig`、`SysClient`、`SysFile`、`SysNotice`。

## 实体与 Mapper

- 实体继承 `com.jimuqu.common.mybatis.core.entity.BaseEntity`。
- 使用 Xbatis/AutoTable：`@Table`、`@TableId`、`@AutoColumn`。
- 主键按现有示例使用 `IdAutoType.GENERATOR` 和 `IdentifierGeneratorType.DEFAULT`，除非同模块已有不同规则。
- `Mapper` 继承项目 `BaseMapperPlus<Entity, Vo>`，不要写 XML。
- 单条查询使用 `get()` 或项目 Mapper 封装方法，不要写 `one()`。
- 不要硬编码逻辑删除条件；遵守 Xbatis/项目已有机制。

## Query 与 Service

Query 对象：

- 使用 `@ConditionTarget(Entity.class)`。
- 字段用 `@Condition` 指定 `EQ`、`LIKE` 等条件。
- 需要查询前处理时实现 `ObjectConditionLifeCycle`。

Service 查询常见模式：

```java
private QueryChain<Entity> buildQueryChain(EntityQuery query) {
    return QueryChain.of(mapper)
            .forSearch(true)
            .where(query);
}
```

分页与列表：

```java
return buildQueryChain(query)
        .returnType(EntityVo.class)
        .paging(pageQuery.build());

return buildQueryChain(query)
        .returnType(EntityVo.class)
        .list();
```

新增/更新：

- 使用项目 `MapstructUtil.convert(bo, Entity.class)`。
- 新增成功后把生成主键回写到 BO。
- 业务校验放在 Service，边界校验放在 Controller/BO validation。

## Controller

项目常见写法：

- 类上使用 `@Post`、`@Controller`、`@RequiredArgsConstructor`、`@Mapping("/module/resource")`。
- 查询列表方法使用 `@Get` + `@Mapping("/list")`。
- 新增/更新/删除使用 `@NoRepeatSubmit`、`@SaCheckPermission`、`@Log`。
- 参数校验使用 Solon validation：`@Validated(AddGroup.class)`、`@Validated(UpdateGroup.class)`、`@NotNull`、`@NotEmpty`、`@NotBlank`。
- 返回项目已有类型：`Page<Vo>`、`Vo`、`List<Vo>`、`Long`、`Integer`、`void`，不要新造响应包装。
- 失败断言使用项目 `Assert`。

权限码按模块命名，例如：

- `system:notice:list`
- `system:notice:query`
- `system:notice:add`
- `system:notice:update`
- `system:notice:delete`

## 常见真实场景

- 状态切换、发布、置顶、启停：通常是变更接口，要加权限、操作日志和必要校验。
- 用户/部门隔离：先查 `LoginHelper`、`@DataPermission` 和同类 Service，不要只在 Controller 判断。
- 导出/上传：先查项目文件与 Excel 相关模块，不直接写本地文件。
