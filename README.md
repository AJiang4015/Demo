# 用户权限管理系统

## 项目介绍

基于微服务架构实现的简化版用户权限管理系统，包含以下核心功能：

- 角色分级管理：普通用户、管理员、超级管理员（初始化含超管）
- 操作日志异步记录：通过MQ持久化关键操作日志
- 微服务间协作：用户服务与权限服务通过RPC通信
- 分库分表实践：用户表水平分片
- 分布式事务：用户注册与角色绑定原子性保障

## 系统架构

```
+-------------------+     +---------------------+     +----------------------+
|   User Service    |<--->|  Permission Service |<--->|  Logging Service     |
| (HTTP API + MQ)   |     | (RPC服务端)          |     | (MQ消费者)            |
+-------------------+     +---------------------+     +----------------------+
```

## 技术栈

- **服务注册与发现**：Nacos
- **RPC通信**：OpenFeign + Nacos
- **消息队列**：RocketMQ
- **分库分表**：ShardingSphere + MySQL
- **分布式事务**：Seata (AT模式)
- **配置中心**：Nacos
- **数据库访问**：MyBatis-Plus
- **认证授权**：Spring Security + JWT

## 模块说明

### 1. 公共模块 (common)

包含所有服务共用的依赖、工具类和通用配置。

### 2. 用户服务 (user-service)

**职责**：
- 用户注册/登录鉴权（JWT）
- 分库分表管理用户数据
- 调用权限服务绑定角色（RPC调用）
- 发送操作日志至MQ（消息生产者）

**接口**：

| 接口路径 | 方法 | 功能描述 | 技术实现 |
| --- | --- | --- | --- |
| /user/register | POST | 用户注册 | 分库分表写入用户表 → RPC调用绑定默认角色 → 发送日志消息至MQ |
| /user/login | POST | 登录生成JWT Token | 校验密码 → 生成Token |
| /users | GET | 分页用户列表 | 根据权限校验结果返回：普通用户仅自己，管理员所有普通用户，超管全部 |
| /user/{userId} | GET | 查询用户信息 | 根据权限校验结果返回：普通用户仅自己，管理员所有普通用户，超管全部 |
| /user/{userId} | PUT | 修改用户信息 | 根据权限限制：普通用户改自己，管理员改普通用户，超管改所有 |
| /user/reset-password | POST | 密码重置 | 普通用户重置自己，管理员重置普通用户，超管重置所有人 |

**数据库表**：
```sql
-- 用户表（分库分表）
CREATE TABLE users (
  user_id BIGINT PRIMARY KEY,
  username VARCHAR(50),
  password VARCHAR(255),
  email VARCHAR(100),
  phone VARCHAR(20),
  gmt_create TIMESTAMP
);
```

### 3. 权限服务 (permission-service)

**职责**：
- 管理用户角色绑定（普通用户/管理员/超管）
- 提供RPC接口查询用户角色码
- 支持角色升级/降级

**接口**：
```java
// RPC接口定义
public interface PermissionService {
    // 绑定默认角色（普通用户）
    void bindDefaultRole(Long userId);

    // 查询用户角色码（返回role_code）
    String getUserRoleCode(Long userId);

    // 超管调用：升级用户为管理员
    void upgradeToAdmin(Long userId);

    // 超管调用：降级用户为普通角色
    void downgradeToUser(Long userId);
}
```

**数据库表**：
```sql
-- 角色表（权限服务单库）
CREATE TABLE roles (
  role_id INT PRIMARY KEY,  -- 1:超管 2:普通用户 3:管理员
  role_code VARCHAR(20) UNIQUE  -- super_admin/user/admin
);

-- 用户-角色关系表
CREATE TABLE user_roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  role_id INT,
  UNIQUE KEY uk_user_role (user_id)  -- 每个用户仅绑定一个角色
);
```

### 4. 日志服务 (logging-service)

**职责**：
- 异步消费MQ日志消息
- 持久化操作日志

**数据库表**：
```sql
-- 操作日志表（单库）
CREATE TABLE operation_logs (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  action VARCHAR(50),  -- 如 "update_user"
  ip VARCHAR(15),
  detail TEXT          -- 记录修改内容（如 {"field":"email", "old":"a","new":"b"}）
);
```

## 核心流程

### 1. 用户注册流程

1. 客户端 -> 用户服务: POST /user/register
2. 用户服务 -> 分库分表: 写入users表
3. 用户服务 -> RPC调用: permissionService.bindDefaultRole(userId)
4. 用户服务 -> MQ: 发送"REGISTER"日志消息
5. 日志服务 -> 消费消息: 写入operation_logs表

### 2. 权限校验流程

1. 客户端 -> 用户服务: 请求需权限接口（如GET /user/123）
2. 用户服务 -> RPC调用: permissionService.getUserRoleCode(userId=123)
3. 权限服务 -> 查询user_roles表 → 关联roles.role_code
4. 权限服务 -> 返回角色码（如 "admin"）
5. 用户服务 -> 本地逻辑校验角色权限：
   - 若接口需管理员权限，检查role_code是否为"admin"
   - 若接口需超管权限，检查role_code是否为"super_admin"

### 3. 分布式事务设计

场景：用户注册需保证用户创建与角色绑定的原子性

## 项目启动

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Nacos 2.0.4+
- RocketMQ 4.9.4+
- Seata 1.5.2+

### 启动步骤

1. 启动Nacos服务
2. 启动RocketMQ服务
3. 启动Seata服务
4. 创建数据库并执行SQL脚本
5. 修改各服务配置文件中的数据库连接信息
6. 依次启动服务：permission-service -> logging-service -> user-service

## 项目特点

1. **微服务架构**：基于Spring Cloud Alibaba实现服务注册发现、配置管理和服务调用
2. **分库分表**：使用ShardingSphere实现用户表的水平分片，提高系统扩展性
3. **消息队列**：使用RocketMQ实现操作日志的异步处理，提高系统性能
4. **分布式事务**：使用Seata确保跨服务操作的数据一致性
5. **权限控制**：基于角色的访问控制，确保数据安全

## 可能的改进点

1. 增加服务熔断和限流机制
2. 添加API网关统一管理请求
3. 实现分布式会话管理
4. 增加监控和告警系统
5. 完善单元测试和集成测试