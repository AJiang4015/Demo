# 用户权限管理系统 (User Permission System)

基于微服务架构的用户权限管理系统，提供用户管理、角色权限控制和操作日志记录功能。

## 📋 项目概述

本项目是一个完整的企业级用户权限管理解决方案，采用Spring Cloud微服务架构，支持用户注册登录、角色权限管理、操作日志记录等核心功能。系统具有高可用、高并发、易扩展的特点。

### 🏗️ 系统架构

```
用户权限管理系统
├── common/              # 公共模块
│   ├── 工具类
│   ├── 统一响应格式
│   ├── 异常处理
│   └── JWT工具
├── user-service/        # 用户服务
│   ├── 用户注册/登录
│   ├── 用户信息管理
│   ├── Token白名单管理
│   └── 权限验证拦截器
├── permission-service/  # 权限服务
│   ├── 角色管理
│   ├── 用户角色绑定
│   ├── 权限升级/降级
│   └── 可见用户查询
└── logging-service/     # 日志服务
    ├── 操作日志记录
    ├── 异步日志处理
    └── 日志查询接口
```

## 🚀 技术栈

### 核心框架
- **Spring Boot 2.6.3** - 微服务基础框架
- **Spring Cloud 2021.0.1** - 微服务治理
- **Spring Cloud Alibaba 2021.0.1.0** - 阿里巴巴微服务组件
- **Nacos** - 服务注册与发现
- **OpenFeign** - 服务间通信

### 数据存储
- **MySQL 8.0.28** - 主数据库
- **Redis** - 缓存和Token白名单
- **MyBatis Plus 3.5.3.1** - ORM框架

### 其他组件
- **JWT 0.9.1** - 身份认证
- **RabbitMQ** - 消息队列
- **Seata 2.1.0** - 分布式事务
- **Lombok** - 代码简化
- **Hutool 5.8.12** - 工具类库

## ✨ 功能特性

### 🔐 用户管理
- ✅ 用户注册/登录
- ✅ JWT Token认证
- ✅ Token白名单管理
- ✅ 用户信息查询和更新
- ✅ 密码加密存储
- ✅ 登录状态管理

### 👥 权限管理
- ✅ 角色管理（用户/管理员）
- ✅ 默认角色自动绑定
- ✅ 角色升级/降级
- ✅ 基于角色的数据可见性控制
- ✅ 权限验证拦截器

### 📊 日志管理
- ✅ 操作日志异步记录
- ✅ 用户行为追踪
- ✅ 日志持久化存储
- ✅ 日志查询接口

### 🛡️ 安全特性
- ✅ JWT Token验证
- ✅ 请求拦截和权限校验
- ✅ 密码加密
- ✅ Token白名单机制
- ✅ 服务降级处理


### 环境要求
- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.0+
- RabbitMQ 3.8+


## 🏗️ 项目结构

```
user-permission-system/
├── common/                     # 公共模块
│   ├── src/main/java/
│   │   └── com/ajiang/common/
│   │       ├── exception/      # 异常处理
│   │       ├── model/          # 通用模型
│   │       ├── util/           # 工具类
│   │       └── config/         # 配置类
│   └── pom.xml
├── user-service/               # 用户服务
│   ├── src/main/java/
│   │   └── com/ajiang/userservice/
│   │       ├── controller/     # 控制器
│   │       ├── service/        # 业务逻辑
│   │       ├── entity/         # 实体类
│   │       ├── mapper/         # 数据访问
│   │       ├── feignclient/    # 远程调用
│   │       └── interceptor/    # 拦截器
│   └── pom.xml
├── permission-service/         # 权限服务
│   ├── src/main/java/
│   │   └── com/ajiang/permissionservice/
│   │       ├── controller/     # 控制器
│   │       ├── service/        # 业务逻辑
│   │       ├── entity/         # 实体类
│   │       └── mapper/         # 数据访问
│   └── pom.xml
├── logging-service/            # 日志服务
│   ├── src/main/java/
│   │   └── com/ajiang/loggingservice/
│   │       ├── controller/     # 控制器
│   │       ├── service/        # 业务逻辑
│   │       ├── entity/         # 实体类
│   │       └── mapper/         # 数据访问
│   └── pom.xml
└── pom.xml                     # 父级POM文件
```

## 🔍 核心组件说明

### JWT工具类 (JwtUtil)
- Token生成和验证
- 用户信息提取
- Token过期处理

### Token白名单服务 (TokenWhitelistService)
- Token白名单管理
- Redis存储Token状态
- 用户登出Token清理

### 权限拦截器 (TokenValidationInterceptor)
- 请求拦截和Token验证
- 白名单路径跳过
- 权限校验失败处理

### 服务间通信 (OpenFeign)
- 权限服务客户端
- 服务降级处理
- 负载均衡

## 🚨 注意事项

1. **安全配置**：请确保在生产环境中修改默认的JWT密钥和数据库密码
2. **服务依赖**：启动顺序建议为 Nacos → Redis → MySQL → 各微服务
3. **端口配置**：确保各服务端口不冲突，默认端口需要在配置文件中指定
4. **数据库初始化**：首次运行需要创建相应的数据库表结构
5. **消息队列**：日志服务依赖RabbitMQ，请确保消息队列正常运行


## 🔄 版本历史

- **v1.0.0** (2025-06-17)
    - 初始版本发布
    - 实现用户管理基础功能
    - 实现角色权限管理
    - 实现操作日志记录
    - 支持微服务架构

## 存在问题
- seata 与 ShardingSphere 分库分表集成有问题，事务回滚时，仍插入新数据

## 👨‍💻 开发团队

- **作者**: ajiang
- **邮箱**: [联系邮箱]
- **版本**: 1.0.0

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

---

如有问题或建议，欢迎提交 Issue 或 Pull Request！