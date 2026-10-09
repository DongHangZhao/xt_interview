# XT 面试项目开发任务说明

## 一、项目简介

本项目是一个基于 Spring Boot + Vue 3 的简化版用户管理系统，作为技术面试的开发任务基础。

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 4.0.6, Java 21, JPA/Hibernate, Flyway, JWT |
| 前端 | Vue 3.4, Vite 5, TypeScript, Element Plus, Pinia |
| 数据库 | MySQL 8.0 |
| 构建 | Maven 3.6+, npm |

### 已有功能

- **用户登录**：基于 JWT 的身份认证，登录后获取 token
- **用户管理**：账号的增、删、改、查（CRUD）操作
- **侧边栏导航**：左侧导航栏，当前仅包含"用户管理"菜单

---

## 二、开发环境搭建

### 1. 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS xt_interview DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

数据库连接信息（默认）：
- 地址：`localhost:3306`
- 用户名：`root`
- 密码：`root`
- 数据库名：`xt_interview`

### 2. 后端启动
后端运行在 `http://localhost:8080`，Flyway 会自动执行迁移脚本创建表结构。

### 3. 前端启动
前端运行在 `http://localhost:5173`，已配置代理将 `/api` 请求转发到后端 8080 端口。

### 4. 默认账号
- 用户名：`admin`
- 密码：`123456`

---

## 三、开发任务

请基于现有项目，完成以下 **2 个必做任务** 和 **1 个选做任务**。

---

### 任务一：部门管理模块（必做）

#### 需求描述

新增一个部门管理模块，实现部门的增删改查功能。

#### 功能要求

1. **数据库**：
   - 新建 `部门` 相关的 Flyway 迁移脚本 
   - 要求脚本创建 `sys_department` 表，字段如下：

   | 字段 | 类型 | 说明 |
   |------|------|------|
   | id | BIGINT, AUTO_INCREMENT, 主键 | 部门ID |
   | name | VARCHAR(100), NOT NULL | 部门名称 |
   | code | VARCHAR(50), NOT NULL, UNIQUE | 部门编码 |
   | description | VARCHAR(500) | 部门描述 |
   | status | INT DEFAULT 1 | 状态：0-禁用，1-启用 |
   | deleted | INT DEFAULT 0 | 是否删除：0-未删除，1-已删除 |
   | create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |
   | update_time | DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

   - 插入 2 条测试数据（如：研发部、运维部）

2. **后端**：
   - 参考用户管理管理，创建 `部门管理模块`所需的 实体类、接口、Service、实现、Controller。
   - 实现一下接口

   | 方法 | 路径 | 说明 |
   |------|------|------|
   | GET | `/api/departments` | 获取所有部门 |
   | GET | `/api/departments/{id}` | 根据ID获取部门 |
   | POST | `/api/departments` | 创建部门 |
   | PUT | `/api/departments/{id}` | 更新部门 |
   | DELETE | `/api/departments/{id}` | 删除部门（软删除） |

3. **前端**：
   - 新增 部门管理的页面：`DepartmentManagement.vue` 页面，参考 `UserManagement.vue` 的实现方式
   - 在侧边栏导航中新增"部门管理"菜单项
   - 在路由配置中新增部门管理路由
   - 实现新增部门相关的 API 封装

---

### 任务二：用户-部门关联（必做）

#### 需求描述

在任务一的基础上，为用户添加所属部门，实现用户与部门的关联。

#### 功能要求

1. **数据库**：
   - 新建 Flyway 迁移脚本，给 `sys_account` 表新增 `department_id` 字段（BIGINT, 可空, 外键关联 `sys_department.id`）

2. **后端**：
   - 修改 `Account` 实体，新增 `departmentId` 字段
   - 修改 `AccountDTO`，新增 `departmentId` 和 `departmentName` 字段
   - 修改 `AccountServiceImpl`，在创建/更新用户时处理 `departmentId`
   - 查询用户列表时，关联查询部门名称（可用 JPA 关联查询或手动关联）

3. **前端**：
   - 用户管理页面的添加/编辑表单中新增"所属部门"下拉选择框
   - 用户列表表格中新增"所属部门"列，显示部门名称
   - 添加/编辑时部门为可选字段

---

### 任务三：用户列表搜索与分页（选做）

#### 需求描述

当用户数量较多时，需要支持按条件搜索和分页展示。

#### 功能要求

1. **后端**：
   - 新增 `GET /api/accounts/page` 分页查询接口
   - 支持按用户名模糊搜索、按状态筛选、按权限级别筛选
   - 支持分页参数 `page`（页码，从0开始）和 `size`（每页条数）
   - 返回分页数据：`{ list, totalElements, totalPages, currentPage, pageSize }`

2. **前端**：
   - 用户管理页面新增搜索栏（用户名输入框、状态下拉、权限级别下拉、搜索按钮）
   - 表格底部新增 `el-pagination` 分页组件
   - 搜索和翻页时调用分页接口

---

## 四、提交要求

### 1. 代码提交

- 请将完成后的代码推送到指定的 Git 仓库
- 提交信息请使用清晰的中文描述，例如：`新增部门管理模块`
- 确保代码能正常编译和运行

### 2. 验收标准

| 验收项 | 要求 |
|--------|------|
| 后端编译 | `mvn compile` 无报错 |
| 后端启动 | `mvn spring-boot:run` 正常启动，Flyway 迁移成功 |
| 前端启动 | `npm run dev` 正常启动，页面可访问 |
| 登录功能 | 使用 admin/123456 可正常登录 |
| 任务一 | 部门 CRUD 功能正常，侧边栏显示部门管理菜单 |
| 任务二 | 用户关联部门正常，添加/编辑用户时可选择部门 |
| 任务三（选做） | 搜索和分页功能正常 |

---

## 五、其他要求

### JWT 认证流程

1. 用户调用 `/api/auth/login` 接口，提交用户名和密码
2. 后端验证通过后，生成 JWT token 返回
3. 前端将 token 存入 `localStorage`
4. 后续请求通过 axios 拦截器在请求头添加 `Authorization: Bearer <token>`
5. 后端 `JwtAuthenticationFilter` 验证 token 有效性

### 认证相关

除登录接口外，所有接口需在请求头中携带 JWT token：

```
Authorization: Bearer <your_token>
```

登录成功后，响应体中 `token` 字段即为 JWT token。


### 前端路由守卫

未登录用户访问需要认证的页面时，自动重定向到登录页。登录成功后跳转到目标页面。

### 密码加密

系统使用 MD5 加密存储密码（`MD5Util.encode` 方法）。创建用户时如果未传密码，默认使用 `123456`。

### 软删除

用户删除为软删除（`deleted` 字段标记），不会从数据库物理删除。查询时只返回 `deleted = 0` 的记录。

