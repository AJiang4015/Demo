# 微服务权限管理系统

## 项目概述

这是一个基于Spring Cloud的微服务权限管理系统，实现了用户管理、权限控制和分页查询功能。系统采用分布式架构，支持用户注册、登录、角色管理和基于权限的数据访问控制。

## 系统架构

### 服务模块

1. **user-service (用户服务)**
    - 用户注册、登录
    - 用户信息管理
    - 用户列表分页查询
    - 密码重置

2. **permission-service (权限服务)**
    - 用户角色绑定
    - 角色权限管理
    - 权限验证
    - 可见用户ID分页查询

3. **logging-service (日志服务)**
    - 操作日志记录
    - 审计追踪

4. **common (公共模块)**
    - 通用工具类
    - 统一响应格式
    - 异常处理

### 技术栈

- **框架**: Spring Boot, Spring Cloud
- **数据库**: MySQL + MyBatis Plus
- **服务发现**: Nacos
- **分布式事务**: Seata
- **消息队列**: RabbitMQ
- **服务调用**: OpenFeign
- **认证**: JWT

## 权限体系

### 角色定义

1. **super_admin (超级管理员)**
    - 可以查看所有用户
    - 可以升级/降级用户角色
    - 拥有最高权限

2. **admin (管理员)**
    - 可以查看普通用户
    - 可以查看自己的信息
    - 不能查看其他管理员和超管

3. **user (普通用户)**
    - 只能查看自己的信息
    - 默认注册角色

### 权限控制逻辑

系统通过以下方式实现权限控制：

1. **用户查询权限**:
    - 超管：查看所有用户
    - 管理员：查看普通用户 + 自己
    - 普通用户：只查看自己

2. **分页查询优化**:
    - 权限服务先过滤可见用户ID
    - 用户服务根据ID列表查询详细信息
    - 避免大量RPC调用，提高性能

## 核心功能

### 1. 用户注册流程

```
用户注册 → 保存用户信息 → RPC调用绑定默认角色 → 发送注册日志
```

### 2. 用户登录流程

```
验证用户名密码 → 生成JWT Token → 发送登录日志
```

### 3. 权限分页查询流程

```
获取当前用户角色 → 权限服务过滤可见用户ID → 用户服务查询用户详情 → 返回分页结果
```

## API 接口说明

### 用户服务接口

#### 1. 用户注册
- **接口**: `POST /user/register`
- **参数**:
  ```json
  {
    "username": "用户名",
    "password": "密码",
    "email": "邮箱",
    "phone": "手机号"
  }
  ```
- **返回**: 用户ID

#### 2. 用户登录
- **接口**: `POST /user/login`
- **参数**:
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **返回**: JWT Token

#### 3. 获取用户信息
- **接口**: `GET /user/{userId}`
- **权限**: 根据角色控制可见范围
- **返回**: 用户详细信息

#### 4. 分页查询用户列表
- **接口**: `GET /user/list`
- **参数**: `pageNo`, `pageSize`
- **权限**: 根据角色返回不同的用户列表
- **返回**: 分页用户列表

### 权限服务接口

#### 1. 绑定默认角色
- **接口**: `POST /role/bind/{userId}`
- **用途**: 新用户注册时自动绑定普通用户角色

#### 2. 查询用户角色
- **接口**: `GET /role/code/{userId}`
- **返回**: 角色代码 (super_admin/admin/user)

#### 3. 升级用户为管理员
- **接口**: `POST /role/upgrade/{userId}`
- **权限**: 仅超管可调用

#### 4. 降级用户为普通用户
- **接口**: `POST /role/downgrade/{userId}`
- **权限**: 仅超管可调用

#### 5. 分页查询可见用户ID
- **接口**: `POST /role/visible-users`
- **参数**: `currentUserId`, `currentUserRole`, `pageNo`, `pageSize`
- **返回**: 根据权限过滤的用户ID分页列表

## 数据库设计

### 用户表 (users)
```sql
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    gmt_create DATETIME
);
```

### 角色表 (roles)
```sql
CREATE TABLE roles (
    role_id INT PRIMARY KEY,
    role_code VARCHAR(20) NOT NULL
);

INSERT INTO roles VALUES 
(1, 'super_admin'),
(2, 'user'),
(3, 'admin');
```

### 用户角色关系表 (user_roles)
```sql
CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL
);
```

## 部署说明

### 环境要求
- JDK 8+
- MySQL 5.7+
- Nacos
- RabbitMQ
- Seata

### 启动顺序
1. 启动基础设施 (MySQL, Nacos, RabbitMQ, Seata)
2. 启动 permission-service
3. 启动 user-service
4. 启动 logging-service

## 性能优化

### 1. 分页查询优化
- 权限服务只返回用户ID，减少数据传输
- 用户服务批量查询用户信息，减少数据库访问
- 避免N+1查询问题

### 2. 缓存策略
- 用户角色信息可考虑缓存
- JWT Token包含基本权限信息

### 3. 数据库优化
- 用户表按用户ID分片
- 角色查询添加索引

## 安全考虑

1. **密码安全**: 使用加密存储
2. **JWT安全**: 设置合理过期时间
3. **权限校验**: 每次操作都进行权限验证
4. **日志审计**: 记录所有关键操作

## 扩展性

1. **水平扩展**: 支持多实例部署
2. **角色扩展**: 可轻松添加新角色类型
3. **权限细化**: 可扩展为更细粒度的权限控制
4. **多租户**: 可扩展支持多租户架构

## 故障处理

### 服务降级
- 权限服务不可用时，默认返回普通用户权限
- 用户服务不可用时，返回友好错误信息

### 事务处理
- 使用Seata保证分布式事务一致性
- 关键操作支持事务回滚

## 监控告警

- 服务健康检查
- 接口响应时间监控
- 错误率统计
- 业务指标监控

---

**注意**: 本系统为演示项目，生产环境使用时请根据实际需求进行安全加固和性能优化。