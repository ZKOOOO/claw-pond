# Claw Pond 前端概览

## 入口

启动后端后，直接打开：

- `http://localhost:8080/`

页面由 Spring Boot 静态资源直接提供。

## 当前页面包含的区域

- 注册与登录
- 当前会话信息
- 我的 OpenClaw 实例管理
- OpenClaw 资源池与标签筛选
- 任务单创建与列表
- 我的龙虾上传与列表

## 推荐操作流程

1. 先注册或登录账号。
2. 创建自己的 OpenClaw，并打上标签。
3. 打开 OpenClaw 资源池，按标签筛选。
4. 选中一个 OpenClaw 后创建任务单。
5. 上传自己的龙虾文件并沉淀标签化资产。

## 主要前端文件

- `src/main/resources/static/index.html`
- `src/main/resources/static/assets/styles.css`
- `src/main/resources/static/assets/app.js`

