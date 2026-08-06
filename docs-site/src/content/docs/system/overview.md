---
title: 系统功能总览
description: 当前系统模块、入口与权限边界
---

`jimuqu-system` 是当前唯一业务模块，包含认证、用户、组织、权限、配置、文件、消息、日志与监控功能。以下清单来自当前控制器，而不是旧版文档菜单。

| 功能 | 控制器 | 路由前缀 | 主要能力 |
| --- | --- | --- | --- |
| 认证 | `AuthController` | `/auth` | 登录、注册、退出、第三方绑定、权限码 |
| 验证码 | `CaptchaController` | `/auth/code`、`/resource/*/code` | 图片、短信和邮箱验证码 |
| 客户端 | `SysClientController` | `/system/client` | 授权类型、Token 时效、状态管理 |
| 用户 | `SysUserController` | `/system/user` | 用户、角色、岗位、导入导出、解锁 |
| 部门 | `SysDeptController` | `/system/dept` | 树形组织结构与数据范围基础 |
| 岗位 | `SysPostController` | `/system/post` | 岗位、部门树和用户岗位 |
| 角色 | `SysRoleController` | `/system/role` | 菜单权限、数据权限和用户授权 |
| 菜单 | `SysMenuController` | `/system/menu` | 动态路由、菜单树、按钮权限 |
| 字典 | `SysDictTypeController`、`SysDictDataController` | `/system/dict` | 字典类型、数据、缓存与导出 |
| 参数 | `SysConfigController` | `/system/config` | 系统参数与缓存刷新 |
| 公告 | `SysNoticeController` | `/system/notice` | 通知公告维护 |
| 消息 | `SysMessageController` | `/resource/message` | 消息盒子与实时消息入口 |
| 社交账号 | `SysSocialController` | `/system/social` | 当前用户的第三方账号绑定记录 |
| 文件 | `SysFileController` | `/resource/oss` | 上传、下载、查询与删除 |
| 存储配置 | `SysOssConfigController` | `/resource/oss/config` | 存储平台与启停管理 |
| 操作日志 | `SysOperLogController` | `/monitor/operlog` | 查询、导出、删除与清空 |
| 登录日志 | `SysLoginInfoController` | `/monitor/loginInfo` | 查询、导出、解锁与清空 |
| 在线用户 | `SysUserOnlineController` | `/monitor/online` | 会话查询与强制退出 |
| 缓存监控 | `CacheController` | `/monitor/cache` | 缓存统计信息 |
| 定时任务 | `ScheduledJobController` | `/monitor/job` | 任务生命周期、执行与日志 |

:::note[个人中心]
`SysProfileController` 处理当前用户资料与密码修改，路由由控制器方法声明；它属于登录用户自助能力，不应复用管理员用户管理权限。
:::
