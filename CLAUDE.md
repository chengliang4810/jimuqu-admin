# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指导。

# 用户要求

## 通用要求
- 对话过程中全程使用中文与用户进行沟通交流。
- 所有生成的MD文档放在`docs`目录下保存。
- Map、List、Set等集合字段，get时不允许为null，除非特别指定。

## 新接口开发规范
当进行新接口开发时，必须严格遵循 `docs/通用增删改查示例.md` 中定义的模式。
- **目录结构**: 严格遵守 `domain`, `mapper`, `service`, `controller` 的分层结构。
- **领域对象**:
    - `domain`: 存放数据库实体类 (`@Table`)。
    - `domain/bo`: 存放业务对象，用于服务层方法参数，并包含校验注解。
    - `domain/vo`: 存放视图对象，用于控制器返回数据。
    - `domain/query`: 存放查询条件对象，使用 `@Condition` 注解。
- **Mapper**: 继承 `BaseMapperPlus`，利用其内置方法和 `QueryChain` 进行数据库操作。禁止编写XML文件。
- **Service**: 定义业务逻辑接口和实现。查询优先使用 `QueryChain` 构建。
- **Controller**: 负责API路由和参数校验，调用Service完成业务。

## 数据库操作规范
- 必须使用 **Xbatis** (`cn.xbatis`) 进行所有数据库操作，参考 `docs/xbatis文档.md`。
- 复杂查询优先使用 `QueryChain`，它提供了强大的链式调用能力。
- 实体类必须继承 `BaseEntity`，以确保包含标准的审计字段。
- 使用 `@AutoColumn` 注解来管理数据库表结构，利用 `AutoTable` 的自动同步功能。

## 文件上传规范
- 文件或图像的保存操作，必须使用 **x-file-storage** (`org.dromara.x.file.storage.core`)。
- 具体实现请参考 `docs/文件上传示例.md` 中的代码。

## 工具类使用规范
- 优先使用项目自定义的工具类，位于 `com.jimuqu.common.core.utils` 包下。
- 其次，广泛使用 **Hutool** 工具库来处理如非空判断、字段处理（分割、加解密）等常见任务。
- 如果所需工具不存在，请将其添加到 `com.jimuqu.common.core.utils` 包中。

# MCP Interactive Feedback 规则

- 注意在任务完成前根据mcp-feedback-enhanced工具的要求进行工具调用

## 项目概述

这是一个基于 **Solon** 框架的轻量级管理系统框架 **jimuqu-admin**。它是一个中文开源项目，提供了完整的管理后台系统，包含用户管理、权限管理和代码生成功能。

## 构建和开发命令

### Maven 命令
```bash
# 构建整个项目
mvn clean package

# 使用指定环境运行 (dev/prod)
mvn spring-boot:run -Pdev
mvn spring-boot:run -Pprod

# 构建时跳过测试 (默认行为)
mvn clean package -DskipTests

# 显式运行测试
mvn test
```

### 应用启动
```bash
# 启动应用 (默认使用 dev 环境)
java -jar jimuqu.jar

# 使用指定环境启动
java -jar jimuqu.jar --spring.profiles.active=prod
```

### 数据库设置
项目使用 **AutoTable** 进行自动数据库架构管理。启动时：
- 数据库表根据实体类自动创建/更新
- 初始数据从 `src/main/resources/sql/{dialect}/` 目录加载
- SQL 迁移文件生成在 `./db/sql/` 目录中, 项目启动自动维护，无需手动处理

## 架构和结构

### 多模块 Maven 结构
- **jimuqu-admin**: 主应用程序入口点
- **jimuqu-common**: 通用工具和配置
  - `jimuqu-common-core`: 核心工具、常量、基类
  - `jimuqu-common-web`: Web 层配置
  - `jimuqu-common-mybatis`: 数据库层 (Xbatis)
  - `jimuqu-common-satoken`: 身份验证和授权
  - `jimuqu-common-security`: 安全配置
  - `jimuqu-common-log`: 操作日志
  - `jimuqu-common-excel`: Excel 导入导出
  - `jimuqu-common-cache`: 缓存层
  - `jimuqu-common-oss`: 对象存储服务
  - `jimuqu-common-social`: 第三方认证
- **jimuqu-modules**: 业务模块
  - `jimuqu-system`: 核心系统功能 (用户、角色、权限)
  - `jimuqu-generator`: 代码生成工具

### 核心技术
- **Solon**: 轻量级 Java 应用框架 (类似 Spring Boot)
- **Xbatis**: MyBatis 增强工具，支持动态 SQL
- **Sa-Token**: 身份验证和授权框架
- **AutoTable**: 自动数据库架构管理
- **Hutool**: Java 工具库
- **MapStruct Plus**: 对象映射
- **EasyExcel**: Excel 处理

### 核心模式

#### 响应包装器
所有 API 响应使用 `R<T>` 包装器类：
```java
R.ok(data);           // 成功响应
R.fail("错误消息");  // 错误响应
R.warn("警告消息"); // 警告响应
```

#### 分页
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

### 配置文件
- `app.yml`: 主配置 (端口、应用信息、安全)
- `app-dev.yml`: 开发环境 (MySQL 数据源)
- `app-prod.yml`: 生产环境 (MariaDB 数据源)
- 其他配置从 `config/*.yml` 加载

### 数据库配置
- **默认**: MySQL/MariaDB 配合 HikariCP 连接池
- **缓存**: 默认本地缓存，支持 Redis
- **架构管理**: AutoTable 处理自动架构更新
- **迁移**: `db/sql/` 目录中的 SQL 文件，使用 Flyway 风格版本控制

### 安全和认证
- **Sa-Token**: 基于 Token 的认证
- **权限**: 基于角色的访问控制 (RBAC)
- **数据范围**: 基于部门的数据权限
- **第三方认证**: JustAuth 集成社交登录

## 开发注意事项

### 环境配置
- **dev**: 本地开发环境，使用 MySQL
- **prod**: 生产环境，使用 MySQL
- 配置文件通过 Maven 和 Solon 配置管理

### 数据库架构
- 表由 AutoTable 自动管理
- 实体类驱动架构变更
- 使用 `@AutoColumn` 注解定义列
- 迁移 SQL 文件自动生成

### 日志
- 操作日志自动启用
- 内置登录跟踪
- 日志级别可按环境配置

### 文件存储
- 支持本地存储和 MinIO
- 通过 `dromara.x-file-storage` 部分配置
- 支持缩略图生成

### 常见问题
- 项目全程使用中文注释和注解
- 默认数据库凭据在配置文件中 (生产环境需修改)
- AutoTable 会自动修改数据库架构
- Sa-Token 配置在 `common-satoken` 模块中