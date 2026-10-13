<div align="center">

# 欣铁面试项目 · xt_interview

**Spring Boot 4 + Vue 3 的用户/部门管理系统**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-42b883.svg)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-00758F.svg)](https://www.mysql.com/)
[![Flyway](https://img.shields.io/badge/Flyway-9-red.svg)](https://documentation.red-gate.com/flyway)

> 基于开源骨架 [iamtcby5188/xt_interview](https://github.com/iamtcby5188/xt_interview) 完成三项开发任务，
> 并修复了骨架中遗留的越权漏洞。

</div>

---

## 📖 这是什么

一个可运行、可演示、可讲解的后台管理系统，用于 **Java 后端岗位的面试实战**。

不是玩具 demo —— 包含真实的权限体系、数据库迁移、异常处理，
以及几个在真实项目中会遇到的坑的解决方案。

**在线预览**：本地启动后访问 `http://localhost:5173`

---

## ✨ 核心特性

### 🔐 完整的 RBAC 权限体系

项目骨架中 `roleLevel` 字段**从未被任何代码用于鉴权** —— 任意登录用户都能创建管理员。
本项目重建了完整的权限体系：

```
普通用户(1)  只能查看/修改自己，无任何管理权限
管理员(2)    可管理用户部门，不能创建同级及以上、不能操作同级及以上、不能操作自己
超管(3)      可管理一切（含创建同级超管），唯一仍禁止操作自己
```

四处关键设计：

| 设计 | 解决的问题 |
|------|-----------|
| JWT 每次请求回库查实时权限 | 权限下调后，旧 token 仍持有高权限 |
| ThreadLocal 在 `finally` 清理 | Tomcat 线程复用导致权限串号 |
| 「改自己」接口用字段白名单 | 用户提交 `roleLevel:3` 自我提权 |
| 同级保护用 `>=` 而非 `>` | 两个管理员配合把第三个升为超管（横向提权） |

实测 18 项越权场景全部正确拦截：

| 身份 | 操作 | 结果 |
|------|------|------|
| 普通用户 | 创建管理员 / 查看他人 / 创建部门 | ❌ 403 |
| 管理员 | 创建超管 / 创建同级 / 设 isSuper / 改超管 / 删自己 | ❌ 403 |
| 超管 | 创建超管 / 修改他人 | ✅ 放行 |
| 超管 | 修改自己 | ❌ 403 |

### 🗄️ 版本化数据库迁移

Flyway 管理全部表结构，5 个迁移脚本清晰可追溯：

| 版本 | 内容 |
|------|------|
| V1 | 建 `sys_account` 表 + 种子数据 |
| V2 | 建 `sys_department` 表 |
| V3 | 加 `department_id` 字段 + 外键约束 |
| V4 | 唯一索引降级为普通索引 |
| V5 | 企业级演示数据（18 部门 + 48 账号） |

### 🔍 搜索与分页

Specification 动态查询，支持用户名模糊搜索、状态筛选、权限筛选及任意组合。
分页边界做了完整防护（负数页码、越界页、超大页长）。

### 📊 企业级演示数据

内置 21 个部门 / 50 个账号 的完整组织架构，含三级权限分布（5 名超管 / 10 名管理员 / 35 名员工）与 3 个禁用账号，可直接演示各种业务场景。

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.6 | 应用框架 |
| Java | 21 | 语言 |
| Spring Data JPA | 4.x | ORM |
| Flyway | 9.x | 数据库版本迁移 |
| Spring Security | 7.x | 权限框架集成 |
| JJWT | 0.12.x | JWT 签发与校验 |
| MySQL | 8.0 | 数据库 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 渐进式框架 |
| Vite | 5.x | 构建工具 |
| TypeScript | 5.x | 类型系统 |
| Element Plus | 2.6 | UI 组件库 |
| Pinia | 2.1 | 状态管理 |
| Vue Router | 4.3 | 路由 |

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+

### 一、初始化数据库

```bash
mysql -uroot -p
```

```sql
CREATE DATABASE `xt_interview` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 🔴 使用业务账号而非 root，遵循最小权限原则
CREATE USER 'admin'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON `xt_interview`.* TO 'admin'@'localhost';
FLUSH PRIVILEGES;
```

### 二、启动后端

```bash
git clone https://github.com/DongHangZhao/xt_interview.git
cd xt_interview

mvn spring-boot:run
```

首次启动会自动执行 Flyway 迁移并写入演示数据。
看到这两行即成功：

```
Tomcat started on port 8080 (http) with context path '/'
Started XtInterviewApplication in 6.0 seconds
```

### 三、启动前端

```bash
cd frontend/web
npm install
npm run dev
```

访问 **http://localhost:5173**

> 💡 数据库连接已通过环境变量占位符外置：
> `spring.datasource.password=${DB_PASSWORD:123456}`
> 本地开箱即用，部署时注入环境变量即可，无需改代码。

---

## 🔑 测试账号

密码统一为 `123456`

| 身份 | 用户名 | 权限范围 |
|------|--------|---------|
| 🔴 超级管理员 | `emp_admin` | 全部功能 |
| 🟡 部门管理员 | `emp_dev_mgr` | 管用户部门，不能管同级 |
| 🟢 普通员工 | `emp_dev01` | 只能查看和修改自己 |
| ⚫ 禁用账号 | `emp_leave01` | 登录被拒绝 |

**建议体验方式**：三个账号分别登录，观察左侧菜单差异。
普通用户只能看到「个人中心」，管理员才能看到「用户管理」和「部门管理」。

---

## 📡 API 文档

共 15 个接口。

### 认证

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录，返回 JWT | 公开 |
| POST | `/api/auth/logout` | 登出 | 公开 |
| GET | `/api/auth/validate` | 校验 token | 公开 |

### 账号管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/accounts` | 账号列表 | 管理员+ |
| GET | `/api/accounts/{id}` | 账号详情 | 管理员+ 或本人 |
| GET | `/api/accounts/page` | 分页查询 | 管理员+ |
| POST | `/api/accounts` | 创建账号 | 管理员+ 且目标权限低于自己 |
| PUT | `/api/accounts/{id}` | 更新账号 | 管理员+ 且目标权限低于自己 |
| DELETE | `/api/accounts/{id}` | 删除账号 | 管理员+ 且目标权限低于自己 |
| PUT | `/api/accounts/my-profile` | 修改我的资料 | 登录用户本人 |

**分页参数**：`page`（从 0 开始）、`size`、`username`（模糊）、`status`、`roleLevel`

```json
{
  "list": [],
  "totalElements": 0,
  "totalPages": 0,
  "currentPage": 0,
  "pageSize": 10
}
```

### 部门管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/departments` | 部门列表 | 登录用户 |
| GET | `/api/departments/{id}` | 部门详情 | 登录用户 |
| POST | `/api/departments` | 创建部门 | 管理员+ |
| PUT | `/api/departments/{id}` | 更新部门 | 管理员+ |
| DELETE | `/api/departments/{id}` | 删除部门 | 管理员+ 且部门下无用户 |

---

## 📁 项目结构

```
xt_interview/
├── pom.xml
├── docs/                          📖 详细文档
│   ├── 01-小白操作手册.md           环境搭建 + 操作 + 排错
│   ├── 02-面试交付文档.md          架构设计 + 技术亮点
│   └── 03-常见问题答疑.md          踩坑记录 + 面试问答
├── src/main/
│   ├── java/com/xt/interview/
│   │   ├── config/                配置：Security / CORS
│   │   ├── controller/            接口层（不含业务逻辑）
│   │   ├── service/               业务层（承载业务规则）
│   │   │   └── impl/
│   │   ├── repository/            数据层（Specification + 联表 SQL）
│   │   ├── entity/                数据库表映射
│   │   ├── dto/                   前后端传输对象
│   │   ├── security/              ★ PermissionChecker 权限核心
│   │   ├── exception/             统一异常体系
│   │   ├── enums/                 RoleLevel
│   │   └── util/                  JWT / MD5 / 软删除编码
│   └── resources/
│       ├── application.properties
│       └── db/migration/          ★ Flyway 迁移脚本 V1~V5
└── frontend/web/src/
    ├── api/                       接口封装 + 类型定义
    ├── router/                    路由 + 权限守卫
    ├── stores/                    Pinia 登录状态
    ├── utils/                     axios 封装
    ├── layouts/                   侧边栏框架
    └── views/                     Login / User / Department / Profile
```

---

## 💡 技术亮点

### 1. 软删除与唯一索引的正确处理

**问题**：唯一索引作用于**物理行**，软删除只标记 `deleted=1`，行还在、索引还占着编码，
导致删除后编码被永久占用。

**方案**：软删除时自动改写唯一码

```
删除前：  test
删除后：  test__del_1789012345678    ← 行保留（可审计），原码已释放
重建：    用 test → ✅ 成功
```

同时唯一索引降级为普通索引，唯一性交由应用层负责。

> ⚠️ 明确否决了 `UNIQUE(code, deleted)` 方案 —— 连续删除两条同 code 记录时，
> 两行均为 `(code, 1)` 会再次撞索引，是定时炸弹。

### 2. N+1 查询规避

部门名称需随用户列表返回。直接实现会是 N+1（返回 N 个用户 = N+1 次查询）。
本项目用单条联表 SQL 构造映射，在内存中组装 DTO：

```java
@Query(value = "SELECT a.id, d.name FROM sys_account a "
        + "JOIN sys_department d ON a.department_id = d.id "
        + "WHERE a.id IN (:ids)", nativeQuery = true)
List<Object[]> findDepartmentNamesByIdIn(@Param("ids") List<Long> ids);
```

整个接口固定 **2 条 SQL**，与数据量无关。

### 3. 统一异常体系

```
BusinessException(message, code)      业务异常 → 400/403
@RestControllerAdvice
  ├─ BusinessException                → 返回对应状态码
  ├─ MethodArgumentNotValidException  → 提取字段级错误提示
  ├─ DataIntegrityViolationException  → 翻译并发唯一索引冲突
  └─ Exception                       → 500，不泄露堆栈
```

解决的问题：骨架无全局处理器时，Spring 返回 HTML 错误页，前端读不到 `message` 字段。

### 4. 前后端页码基数转换

后端 `page` 按需求从 0 开始，Element Plus `el-pagination` 从 1 开始。
用 `v-model` 双向绑定会导致值污染翻页错乱，改为单向绑定 + 事件内转换：

```vue
:current-page="query.page + 1"
@current-change="handlePageChange"
```

---

## 🐛 开发中踩过的坑

<details>
<summary><b>点击展开</b>（面试可讲的真实案例）</summary>

### Spring Boot 4 / Spring Data JPA 4 接口变更

`Specification` 查询不再混入 `JpaRepository`，必须显式继承：

```java
extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account>
```

社区上 Boot 2.x/3.x 的资料均不适用。

### IDE 环境变量劫持端口

IDE 注入的 `SERVER__PORT=7218` 被 Spring Boot 宽松绑定映射为 `server.port`，
覆盖了配置文件中的 8080。

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

### Flyway 失败标记锁死启动

迁移失败后 `flyway_schema_history` 留下 `success=0` 记录，
之后每次启动都失败且**不重跑**。

```sql
DELETE FROM flyway_schema_history WHERE success = 0;
```

> 更重要的教训：**已执行成功的迁移脚本永不允许修改**，
> 改动必须新建编号更大的脚本。

### 迁移脚本硬编码自增主键

部门 ID 因测试数据漂移导致外键失败（MySQL Error 1452）。
改用子查询按业务编码关联：

```sql
department_id = (SELECT id FROM sys_department WHERE code = 'IT_DEV')
```

### 骨架遗留的 TypeScript 错误

`AxiosResponse` 赋给 `any[]` 报 TS2740 —— 响应拦截器剥离了外壳但类型声明未同步。
通过泛型化封装函数重新声明返回类型解决。现 `vue-tsc --noEmit` 零报错。

</details>

完整记录见 [`docs/03-常见问题答疑.md`](docs/03-常见问题答疑.md)

---

## ✅ 质量保障

| 检查项 | 命令 | 结果 |
|--------|------|------|
| 后端编译 | `mvn clean compile` | 零报错 |
| 前端类型 | `npx vue-tsc --noEmit` | 零报错 |
| 前端构建 | `npm run build` | 成功 |
| 功能验收 | curl 实测 | 7 项全通过 |
| 权限验证 | curl 实测 | 18 项越权场景全部拦截 |

### 分页边界防护

| 输入 | 处理 |
|------|------|
| `page < 0` | 兜底为 0 |
| `size <= 0` | 兜底为 1 |
| `size > 100` | 限制为 100，防止单次拉全表 |
| 翻页超出总页 | 返回空列表，不报错 |
| 删除当前页最后一条 | 自动回退一页，不停留空白页 |

---

## ⚠️ 已知限制

诚实列出当前不足：

1. **密码使用 MD5** —— 存量兼容问题，`SecurityConfig` 已备好 `PasswordEncoder`（BCrypt），待择机切换
2. **无刷新令牌** —— JWT 有效期 24 小时，过期需重新登录
3. **部门无层级** —— 当前为平铺结构 + 编码语义分组，未建 `parent_id` 树形关系
4. **无自动化测试** —— 功能验证依赖 curl 实测，未编写单元测试
5. **令牌存于 localStorage** —— 存在 XSS 风险，生产环境应改用 httpOnly Cookie

---

## 📄 相关文档

| 文档 | 内容 |
|------|------|
| [📘 小白操作手册](docs/01-小白操作手册.md) | 环境搭建、完整操作流程、常见报错解决 |
| [📗 面试交付文档](docs/02-面试交付文档.md) | 架构设计、技术亮点、技术决策记录 |
| [📕 常见问题答疑](docs/03-常见问题答疑.md) | 12 个踩坑记录 + 预判面试问答 |

---

## 📄 License

本项目仅用于学习与面试展示。

---

<div align="center">

**如果这个项目对你有帮助，欢迎点 ⭐ Star**

</div>