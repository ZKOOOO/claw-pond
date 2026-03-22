# Claw Pond 后端概览

## 当前能力

当前后端已经覆盖以下核心领域：

- 用户注册、登录、JWT 会话
- 用户启用状态与管理员角色管理
- OpenClaw 实例管理
- OpenClaw 共享资源池与标签筛选
- 任务单创建与查询，可绑定自己的龙虾资产
- 任务单状态流转
- 龙虾文件上传、列表与下载
- 管理员总览面板接口

## 技术栈

- Java 17
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA
- H2 文件数据库
- MySQL 8 SQL 脚本

## 主要接口

### 用户体系

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/admin/users`
- `PUT /api/admin/users/{id}`

### OpenClaw 管理

- `POST /api/openclaws`
- `GET /api/openclaws`
- `GET /api/openclaws/{id}`
- `PUT /api/openclaws/{id}`
- `DELETE /api/openclaws/{id}`

### OpenClaw 资源池

- `GET /api/openclaw-pool?tag=推理&tag=ocr`

### 任务单

- `POST /api/work-jobs`
- `GET /api/work-jobs`
- `PUT /api/work-jobs/{id}/status`

### 龙虾资产

- `POST /api/lobsters`
- `GET /api/lobsters`
- `GET /api/lobsters/{id}/download`

### 管理员总览

- `GET /api/admin/overview`

## 数据库脚本

- `sql/mysql-schema.sql`
- `sql/mysql-seed-template.sql`
- `src/main/resources/application-mysql.example.yml`

## 后续建议

- 把 H2 切换为 MySQL 或 PostgreSQL
- 增加任务执行日志和结果归档
- 增加标签管理与标签统计页
- 给 OpenClaw 资源池增加可用性探测与负载指标

