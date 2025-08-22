# QWEN.md - Jimuqu Admin 项目上下文

## 项目概述

**Jimuqu Admin** 是一个基于 **Solon** 框架的轻量级管理系统框架，是一个中文开源项目。该项目提供了完整的企业级管理后台系统，包含用户管理、权限管理、部门管理、代码生成等核心功能模块。

### 核心特点
- **国产化技术栈**：深度集成 Solon、Hutool、Sa-Token 等国产框架
- **轻量高效**：启动快、资源占用低、响应速度优异
- **开箱即用**：提供完整的管理系统基础功能模块
- **自动数据库管理**：使用 AutoTable 自动维护数据库表结构
- **多模块架构**：清晰的模块化设计，便于扩展和维护

### 技术栈
- **框架**：Solon 3.4.3 (轻量级 Java 应用框架)
- **数据库**：MySQL 8.4.0 / SQLite 3.47.2.0
- **连接池**：HikariCP 7.0.1
- **ORM**：Xbatis 1.8.9 (MyBatis 增强工具)
- **权限认证**：Sa-Token 1.44.0
- **工具库**：Hutool 7.0.0-M1
- **对象映射**：MapStruct Plus 1.5.0
- **Excel处理**：FastExcel 1.2.0
- **代码生成**：Velocity 2.3 / Beetl 3.19.0
- **文件存储**：X-File-Storage 2.3.0 + MinIO 8.5.2
- **第三方登录**：JustAuth 1.16.6

## 项目结构

### 多模块 Maven 架构
```
jimuqu-admin/
├── jimuqu-admin/          # 主应用程序入口
├── jimuqu-common/         # 通用工具和配置
│   ├── jimuqu-common-bom/      # 依赖管理
│   ├── jimuqu-common-core/     # 核心工具、常量、基类
│   ├── jimuqu-common-web/      # Web层配置
│   ├── jimuqu-common-mybatis/  # 数据库层
│   ├── jimuqu-common-satoken/ # 身份验证和授权
│   ├── jimuqu-common-security/ # 安全配置
│   ├── jimuqu-common-log/      # 操作日志
│   ├── jimuqu-common-excel/    # Excel导入导出
│   ├── jimuqu-common-cache/    # 缓存层
│   ├── jimuqu-common-oss/      # 对象存储服务
│   ├── jimuqu-common-social/   # 第三方认证
│   └── jimuqu-common-doc/      # 文档处理
└── jimuqu-modules/        # 业务模块
    ├── jimuqu-system/        # 核心系统功能
    └── jimuqu-generator/     # 代码生成工具
```

### 核心功能模块
- **用户管理**：用户增删改查，分配部门/角色/岗位
- **部门管理**：树形组织结构管理，支持数据权限
- **角色管理**：分配角色菜单权限，按机构划分数据范围
- **菜单管理**：配置系统菜单、操作权限、按钮权限标识
- **字典管理**：维护系统固定数据（如状态/类型等）
- **参数管理**：动态配置系统参数
- **文件管理**：文件展示/上传/下载/删除
- **定时任务**：任务管理、日志管理、执行器监控
- **代码生成**：多数据源代码生成（Java/HTML/XML/SQL）
- **操作日志**：记录系统操作日志和异常信息
- **在线用户**：监控在线用户并支持强制踢出

## 构建和运行

### 环境要求
- **Java**: 17+
- **Maven**: 3.6+
- **数据库**: MySQL 8.0+ 或 SQLite

### Maven 命令
```bash
# 构建整个项目
mvn clean package

# 使用指定环境构建 (dev/prod)
mvn clean package -Pdev
mvn clean package -Pprod

# 构建时跳过测试 (默认行为)
mvn clean package -DskipTests

# 显式运行测试
mvn test
```

### 应用启动
```bash
# 启动应用 (默认使用 dev 环境)
java -jar jimuqu-admin/target/jimuqu.jar

# 使用指定环境启动
java -jar jimuqu-admin/target/jimuqu.jar --solon.env=prod

# Docker 运行
docker build -t jimuqu-admin .
docker run -p 5320:5320 jimuqu-admin
```

### 应用配置
- **默认端口**: 5320
- **开发环境**: 使用 MySQL 数据库
- **生产环境**: 使用 MariaDB 数据库
- **配置文件**: 
  - `app.yml` - 主配置
  - `app-dev.yml` - 开发环境配置
  - `app-prod.yml` - 生产环境配置

## 开发约定

### 代码规范
1. **全程使用中文**：注释、注解、文档均使用中文
2. **工具类优先级**：
   - 优先使用 `com.jimuqu.common.core.utils` 包下的工具
   - 其次使用 Hutool 工具
   - 如果不存在某工具，请添加到 `com.jimuqu.common.core.utils` 下
3. **空值处理**：
   - 判断是否为空，字段处理（分割，加解密）等情况，优先使用 Hutool 工具
   - Map、List、Set 等集合字段，get 时不允许为 null，除非指定要求

### 核心模式

#### 响应包装器
所有 API 响应使用 `R<T>` 包装器类：
```java
R.ok(data);           // 成功响应
R.fail("错误消息");  // 错误响应
R.warn("警告消息"); // 警告响应
```

#### 分页处理
使用 `Page<T>` 类进行分页：
```java
Page<T> page = Page.of(currentPage, pageSize);
// 结果由 Xbatis 自动填充
```

#### 实体基类
所有领域实体继承 `BaseEntity`，提供：
- 通用字段 (createTime, updateTime 等)
- 审计字段 (createBy, updateBy)
- 软删除支持

#### 数据库注解
实体使用 Xbatis 和 AutoTable 注解：
- `@Table`: 表映射
- `@TableId`: 主键配置
- `@AutoColumn`: 带自动架构管理的列定义

### 数据库管理
- **AutoTable**: 自动数据库架构管理
- **启动时自动**：根据实体类创建/更新表结构
- **初始数据**: 从 `src/main/resources/sql/{dialect}/` 目录加载
- **迁移文件**: 生成在 `./db/sql/` 目录中
- **配置**: 通过 `auto-table` 配置段控制行为

### 安全和认证
- **Sa-Token**: 基于 Token 的认证
- **权限**: 基于角色的访问控制 (RBAC)
- **数据范围**: 基于部门的数据权限
- **第三方认证**: JustAuth 集成社交登录

### 代码生成
系统包含代码生成器，可创建：
- CRUD 操作 (Controller, Service, Mapper)
- Vue 前端组件
- API 文档
- 数据库查询

支持多种模板 (Vue, Vben, TypeScript) 且可自定义。

## 配置说明

### 数据库配置
```yaml
# 开发环境 (app-dev.yml)
solon.dataSources.jimuqu!:
  class: "org.noear.solon.data.dynamicds.DynamicDataSource"
  default: "master"
  master:
    dataSourceClassName: "com.zaxxer.hikari.HikariDataSource"
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/jimuqu_db?...
    username: root
    password: P@ssw0rd
```

### 缓存配置
```yaml
# 本地缓存 (默认)
jimuqu.cache:
  driverType: "local"
  keyHeader: "jimuqu"

# Redis 缓存 (可选)
# jimuqu.cache:
#   driverType: "redis"
#   server: "127.0.0.1:6379"
#   db: 0
```

### 文件存储配置
```yaml
dromara:
  x-file-storage:
    default-platform: local-plus
    local-plus:
      - platform: local-plus
        enable-storage: true
        enable-access: true
        domain: http://127.0.0.1:8080/file/
        storage-path: D:/temp/
    minio:
      - platform: minio-user
        enable-storage: true
        access-key: j9rMyECcmNH0lNBqPfOo
        secret-key: 0NYFJSl4D8msuxHirenthXA4lvju4c3QNdmQ29Ob
        end-point: http://127.0.0.1:9000
        bucket-name: user
```

## 开发注意事项

### 环境配置
- **dev**: 本地开发环境，使用 MySQL
- **prod**: 生产环境，使用 MariaDB
- 配置文件通过 Maven 和 Solon 配置管理

### 数据库架构
- 表由 AutoTable 自动管理
- 实体类驱动架构变更
- 使用 `@AutoColumn` 注解定义列
- 迁移 SQL 文件自动生成

### 日志管理
- 操作日志自动启用
- 内置登录跟踪
- 日志级别可按环境配置

### 常见问题
- 项目全程使用中文注释和注解
- 默认数据库凭据在配置文件中（生产环境需修改）
- AutoTable 会自动修改数据库架构
- Sa-Token 配置在 `common-satoken` 模块中

## 相关链接
- **前端项目**: https://gitee.com/chengliang4810/jimuqu-admin-ui
- **文档地址**: https://doc.jimuqu.com
- **DeepWiki文档**: https://deepwiki.com/chengliang4810/jimuqu-admin
- **演示系统**: https://admin.jimuqu.com

## 许可证
- 项目代码、文档均开源免费可商用
- 保留开源协议文件即可