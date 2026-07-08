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

**当前进度**：`develop` 已完成 v0.5（处方/打印）。v0.6–v1.0 见 [`docs/给Agent/进度清单-v0.5-v1.0.md`](docs/给Agent/进度清单-v0.5-v1.0.md)。

## Agent / Cursor 开发提示

本项目在 Cursor 中配置了 **MCP 工具**，开发时 Agent 应优先使用（若已启用）：

| MCP | 用途 | 典型场景 |
| --- | --- | --- |
| **user-jetbrains** | IntelliJ IDEA 集成 | `mvn test`、查看编译错误、运行 `ClinicApplication`、符号搜索 |
| **user-datagrip** | DataGrip 数据库 | 验证 Flyway 迁移、查表数据、执行 SQL |

### DataGrip MCP

DataGrip MCP 绑定的是 **DataGrip 里已配置的数据源**，与 Git 仓库路径无关。只要 DataGrip 已打开任意项目（如 `C:/Users/lwx/DataGripProjects/test`），即可调用。

```text
list_database_connections  → 获取 connectionId（如 postgres@localhost）
execute_sql_query          → 必填 connectionId、databaseName、schemaName、queryText
```

本地 dev 连接参数（见 `application-dev.yml`）：

| 参数 | 值 |
| --- | --- |
| `projectPath` | `C:/Users/lwx/DataGripProjects/test`（或当前打开的 DG 项目） |
| `connectionId` | `list_database_connections` 返回的 `postgres@localhost` id |
| `databaseName` | `postgres` |
| `schemaName` | `public` |

### IDEA MCP 跑 Maven

使用 `execute_terminal_command` 时，**需手动设置 JAVA_HOME**（MCP 终端不会自动继承 IDEA 项目 JDK）：

```powershell
$env:JAVA_HOME='C:\Users\lwx\.jdks\corretto-21.0.11'
cd 'D:\xiangmu\发凤村卫生室'
mvn -pl backend test
```

Maven 路径示例：`D:\soft\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd`

也可用 `get_project_problems` 查编译错误；或在 IDEA 中为 `backend test` 建 Run Configuration，用 `execute_run_configuration` 执行。

**建议流程**：

1. 改 Flyway 迁移后 → DataGrip MCP 查 `information_schema.tables` 或启动应用验证
2. 改 Java 后 → IDEA MCP `get_project_problems` 或 `mvn test`（带 JAVA_HOME）
3. 自测后 → DataGrip MCP 查 `inventory_batch`、`inventory_flow` 等表数据

## 文档

- 架构设计：[`docs/设计/v0.1-架构设计.md`](docs/设计/v0.1-架构设计.md)
- 开发交接：[`docs/给Agent/开发交接.md`](docs/给Agent/开发交接.md)
- **进度清单**：[`docs/给Agent/进度清单-v0.5-v1.0.md`](docs/给Agent/进度清单-v0.5-v1.0.md)
- 部署指南：[`docs/共用/DEPLOYMENT.md`](docs/共用/DEPLOYMENT.md)
