---
title: 配置说明
description: 环境、数据库、Redis 与服务配置
---

## 配置文件

配置位于 `jimuqu-admin/src/main/resources/`：

- `app.yml`：公共配置、服务端口与模块开关
- `app-dev.yml`：开发环境数据源
- `app-prod.yml`：生产环境数据源
- `config/*.yml`：按能力拆分的补充配置

Maven 默认激活 `dev` Profile。运行 JAR 时可通过 `--solon.env=prod` 显式切换生产环境。

## 服务端口

```yaml
server.port: 5320
```

## 生产数据库环境变量

| 环境变量 | 是否必填 | 说明 |
| --- | --- | --- |
| `JIMU_DB_USERNAME` | 是 | MySQL 用户名 |
| `JIMU_DB_PASSWORD` | 是 | MySQL 密码 |
| `JIMU_DB_URL` | 否 | 完整 JDBC URL，默认连接本机 `jimuqu_db` |

## Redis 环境变量

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `JIMU_REDIS_SERVER` | `127.0.0.1:6379` | Redis 地址 |
| `JIMU_REDIS_DB` | `0` | Redis 数据库编号 |
| `JIMU_REDIS_PASSWORD` | 空 | Redis 密码 |
| `JIMU_REDIS_PREFIX` | `jimuqu` | 缓存键前缀 |

:::tip[配置原则]
开发环境可以使用本地配置；生产凭据应通过环境变量或密钥管理系统注入，不要提交到 Git。
:::

## 数据库初始化

目标数据库可以不存在，也可以是预先创建的空库。若允许应用自动建库，需要给数据库账号相应权限。生产环境建议先完成备份与权限评审，再启用 AutoTable 结构维护。
