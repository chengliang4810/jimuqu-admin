---
title: 项目结构
description: Maven 模块、代码分层与模块职责
---

## Maven 模块

```text
jimuqu-admin/
├── jimuqu-admin/          # 应用入口与运行配置
├── jimuqu-common/         # 通用基础能力
│   ├── jimuqu-common-core
│   ├── jimuqu-common-web
│   ├── jimuqu-common-mybatis
│   ├── jimuqu-common-satoken
│   ├── jimuqu-common-security
│   └── ...
└── jimuqu-modules/
    └── jimuqu-system/     # 用户、角色、菜单等系统业务
```

`jimuqu-admin` 负责组装应用；`jimuqu-common` 提供可复用基础设施；`jimuqu-modules` 承载业务模块。新业务应优先作为独立模块加入 `jimuqu-modules`，避免把领域逻辑堆进启动模块。

## 业务分层

```text
com.jimuqu.[module]/
├── controller/       # 路由、鉴权、校验和响应
├── service/impl/     # 业务规则与事务
├── mapper/           # Xbatis 数据访问
└── domain/
    ├── Entity.java   # 数据库实体
    ├── bo/           # 写入参数
    ├── query/        # 查询条件
    └── vo/           # 返回模型
```

## 调用边界

Controller 只负责协议层工作，Service 承担业务逻辑，Mapper 聚焦查询和持久化。对象转换统一使用 `MapstructUtil.convert()`，接口响应统一使用 `R<T>`。

:::note
实体继承 `BaseEntity` 并使用 `@Table`、`@TableId` 和 `@AutoColumn`；BO 继承 `BoBaseEntity`；VO 实现 `Serializable`。
:::
