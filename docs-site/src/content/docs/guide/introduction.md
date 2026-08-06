---
title: 项目介绍
description: 了解 Jimuqu Admin 的定位、能力边界与技术选型
---

Jimuqu Admin 是一套基于 **Java 21 + Solon 4** 的企业管理后台基础框架。它提供后台系统普遍需要的认证授权、组织权限、系统配置、审计日志和文件管理能力，让业务项目可以直接从领域功能开始开发。

## 适合谁

- 需要快速搭建企业内部管理系统的开发团队
- 希望使用 Solon、Sa-Token、Xbatis 等国产技术栈的 Java 项目
- 需要完整 RBAC、数据权限和模块化基础设施的中小型系统
- 用于学习现代 Java 多模块工程与后台系统设计

## 核心功能

| 范围 | 已提供能力 |
| --- | --- |
| 身份认证 | 密码、短信、邮箱、社交账号与小程序登录策略 |
| 权限体系 | 用户、角色、菜单、按钮权限、部门数据范围 |
| 系统管理 | 参数、字典、通知公告、客户端与在线用户 |
| 文件与消息 | 本地/MinIO 文件存储、邮件、短信、SSE、WebSocket |
| 稳定性 | 限流、幂等、缓存、接口加解密、数据脱敏 |
| 开发工具 | Xbatis、AutoTable、MapStruct Plus、FastExcel |

## 技术选型

项目以轻量和可维护为优先：Solon 负责应用容器与 Web 能力，Sa-Token 负责认证授权，Xbatis 提供类型安全的链式查询，AutoTable 根据实体维护表结构。

:::note[版本以当前仓库为准]
旧版文档与教程可能仍写有 Java 17、Solon 3 或更早的依赖版本。开发和部署时请以根目录 `pom.xml` 与实际配置文件为准。
:::

## 开源与相关项目

- 后端：[GitHub](https://github.com/chengliang4810/jimuqu-admin) / [Gitee](https://gitee.com/chengliang4810/jimuqu-admin)
- 前端：[Gitee](https://gitee.com/chengliang4810/jimuqu-admin-ui)
- 演示：[admin.jimuqu.com](https://admin.jimuqu.com)
- 协议：MIT License
