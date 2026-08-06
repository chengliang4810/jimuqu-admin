---
title: 数据权限测试指南
description: 账号、部门、SQL 与接口级验证场景
---

## 测试准备

### 1. 确保数据库中有测试数据

确保以下表中有测试数据：
- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_dept` - 部门表
- `sys_role_dept` - 角色部门关联表
- `sys_user_role` - 用户角色关联表

### 2. 创建测试用户和角色

```sql
-- 创建测试部门
INSERT INTO sys_dept (dept_id, parent_id, dept_name, order_num, leader, phone, email, status) VALUES
(100, 0, '总部', 0, 'admin', '15888888888', 'admin@example.com', '0'),
(101, 100, '研发部', 1, 'dev', '15888888881', 'dev@example.com', '0'),
(102, 100, '市场部', 2, 'market', '15888888882', 'market@example.com', '0'),
(103, 101, '前端组', 1, 'fe', '15888888883', 'fe@example.com', '0'),
(104, 101, '后端组', 2, 'be', '15888888884', 'be@example.com', '0');

-- 创建测试角色
INSERT INTO sys_role (role_id, role_name, role_key, data_scope) VALUES
(1, '超级管理员', 'admin', '1'),
(2, '研发部经理', 'dev_manager', '4'),
(3, '市场部经理', 'market_manager', '3'),
(4, '研发部员工', 'dev_staff', '4'),
(5, '市场部员工', 'market_staff', '5');

-- 创建角色部门关联（自定义权限）
INSERT INTO sys_role_dept (role_id, dept_id) VALUES
(2, 101), -- 研发部经理可以管理研发部
(2, 103), -- 研发部经理可以管理前端组
(2, 104); -- 研发部经理可以管理后端组

-- 创建测试用户
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status) VALUES
(1, 100, 'admin', '超级管理员', 'admin@example.com', '15888888888', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dOEiQwQW7lO3n0faRCi', '0'),
(2, 101, 'dev_manager', '研发部经理', 'dev_manager@example.com', '15888888881', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dOEiQwQW7lO3n0faRCi', '0'),
(3, 102, 'market_manager', '市场部经理', 'market_manager@example.com', '15888888882', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dOEiQwQW7lO3n0faRCi', '0'),
(4, 103, 'fe_staff', '前端员工', 'fe_staff@example.com', '15888888883', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dOEiQwQW7lO3n0faRCi', '0'),
(5, 104, 'be_staff', '后端员工', 'be_staff@example.com', '15888888884', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dOEiQwQW7lO3n0faRCi', '0'),
(6, 102, 'market_staff', '市场员工', 'market_staff@example.com', '15888888885', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dOEiQwQW7lO3n0faRCi', '0');

-- 创建用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin 是超级管理员
(2, 2), -- dev_manager 是研发部经理
(3, 3), -- market_manager 是市场部经理
(4, 4), -- fe_staff 是研发部员工
(5, 4), -- be_staff 是研发部员工
(6, 5); -- market_staff 是市场员工
```

## 测试场景

### 1. 超级管理员测试 (admin)
- **权限范围**: 全部数据权限 (1)
- **预期结果**: 可以查看所有用户
- **测试步骤**:
  1. 使用admin账号登录
  2. 访问 `/system/user/list`
  3. 预期返回6个用户

### 2. 研发部经理测试 (dev_manager)
- **权限范围**: 部门及以下数据权限 (4)
- **可访问部门**: 101 (研发部), 103 (前端组), 104 (后端组)
- **预期结果**: 只能看到研发部、前端组、后端组的用户
- **测试步骤**:
  1. 使用dev_manager账号登录
  2. 访问 `/system/user/list`
  3. 预期返回3个用户: dev_manager, fe_staff, be_staff

### 3. 市场部经理测试 (market_manager)
- **权限范围**: 部门数据权限 (3)
- **可访问部门**: 102 (市场部)
- **预期结果**: 只能看到市场部的用户
- **测试步骤**:
  1. 使用market_manager账号登录
  2. 访问 `/system/user/list`
  3. 预期返回2个用户: market_manager, market_staff

### 4. 研发部员工测试 (fe_staff/be_staff)
- **权限范围**: 部门及以下数据权限 (4)
- **可访问部门**: 101 (研发部), 103 (前端组), 104 (后端组)
- **预期结果**: 只能看到研发部、前端组、后端组的用户
- **测试步骤**:
  1. 使用fe_staff账号登录
  2. 访问 `/system/user/list`
  3. 预期返回3个用户: dev_manager, fe_staff, be_staff

### 5. 市场员工测试 (market_staff)
- **权限范围**: 仅本人数据权限 (5)
- **预期结果**: 只能看到自己
- **测试步骤**:
  1. 使用market_staff账号登录
  2. 访问 `/system/user/list`
  3. 预期返回1个用户: market_staff

## 测试验证

### 1. 检查SQL日志
启用SQL日志查看数据权限条件是否正确添加：

```properties
# application.yml
logging:
  level:
    com.jimuqu.common.mybatis.interceptor: DEBUG
    com.jimuqu.system.mapper: DEBUG
```

### 2. 验证数据权限条件
查看生成的SQL是否包含正确的数据权限条件：

- **超级管理员**: 无额外WHERE条件
- **研发部经理**: `WHERE u.dept_id IN (101, 103, 104)`
- **市场部经理**: `WHERE u.dept_id = 102`
- **市场员工**: `WHERE u.user_id = 6`

### 3. 权限验证测试
测试用户详情查看的权限验证：

```bash
# 使用market_staff账号尝试查看admin用户详情
GET /system/user/1
# 预期返回: 没有权限访问用户数据！
```

## 常见问题排查

### 1. 数据权限不生效
- 检查方法上是否有`@DataPermission`注解
- 检查XbatisConfig是否正确注册了拦截器
- 检查LoginUser中是否正确设置了dataScope和roleId

### 2. SpEL表达式解析错误
- 检查数据权限服务Bean是否命名为"sysDataScopeService"
- 检查LoginUser是否有getRoleId()和getDeptId()方法
- 查看日志中的SpEL解析错误信息

### 3. SQL条件生成错误
- 检查@DataColumn注解的key-value映射是否正确
- 检查DataScopeType的sqlTemplate是否正确
- 验证数据库字段名是否与映射一致

### 4. 循环调用问题
- 确保在数据权限服务中不调用标注了数据权限注解的方法
- 检查是否有循环依赖导致的问题

## 性能测试

### 1. 大数据量测试
创建1000个用户，分布在不同部门，测试查询性能。

### 2. 并发测试
模拟多用户并发查询，验证数据权限隔离性。

### 3. 复杂权限测试
配置复杂的部门层级和权限组合，测试权限计算的正确性。

## 自动化测试

### 1. 单元测试
运行`SysDataScopeServiceImplTest`测试数据权限服务。

### 2. 集成测试
使用Spring Boot Test框架测试完整的数据权限流程。

### 3. 接口测试
使用Postman或类似工具测试实际API的数据权限效果。