# Jimuqu Admin

Jimuqu Admin 是基于 Java 21、Solon 3、Xbatis、AutoTable 与 Sa-Token 的企业管理后台。

> ❤️ 项目代码、文档均**开源免费可商用**，保留开源协议文件即可  
> 💡 活到老写到老 • 为兴趣而开源 • 为学习而开源 • 为技术共享而开源

🔗 前端项目地址：[https://gitee.com/chengliang4810/jimuqu-admin-ui](https://gitee.com/chengliang4810/jimuqu-admin-ui)  
📚 文档地址：[https://doc.jimuqu.com](https://doc.jimuqu.com)  
🌐 DeepWiki文档：[https://deepwiki.com/chengliang4810/jimuqu-admin](https://deepwiki.com/chengliang4810/jimuqu-admin)  
🚀 演示系统：[https://admin.jimuqu.com](https://admin.jimuqu.com)

## 计划列表
- [X] Hutool V7升级, 支持最新版Hutool
- [X] 字段翻译功能：支持枚举、字符串、数据库字典
- [X] 基于IP/用户/全局接口限流功能
- [X] Reids模块基于redisson的缓存工具
- [X] sms 短信模块
- [X] websocket模块
- [X] sse模块
- [X] mail 邮件模块
- [X] ip2region进行升级支持ipv6
- [X] idempotent 幂等功能
- [X] 集成fesod(FastExcel)导入导出功能
- [X] encrypt API接口数据加解密模块
- [X] sensitive 脱敏模块
- [ ] 无侵入式基于注释内容生成接口文档
- [ ] 前端H5/Uniapp开发
- [ ] 提供项目Mcp接口

---

## 🛠 主要技术栈

| 技术名                             | 作用                                      | 特点                                      |
|---------------------------------|------------------------------------------|------------------------------------------|
| **🚀 Solon**                    | Java 轻量级应用框架                      | 国产、高性能、低延时                     |
| **🧰 Hutool**                   | Java 工具库                              | 国产、简化代码、功能全面                 |
| **🔒 Sa-Token**                 | 权限认证框架                             | 国产、轻量级、支持分布式会话             |
| **🗃️ Xbatis**                  | MyBatis 增强工具                         | 兼容 MyBatis、动态 SQL 支持              |
| **⚙️ AutoTable**                | 数据库表结构自动维护                     | 国产、支持多数据库                       |
| **📊 FastExcel**                | Excel 导入导出工具                       | 阿里开源、避免 OOM                       |
| **👤 JustAuth**                 | 第三方登录集成                           | 国产、支持 20+ 平台                      |
| **📍 ip2region**                | 离线 IP 地址定位                         | 数据本地化、毫秒级查询                   |
| **🔄 MapStruct Plus**           | 对象转换工具                             | 零反射、高性能                           |
| **🧩 Lombok**                   | 代码简化                                 | 减少样板代码                             |
| **💧 HikariCP**                 | JDBC 连接池                              | 高性能、轻量级                           |
| **🧵 TransmittableThreadLocal** | 线程间上下文传递                         | 阿里开源、解决异步线程上下文丢失         |
| **💾 SQLite**                   | 嵌入式数据库                             | 轻量级、零配置、单文件存储               |

---

## 📋 功能列表

| 业务             | 功能说明                                                                 |
|------------------|--------------------------------------------------------------------------|
| **📱 客户端管理** | 管理对接客户端（PC/小程序等），支持动态授权登录方式和Token时效控制       |
| **👥 用户管理**   | 用户增删改查，分配部门/角色/岗位                                         |
| **🏢 部门管理**   | 树形组织结构管理（公司/部门/小组），支持数据权限                         |
| **📋 岗位管理**   | 配置用户职务信息                                                         |
| **📑 菜单管理**   | 配置系统菜单、操作权限、按钮权限标识                                     |
| **🔐 角色管理**   | 分配角色菜单权限，按机构划分数据范围                                     |
| **📖 字典管理**   | 维护系统固定数据（如状态/类型等）                                        |
| **⚙️ 参数管理**   | 动态配置系统参数                                                         |
| **📢 通知公告**   | 发布和维护系统公告                                                       |
| **📝 操作日志**   | 记录系统操作日志和异常信息                                               |
| **🔐 登录日志**   | 查询登录记录（含异常登录）                                               |
| **📂 文件管理**   | 文件展示/上传/下载/删除                                                  |
| **⚙️ 文件配置管理** | 动态管理文件上传/下载配置                                                |
| **👀 在线用户管理** | 监控在线用户并支持强制踢出                                               |
| **🔌 系统接口**   | 自动生成API文档                                                          |

---

## 快速启动

准备 Java 21、MySQL 8 和 Redis 后，直接运行 Release 中的 JAR：

```bash
export JIMU_DB_USERNAME='<数据库账号>'
export JIMU_DB_PASSWORD='<数据库密码>'
java -jar jimuqu-admin.jar --solon.env=prod
```

服务默认监听 `5320`。生产环境必须提供 `JIMU_DB_USERNAME` 和 `JIMU_DB_PASSWORD`；数据库地址可通过 `JIMU_DB_URL` 覆盖，Redis 可通过 `JIMU_REDIS_SERVER`、`JIMU_REDIS_DB`、`JIMU_REDIS_PASSWORD` 和 `JIMU_REDIS_PREFIX` 配置。

首次部署既可以让目标数据库不存在并授予数据库账号建库权限，也可以预先创建一个完全空的 MySQL 数据库。AutoTable 会自动创建表结构并执行唯一的 `sql/MySQL/jimuqu.sql` 初始化数据文件；已有业务数据的数据库及第二次、后续启动不会重复写入种子数据。

生产上线前必须修改初始化管理员密码和客户端密钥，并替换前后端成对的 RSA 配置；Redis、OSS 与数据库账号均应采用最小权限和独立凭据。

---

## 🖼 系统预览图

📸 **管理界面一览**  
![img.png](docs/images/img_14.png)
![img.png](docs/images/img_15.png)
![img.png](docs/images/img_16.png)
![img.png](docs/images/img.png)
![img.png](docs/images/img_1.png)  
![img.png](docs/images/img_3.png)  
![img.png](docs/images/img_4.png)  
![img.png](docs/images/img_5.png)
![img.png](docs/images/img_6.png)
![img.png](docs/images/img_7.png)
![img.png](docs/images/img_8.png)
![img.png](docs/images/img_9.png)
![img.png](docs/images/img_10.png)
![img.png](docs/images/img_11.png)
![img.png](docs/images/img_12.png)
![img.png](docs/images/img_13.png)

---

### 🌟 项目亮点
- **国产化技术栈**：Solon/Hutool/Sa-Token 等国产框架深度集成
- **轻量高效**：启动快、资源占用低、响应速度优异
- **开箱即用**：提供完整的管理系统基础功能模块
- **文档齐全**：中文文档 + 在线演示系统降低学习成本

**🎯 适合场景**：企业后台系统、快速开发平台、教学研究项目

## 全栈验证

Windows、macOS 和 Linux 使用同一个 Node.js 入口：

```bash
node script/test-fullstack.mjs
```

运行前设置 `JIMU_TEST_MYSQL_PASSWORD`；前端仓库不在自动发现位置时，再设置 `JIMU_TEST_FRONTEND_DIR`。脚本会创建本次运行独占的 MySQL 数据库、Redis DB 15 键前缀和 OSS 临时目录，并在结束时只清理这些资源。GitHub Actions 会在 Windows、macOS 和 Linux 上分别执行完整的 Maven、JAR、前端构建和 Playwright 门禁。

---

## 许可证

本项目使用 [MIT License](LICENSE)。第三方项目的版权与许可声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 上游项目声明

- [Bell-Plus](https://gitee.com/dapppp/bell-plus)，前端迁移基线为 `main@c1a99e5d9f568936d8e3fbcf37d302d5ca3127de`，MIT License；Bell-Plus 基于 Vue Vben Admin。
- [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus/tree/6.X/)，后端行为参考基线为 `6.X@da5f30cae2deb174a1ba37a2ad41ff1ba42c9f38`，MIT License。本项目仅参考其接口行为，后端实现仍使用 Solon/Xbatis。
