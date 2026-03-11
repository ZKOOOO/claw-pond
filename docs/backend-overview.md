# Claw Pond 后端概览

## 当前能力

当前后端已经覆盖以下核心领域：

- 用户注册、登录、JWT 会话
- OpenClaw 实例管理
- OpenClaw 共享资源池与标签筛选
- 任务单创建与查询
- 龙虾文件上传、列表与下载

## 技术栈

- Java 17
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA
- H2 文件数据库

## 主要接口

### 用户体系

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

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

### 龙虾资产

- `POST /api/lobsters`
- `GET /api/lobsters`
- `GET /api/lobsters/{id}/download`

## 后续建议

- 把 H2 切换为 MySQL 或 PostgreSQL
- 增加任务状态流转和调度日志
- 给龙虾资产增加版本号和可见范围
- 给 OpenClaw 资源池增加可用性探测与负载指标

