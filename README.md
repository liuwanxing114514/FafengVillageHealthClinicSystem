# 发凤村卫生室诊所辅助系统

家庭诊所辅助软件：药品管理、库存流水、患者病历、处方打印。第一版（v0.1–v1.0）不依赖 AI。

## 技术栈

- 后端：Java 21 + Spring Boot 3 + MyBatis-Plus + Flyway
- 前端：Vue 3 + TypeScript + Vite + Element Plus
- 数据库：PostgreSQL 16（pgvector 镜像）

## 快速启动（Docker）

```powershell
# 1. 复制环境变量
copy .env.example .env
# 编辑 .env，至少修改 POSTGRES_PASSWORD

# 2. 启动全部服务
docker compose up -d --build

# 3. 验证
curl http://localhost:8080/api/health
# 浏览器访问 http://localhost
```

## 本地开发（不用 Docker 跑应用）

### 前置

- JDK 21、Maven 3.9+
- Node.js 22+
- PostgreSQL（推荐 `pgvector/pgvector:pg16` 容器仅跑数据库）

### 数据库

```powershell
docker run -d --name clinic-pg -e POSTGRES_DB=clinic -e POSTGRES_USER=clinic -e POSTGRES_PASSWORD=change_me -p 5432:5432 pgvector/pgvector:pg16
```

### 后端

```powershell
cd backend
mvn spring-boot:run
```

### 前端

```powershell
cd frontend
npm install
npm run dev
```

开发服：http://localhost:5173（API 代理到 8080）

## 目录说明

| 目录 | 说明 |
| --- | --- |
| `backend/` | Spring Boot 模块化单体 |
| `frontend/` | Vue SPA |
| `scripts/` | 备份/恢复脚本（v1.0 实装） |
| `docs/` | 设计文档与开发指南 |

## 版本规划

见 [`docs/共用/ROADMAP.md`](docs/共用/ROADMAP.md)。

当前分支 **v0.1-skeleton**：仅项目骨架 + Flyway V0（启用 `vector`、`pg_trgm` 扩展）。

## 文档

- 架构设计：[`docs/设计/v0.1-架构设计.md`](docs/设计/v0.1-架构设计.md)
- 开发交接：[`docs/给Agent/开发交接.md`](docs/给Agent/开发交接.md)
- 部署指南：[`docs/共用/DEPLOYMENT.md`](docs/共用/DEPLOYMENT.md)
