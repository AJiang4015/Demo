# 用户权限管理系统（简化版）

## 项目介绍

基于Spring Boot实现的简化版用户权限管理系统，专注于核心功能实现：

- **用户注册/登录**：基于JWT的用户认证
- **角色权限控制**：普通用户、管理员、超级管理员三级权限
- **操作日志落库**：同步记录关键操作日志

## 系统架构（简化版）

```
+-------------------+     +---------------------+     +----------------------+
|   User Service    |---->|  Permission Service |     |  Logging Service     |
| (HTTP API)        |     | (HTTP API)          |     | (HTTP API)           |
+-------------------+     +---------------------+     +----------------------+
                    \                                 /
                     \                               /
                      +-----------------------------+
                      |        MySQL Database       |
                      | (单库，包含所有表)            |
                      +-----------------------------+
```

## 技术栈（简化版）

- **Web框架**：Spring Boot 2.6.3
- **数据库访问**：MyBatis-Plus
- **数据库**：MySQL（单库）
- **认证授权**：Spring Security + JWT
- **工具库**：Hutool、Lombok
- **JSON处理**：FastJSON

## 模块说明

### 1. 公共模块 (common)

包含所有服务共用的依赖、工具类和通用配置。

### 2. 用户服务 (user-service)

**职责**：
- 用户注册/登录鉴权（JWT）
- 用户信息管理
- 调用权限服务进行权限校验
- 记录操作日志

**接口**：

| 接口路径 | 方法 | 功能描述 | 权限要求 |
| --- | --- | --- | --- |
| /user/register | POST | 用户注册 | 无需登录 |
| /user/login | POST | 登录生成JWT Token | 无需登录 |
| /users | GET | 分页用户列表 | 管理员及以上 |
| /user/{userId} | GET | 查询用户信息 | 本人或管理员及以上 |
| /user/{userId} | PUT | 修改用户信息 | 本人或管理员及以上 |
| /user/reset-password | POST | 密码重置 | 本人或管理员及以上 |

**数据库表**：
```sql
-- 用户表（单库）
CREATE TABLE users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100),
  phone VARCHAR(20),
  status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 3. 权限服务 (permission-service)

**职责**：
- 管理用户角色绑定（普通用户/管理员/超管）
- 提供HTTP接口查询用户角色
- 支持角色升级/降级

**接口**：

| 接口路径 | 方法 | 功能描述 | 权限要求 |
| --- | --- | --- | --- |
| /permission/bind-default/{userId} | POST | 绑定默认角色（普通用户） | 系统内部调用 |
| /permission/user/{userId}/role | GET | 查询用户角色信息 | 系统内部调用 |
| /permission/upgrade/{userId} | PUT | 升级用户为管理员 | 超级管理员 |
| /permission/downgrade/{userId} | PUT | 降级用户为普通用户 | 超级管理员 |
| /permission/roles | GET | 获取所有角色列表 | 管理员及以上 |

**数据库表**：
```sql
-- 角色表
CREATE TABLE roles (
  role_id INT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(20) UNIQUE  -- super_admin/user/admin
);

-- 用户-角色关系表
CREATE TABLE user_roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id INT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id)  -- 每个用户仅绑定一个角色
);
```

### 4. 日志服务 (logging-service)

**职责**：
- 接收并记录操作日志
- 提供日志查询接口

**接口**：

| 接口路径 | 方法 | 功能描述 | 权限要求 |
| --- | --- | --- | --- |
| /log/record | POST | 记录操作日志 | 系统内部调用 |
| /logs | GET | 查询操作日志（分页） | 管理员及以上 |
| /logs/user/{userId} | GET | 查询指定用户操作日志 | 本人或管理员及以上 |

**数据库表**：
```sql
-- 操作日志表
CREATE TABLE operation_logs (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  action VARCHAR(50) NOT NULL COMMENT '操作类型',
  resource VARCHAR(100) COMMENT '操作资源',
  ip VARCHAR(45) COMMENT 'IP地址',
  user_agent VARCHAR(500) COMMENT '用户代理',
  detail TEXT COMMENT '操作详情',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 核心流程（简化版）

### 1. 用户注册流程

1. 客户端 -> 用户服务: POST /user/register
2. 用户服务 -> 数据库: 写入users表
3. 用户服务 -> 权限服务: POST /permission/bind-default/{userId}
4. 用户服务 -> 日志服务: POST /log/record（记录注册日志）
5. 返回注册成功响应

### 2. 权限校验流程

1. 客户端 -> 用户服务: 请求需权限接口（携带JWT Token）
2. 用户服务 -> JWT解析: 获取用户ID
3. 用户服务 -> 权限服务: GET /permission/user/{userId}/role
4. 权限服务 -> 返回用户角色信息
5. 用户服务 -> 本地权限校验:
   - 检查用户角色是否满足接口权限要求
   - 返回相应的数据或拒绝访问

### 3. 操作日志记录

1. 用户执行关键操作（如修改用户信息）
2. 用户服务 -> 执行业务逻辑
3. 用户服务 -> 日志服务: POST /log/record
4. 日志服务 -> 数据库: 写入operation_logs表

## 项目启动（简化版）

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+

### 数据库初始化

1. 创建数据库：
```sql
CREATE DATABASE user_permission_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表脚本（见上述数据库表结构）

3. 初始化角色数据：
```sql
INSERT INTO roles (role_name, role_code) VALUES 
('超级管理员', 'super_admin'),
('管理员', 'admin'),
('普通用户', 'user');
```

4. 创建超级管理员账户：
```sql
INSERT INTO users (username, password, email, status) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyF5bIWY2P3fhXK2xqJY5Vj.2Pu', 'admin@example.com', 1);
-- 密码为：123456

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
```

### 启动步骤

1. 修改各服务配置文件中的数据库连接信息
2. 依次启动服务：
   ```bash
   # 启动权限服务
   cd permission-service
   mvn spring-boot:run
   
   # 启动日志服务
   cd logging-service
   mvn spring-boot:run
   
   # 启动用户服务
   cd user-service
   mvn spring-boot:run
   ```

### 接口测试

1. 用户注册：
```bash
curl -X POST http://localhost:8081/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456","email":"test@example.com"}'
```

2. 用户登录：
```bash
curl -X POST http://localhost:8081/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

## 项目特点（简化版）

1. **简化架构**：基于Spring Boot的单体服务架构，易于开发和部署
2. **核心功能**：专注于用户管理、权限控制和操作日志三大核心功能
3. **JWT认证**：使用JWT实现无状态的用户认证
4. **角色权限**：基于角色的访问控制，支持三级权限管理
5. **操作审计**：完整记录用户操作日志，便于审计和追踪

## 最新更新记录

### 2025年1月 - 完善操作日志功能

**更新内容**：为user-service的所有业务方法添加完整的MQ操作日志记录

**具体改进**：
1. **完善日志覆盖**：为`getUserInfo`和`getUserList`方法添加了MQ日志发送功能
2. **增强日志信息**：所有操作日志现在都包含完整的IP地址信息
3. **新增日志类型**：
   - `USER_INFO_VIEW`：用户信息查看日志
   - `USER_LIST_VIEW`：用户列表查看日志
4. **优化日志内容**：
   - 查看用户信息日志包含：目标用户名、目标用户ID、操作类型
   - 查看用户列表日志包含：当前用户角色、分页信息、结果数量、操作类型

**技术实现**：
- 扩展了`LogProducer`类，新增`sendUserInfoViewLog`和`sendUserListViewLog`方法
- 更新了`UserService`接口和`UserServiceImpl`实现类的方法签名
- 修改了`UserController`控制器，确保IP地址正确传递到服务层
- 所有操作日志都通过RabbitMQ异步发送到logging-service进行处理

**日志记录范围**：
- ✅ 用户注册 (`USER_REGISTER`)
- ✅ 用户登录 (`USER_LOGIN`)
- ✅ 用户信息查看 (`USER_INFO_VIEW`) - **新增**
- ✅ 用户列表查看 (`USER_LIST_VIEW`) - **新增**
- ✅ 用户信息修改 (`USER_UPDATE`)
- ✅ 密码重置 (`PASSWORD_RESET`)

## 后续扩展计划

1. **分库分表**：当用户量增长时，可引入ShardingSphere实现数据分片
2. **消息队列**：引入RocketMQ实现操作日志的异步处理
3. **服务注册发现**：引入Nacos实现微服务架构
4. **分布式事务**：引入Seata保证跨服务数据一致性
5. **API网关**：添加网关统一管理请求路由和鉴权