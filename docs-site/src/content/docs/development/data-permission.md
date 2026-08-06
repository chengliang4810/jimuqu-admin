---
title: 数据权限
description: 按部门和角色控制可见数据范围
---

数据权限用于限制用户能查询到的业务数据范围。它与接口权限不同：`@SaCheckPermission` 决定能否调用接口，数据权限决定调用后能看到哪些记录。

## 支持范围

- 全部数据
- 自定义部门
- 本部门
- 本部门及以下
- 仅本人

## 使用原则

1. 在需要过滤数据的查询入口使用 `@DataPermission`。
2. 明确实体中的部门字段与用户字段映射。
3. 超级管理员绕过过滤，普通角色按数据范围生成条件。
4. 详情、导出和批量操作同样必须应用数据权限，不能只保护列表。

## 注解映射

`@DataPermission` 可以用于类或方法，内部通过 `@DataColumn` 把权限表达式关键字映射到数据库字段：

```java
/** 查询受当前用户数据范围限制的用户列表。 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public Page<SysUserVo> selectPageUserList(SysUserQuery query, PageQuery pageQuery) {
    return userMapper.selectPageUserList(query, pageQuery);
}
```

默认映射是 `deptName -> dept_id`。实际字段必须与查询中的表或别名一致，关联查询时尤其要避免列名歧义。

## 读写边界

数据权限不仅影响列表查询。按 ID 查看、更新、删除和导出前，都应确认目标数据位于当前用户的数据范围内。项目中的 `DataScopeAccess`、`DataScopeRule` 与 `DataScopeWriteRule` 分别承载权限访问、读取规则和写入规则。

:::caution[不要只依赖前端]
菜单隐藏和按钮禁用不是安全边界。数据范围必须由后端查询条件强制执行，并覆盖所有读取与变更入口。
:::

## 验证建议

至少准备管理员、部门负责人和普通员工三类账号，分别验证跨部门列表、详情访问、导出与修改操作；同时检查最终 SQL 是否包含预期的数据范围条件。

更完整的注解、表达式和测试场景见[数据权限使用示例](/reference/data-permission-guide/)与[数据权限测试指南](/reference/data-permission-testing/)。
