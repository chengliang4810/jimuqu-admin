---
title: 生产部署
description: 构建、运行与上线检查
---

## 构建

```bash
mvn clean package -Pprod
```

默认跳过测试。如需在发布前执行测试：

```bash
mvn clean package -Pprod -DskipTests=false
```

构建产物位于 `jimuqu-admin/target/jimuqu-admin.jar`。

## 运行

```bash
export JIMU_DB_USERNAME='<数据库账号>'
export JIMU_DB_PASSWORD='<数据库密码>'
java -jar jimuqu-admin.jar --solon.env=prod
```

Windows PowerShell：

```powershell
$env:JIMU_DB_USERNAME = '<数据库账号>'
$env:JIMU_DB_PASSWORD = '<数据库密码>'
java -jar jimuqu-admin.jar --solon.env=prod
```

## 上线检查

- 使用 Java 21 运行，确认没有被系统旧 JDK 接管
- 数据库和 Redis 只授予应用所需的最小权限
- 替换初始化管理员密码、客户端密钥与 RSA 密钥对
- 确认文件存储目录或 MinIO Bucket 可写且具备备份策略
- 通过反向代理启用 HTTPS，并限制内部管理接口暴露范围
- 检查服务日志、健康接口与关键登录流程

## 全栈门禁

仓库提供跨平台验证入口：

```bash
node script/test-fullstack.mjs
```

运行前需设置 `JIMU_TEST_MYSQL_PASSWORD`。脚本会使用独立测试数据库、Redis 前缀和临时 OSS 目录，并在结束后清理本次创建的资源。
