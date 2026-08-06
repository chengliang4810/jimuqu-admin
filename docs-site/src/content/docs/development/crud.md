---
title: CRUD 开发
description: 按项目约定实现一个完整业务接口
---

## 开发顺序

1. 定义 Entity、BO、Query 与 VO。
2. 创建继承 `BaseMapperPlus<Entity, Vo>` 的 Mapper。
3. 定义 Service 接口与实现。
4. 创建 Controller，补充权限、日志和参数校验。
5. 启动应用，让 AutoTable 维护表结构。
6. 为核心业务规则添加测试。

## 实体

```java
@Table(value = "biz_demo")
public class BizDemo extends BaseEntity {
    /** 示例主键。 */
    @TableId(value = IdAutoType.GENERATOR,
        generatorName = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "示例主键")
    private Long id;

    /** 示例名称。 */
    @AutoColumn(comment = "示例名称", notNull = true, length = 100)
    private String name;
}
```

## Mapper

```java
@Mapper
public interface BizDemoMapper extends BaseMapperPlus<BizDemo, BizDemoVo> {
    /** 按名称查询单条数据。 */
    default BizDemo getByName(String name) {
        return QueryChain.of(this)
            .where(where -> where.eq(BizDemo::getName, name))
            .get();
    }
}
```

查询单条数据使用 `get()`，不要使用不存在的 `one()`。逻辑删除条件由 Xbatis 自动处理，不要硬编码 `del_flag = '0'`。

## Controller

```java
@Controller
@Mapping("/demo")
public class BizDemoController {
    /** 新增示例数据。 */
    @Post
    @SaCheckPermission("demo:add")
    @Log(title = "示例管理", businessType = BusinessType.ADD)
    public R<Void> add(@Validated(AddGroup.class) BizDemoBo bo) {
        return R.ok();
    }
}
```

完整的现有实现可参考系统参数模块 `SysConfigController`、`SysConfigServiceImpl` 与相关领域对象。
