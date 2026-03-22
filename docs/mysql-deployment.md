# MySQL 部署说明

## 文件位置

- 建表脚本：`sql/mysql-schema.sql`
- 初始化管理员模板：`sql/mysql-seed-template.sql`
- Spring Boot 配置样例：`src/main/resources/application-mysql.example.yml`

## 推荐环境

- MySQL 8.0+
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_0900_ai_ci`

## 执行顺序

1. 在 MySQL 中执行 `sql/mysql-schema.sql`
2. 如需初始化管理员，再执行 `sql/mysql-seed-template.sql`
3. 把 `src/main/resources/application-mysql.example.yml` 复制成你自己的配置并修改账号密码
4. 把应用的 `spring.datasource.*` 指向 MySQL
5. 将 `spring.jpa.hibernate.ddl-auto` 保持为 `validate`

## 说明

- 当前 SQL 与现有实体模型保持一致，包括：
  - 用户表
  - 用户启用状态字段
  - OpenClaw 实例表
  - 标签表
  - OpenClaw 与标签关联表
  - 任务单表
  - 任务单与龙虾关联字段
  - 任务单标签关联表
  - 龙虾资产表
  - 龙虾标签关联表
- UUID 统一使用 `CHAR(36)`
- 上传文件本体仍然保存在磁盘目录 `data/uploads/lobsters`

