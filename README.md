# 发凤村卫生室诊所辅助系统

家庭诊所辅助软件：药品管理、库存流水、患者病历、处方打印。第一版（v0.1–v1.0）不依赖 AI。

## 技术栈

- 后端：Java 21 + Spring Boot 3 + MyBatis-Plus + Flyway
- 前端：Vue 3 + TypeScript + Vite + Element Plus
- 数据库：PostgreSQL 16（pgvector 镜像）

## 配置与环境

后端使用 **Spring Profile**，由 Maven 打包时切换：

| Profile | 用途 | 打包命令 |
| --- | --- | --- |
| `dev` | 本地 IDEA 开发（默认） | `mvn package` 或 IDEA Run（Active profiles: `dev`） |
| `docker` | Docker Compose | `mvn -Pdocker package` |
| `prod` | 诊所机器部署 | `mvn -Pprod package` |

配置文件：

- `application.yml` — 公共配置
- `application-dev.yml` — 本地数据库（postgres / admin）
- `application-docker.yml` — 容器内网数据库
- `application-prod.yml` — 生产部署（密码可用 `CLINIC_DB_PASSWORD` 覆盖）

`.env` **仅 Docker Compose 可选**（改端口、数据库密码、数据目录）；本地 IDEA 开发不需要。不创建 `.env` 也能 `docker compose up`，会用 `docker-compose.yml` 里的默认值。

## 快速启动（Docker）

```powershell
# 可选：copy .env.example .env  # 仅当需要改密码或端口时
docker compose up -d --build
```

后端镜像构建时已使用 `-Pdocker`，无需再传 `SPRING_DATASOURCE_*`。

## 本地开发（IDEA）

1. JDK 21 + Maven 导入项目
2. Run `ClinicApplication`，**Active profiles** 填 `dev`（或使用 `.run/ClinicApplication.run.xml`）
3. 修改 `application-dev.yml` 中的数据库连接（默认 postgres / admin）
4. 验证：`http://localhost:8080/api/health`

```powershell
# 或命令行
mvn -pl backend spring-boot:run
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

**当前进度**：`develop` 已完成 v0.4（患者/病历）。v0.5–v1.0 见 [`docs/给Agent/进度清单-v0.5-v1.0.md`](docs/给Agent/进度清单-v0.5-v1.0.md)。

## Agent / Cursor 开发提示

本项目在 Cursor 中配置了 **MCP 工具**，开发时 Agent 应优先使用（若已启用）：

| MCP | 用途 | 典型场景 |
| --- | --- | --- |
| **user-jetbrains** | IntelliJ IDEA 集成 | 运行 `ClinicApplication`、查看编译错误、`mvn test`、重构、符号搜索 |
| **user-datagrip** | DataGrip 数据库 | 列出连接、`execute_sql_query` 验证 Flyway 迁移结果、预览表数据、检查索引 |

**建议流程**：

1. 改 Flyway 迁移后 → DataGrip 执行 SQL 或启动应用验证表结构
2. 改 Java 后 → JetBrains `build_project` / `get_project_problems` 查编译问题
3. 自测前 → DataGrip 查 `prescription`、`inventory_flow` 等表数据是否正确

本地 dev 数据库默认见 `application-dev.yml`（PostgreSQL，库名 `clinic`）。

## 文档

- 架构设计：[`docs/设计/v0.1-架构设计.md`](docs/设计/v0.1-架构设计.md)
- 开发交接：[`docs/给Agent/开发交接.md`](docs/给Agent/开发交接.md)
- **进度清单**：[`docs/给Agent/进度清单-v0.5-v1.0.md`](docs/给Agent/进度清单-v0.5-v1.0.md)
- 部署指南：[`docs/共用/DEPLOYMENT.md`](docs/共用/DEPLOYMENT.md)
