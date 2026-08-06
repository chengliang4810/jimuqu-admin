---
title: 快速开始
description: 准备环境并运行 Jimuqu Admin
---

## 环境要求

| 软件 | 要求 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9 或更高版本 |
| MySQL | 8.x |
| Redis | 6.x 或更高版本 |

## 获取后端代码

```bash
git clone https://github.com/chengliang4810/jimuqu-admin.git
cd jimuqu-admin
```

## 启动 MySQL 与 Redis

如果本机已经安装 MySQL 8 和 Redis，可以直接使用现有服务。全新开发环境也可以使用 Docker 启动与默认开发配置匹配的实例：

```bash
docker run --name jimuqu-mysql \
  -e MYSQL_ROOT_PASSWORD='P@ssw0rd' \
  -e MYSQL_DATABASE='jimuqu_db' \
  -p 3306:3306 \
  -d mysql:8.4

docker run --name jimuqu-redis \
  -p 6379:6379 \
  -d redis:7-alpine
```

Windows PowerShell 需要把命令写成单行：

```powershell
docker run --name jimuqu-mysql -e MYSQL_ROOT_PASSWORD='P@ssw0rd' -e MYSQL_DATABASE='jimuqu_db' -p 3306:3306 -d mysql:8.4
docker run --name jimuqu-redis -p 6379:6379 -d redis:7-alpine
```

等待 MySQL 完成初始化后再启动后端：

```bash
docker logs -f jimuqu-mysql
```

日志出现 `ready for connections` 后按 `Ctrl+C` 退出日志查看。这里的密码只用于本机开发容器，不能用于测试、预发布或生产环境。

## 配置后端数据库

开发配置位于 `jimuqu-admin/src/main/resources/app-dev.yml`，默认连接：

```yaml
jdbcUrl: jdbc:p6spy:mysql://localhost:3306/jimuqu_db
username: root
password: P@ssw0rd
```

使用自行安装的 MySQL 时，请先创建 `jimuqu_db`，并把该文件中的地址、用户名和密码改为本机开发数据库信息。不要把个人或生产凭据提交到 Git。

```sql
CREATE DATABASE IF NOT EXISTS jimuqu_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

首次连接空数据库时，AutoTable 会根据实体创建表，并从 `classpath:sql/MySQL/jimuqu.sql` 写入初始化数据。已有业务数据的数据库不会重复写入种子数据。

## 启动后端

推荐先完成一次完整构建，再运行生成的 JAR：

```bash
mvn clean package -Pdev
java -jar jimuqu-admin/target/jimuqu-admin.jar --solon.env=dev
```

需要使用 Maven 开发启动时，在依赖已经完成构建后执行：

```bash
mvn -pl jimuqu-admin solon:run -Pdev
```

服务启动后默认监听 [http://127.0.0.1:5320](http://127.0.0.1:5320)。保持这个终端运行，再打开一个新终端启动前端。

## 启动前端

前端是独立仓库，要求 Node.js `^22.18.0 || ^24.0.0` 和 pnpm `11.2.2`。

### 1. 获取前端代码

```bash
git clone https://github.com/chengliang4810/jimuqu-admin-ui.git
cd jimuqu-admin-ui
```

Gitee 镜像：

```bash
git clone https://gitee.com/chengliang4810/jimuqu-admin-ui.git
cd jimuqu-admin-ui
```

### 2. 准备 pnpm

先检查 Node.js 版本：

```bash
node --version
```

使用 Node.js 自带的 Corepack 安装项目锁定的 pnpm：

```bash
corepack enable
corepack prepare pnpm@11.2.2 --activate
pnpm --version
```

如果当前 Node.js 发行包没有提供 Corepack，可以改用：

```bash
npm install --global pnpm@11.2.2
```

### 3. 安装依赖

```bash
pnpm install --frozen-lockfile
```

必须在仓库根目录执行。项目使用 pnpm Workspace 和 catalog 统一依赖版本，不支持 npm 或 yarn 安装。

### 4. 确认开发代理

当前 `.env.development` 已配置：

```dotenv
VITE_PORT=5666
VITE_BASE=/
VITE_GLOB_API_URL=/dev-api
```

`vite.config.ts` 会把 `/dev-api` 请求代理到 `http://127.0.0.1:5320`，并在转发时移除 `/dev-api` 前缀。默认本地联调无需修改 API 地址。

如果后端不在本机 `5320` 端口，需要修改 `vite.config.ts` 中的代理目标后重新启动前端开发服务器。

### 5. 启动前端

```bash
pnpm dev
```

启动成功后访问 [http://127.0.0.1:5666](http://127.0.0.1:5666)。前端终端和后端终端都必须保持运行。

## 验证前后端联通

按以下顺序检查：

1. 后端终端没有数据库、Redis 或端口占用错误。
2. 浏览器能够打开 `http://127.0.0.1:5666`。
3. 登录页请求 `/dev-api/auth/code` 时没有代理连接失败。
4. 登录后能够正常请求用户信息、权限码和动态菜单。

开发环境默认关闭图片验证码与接口加密，便于本地联调；生产环境仍会启用相应安全能力。

## 常用前端命令

```bash
# TypeScript 类型检查
pnpm typecheck

# 单元测试
pnpm test:unit

# 代码检查
pnpm lint

# 生产构建
pnpm build

# 本地预览生产构建
pnpm preview
```

端到端测试不要直接在前端仓库运行 `pnpm test:e2e`。项目支持的完整入口位于后端仓库：

```bash
node script/test-fullstack.mjs
```

## 停止本地服务

前端和后端终端分别按 `Ctrl+C`。如果使用了上面的 Docker 容器：

```bash
docker stop jimuqu-mysql jimuqu-redis
```

再次开发时使用：

```bash
docker start jimuqu-mysql jimuqu-redis
```

## 下一步

1. 阅读[配置说明](/guide/configuration/)，了解环境变量与公共配置。
2. 阅读[项目结构](/development/architecture/)，确认模块职责。
3. 按照[CRUD 开发](/development/crud/)新增第一个业务接口。

:::caution[首次上线]
初始化账号、客户端密钥和前后端 RSA 密钥只适合首次启动。任何可访问公网的环境都必须在上线前替换。
:::
