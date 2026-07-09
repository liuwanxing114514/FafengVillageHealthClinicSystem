# 发凤村卫生室 · 诊所辅助系统

> 面向基层诊所的全栈管理系统：药品库存、患者病历、处方打印，可选 AI（结构化整理、OCR 入库、Agent 工具调用、RAG 相似病例）。  
> 源自真实村卫生室场景，**v3.0 已可用于生产部署**（Docker Compose / 群晖 NAS）。

[![Java 21](https://img.shields.io/badge/Java-21-blue)](backend/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-green)](backend/)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883)](frontend/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-336791)](docker-compose.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 为什么值得关注

| 维度 | 说明 |
| --- | --- |
| **真实落地** | 为发凤村卫生室定制，覆盖日常进销存、病历、处方全流程 |
| **渐进式架构** | v0.1 骨架 → v1.0 无 AI 可用 → v2.x 叠加 Agent / RAG，共 20+ 版本分支可学习 |
| **模块化单体** | 单 Spring Boot + 单 Vue SPA，按 domain 分包，非微服务但边界清晰 |
| **AI 可拔插** | 关闭 API Key 即退化为稳定 MVP；开启后支持 Spring AI Agent、pgvector RAG |
| **隐私设计** | 调用外部 AI 前自动脱敏姓名、手机、身份证、门牌（库内仍存原文） |
| **可运维** | Flyway 14 版迁移、backup/restore 脚本、NAS 部署与验收清单 |

---

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 21 · Spring Boot 3.4 · Spring Security · MyBatis-Plus · Flyway |
| AI | Spring AI 1.0 · OpenAI 兼容 API（DeepSeek / 硅基流动）· pgvector |
| 前端 | Vue 3 · TypeScript · Vite 6 · Element Plus · Pinia · PWA |
| 数据 | PostgreSQL 16（pgvector + pg_trgm） |
| 部署 | Docker Compose · Nginx 静态托管 · 可选 OCR 侧车 |

---

## 功能概览

| 模块 | 能力 |
| --- | --- |
| **基础业务** | 登录鉴权、药品建档、扫码入出库、Excel 导入、库存预警、FEFO 批次出库 |
| **临床** | 患者档案、门诊病历、处方开具与打印、就诊收费、预印处方模板 |
| **AI 辅助** | 语音录入、快捷语、DeepSeek 结构化整理（草稿确认）、OCR 拍照入库 |
| **Agent** | 自然语言查库存/流水；处方 → 出库 → 打印一条龙 |
| **RAG** | 脱敏病历向量化、相似病例 Top-3、设置页全量同步 |
| **导出** | 库存流水 Excel、病历保存 PDF |

完整版本定义见 [`docs/共用/ROADMAP.md`](docs/共用/ROADMAP.md)。

---

## 快速开始（Docker，推荐）

**前置**：Docker Desktop 或 Linux Docker，Git。

```bash
git clone <你的仓库地址>
cd 发凤村卫生室

cp .env.example .env
# 编辑 .env：至少修改 POSTGRES_PASSWORD；CLINIC_DATA_DIR 默认 ./clinic-data

docker compose up -d --build
```

1. 浏览器打开 `http://localhost:8088`（端口见 `.env` 的 `FRONTEND_PORT`）
2. 首次访问 `/setup` 设置管理员密码
3. （可选）加载演示数据：

```bash
chmod +x scripts/*.sh    # Linux / macOS
./scripts/seed-demo.sh   # Windows 可用 scripts/seed-demo.ps1
```

演示数据为虚构患者（张三、李四等），**请勿在生产环境执行 seed**。

---

## 本地开发

| 步骤 | 命令 |
| --- | --- |
| 后端测试 | `cd backend && mvn test`（需 JDK 21） |
| 前端构建 | `cd frontend && npm install && npm run build` |
| 联调 | 后端 `spring.profiles.active=dev` + 前端 `npm run dev` |

开发环境数据库配置见 `backend/src/main/resources/application-dev.yml`。  
AI 相关环境变量见 [`.env.example`](.env.example)（Spring Boot 不会自动读 `.env`，IDEA 需 EnvFile 插件或手动配置）。

---

## AI 能力开关

**默认最稳**：仅启动 `postgres + backend + frontend`，不填 API Key → 与 v1.0 同等可用。

| 能力 | 启用方式 |
| --- | --- |
| 结构化整理 / Agent | `.env` 填 `DEEPSEEK_API_KEY`，`CLINIC_AI_ENABLED=true` |
| 相似病例 RAG | 另填 `CLINIC_EMBEDDING_*`，`CLINIC_EMBEDDING_ENABLED=true` |
| OCR 入库 | `docker compose --profile ocr up -d` |
| 语音容器 | 不推荐；生产用手机输入法语音即可 |

详见 [`docs/共用/DEPLOYMENT.md` → AI 开关矩阵](docs/共用/DEPLOYMENT.md#八ai-能力独立开关)。

---

## 项目结构

```text
backend/          Spring Boot 后端（按 medicine / inventory / patient / clinic / ai / agent 分包）
frontend/         Vue 3 SPA
scripts/          备份、恢复、演示数据脚本
ocr-service/      可选 OCR 侧车（v1.4）
docker-compose.yml
docs/             架构设计、部署、路线图、测试清单
```

架构细节：[`docs/设计/v0.1-架构设计.md`](docs/设计/v0.1-架构设计.md) · AI 与脱敏：[`docs/给Agent/AI架构.md`](docs/给Agent/AI架构.md)

---

## 文档索引

| 文档 | 说明 |
| --- | --- |
| [DEPLOYMENT.md](docs/共用/DEPLOYMENT.md) | 生产部署（群晖 NAS）、备份恢复、版本升级 |
| [ROADMAP.md](docs/共用/ROADMAP.md) | 版本划分权威来源 |
| [测试清单.md](docs/共用/测试清单.md) | 本地 / NAS 验收项 |
| [使用者功能确认.md](docs/给人看/使用者功能确认.md) | 功能说明（面向使用者） |

---

## 版本分支（学习路径）

仓库保留从骨架到完整版的 Git 分支，可按版本 checkout 学习演进过程：

`v0.1-skeleton` → `v1.0-release`（纯业务 MVP）→ `v1.3-deepseek` → `v2.1-agent-workflow` → `v2.3-rag-search` → **`v3.0-release`**

---

## 免责声明

本项目为**个人 / 开源学习项目**，仅供技术交流与参考：

- **非医疗器械软件**，不得用于临床诊断决策或替代正规 HIS/EMR
- 仓库内演示数据均为虚构；**禁止**将真实患者信息提交到 Issue / PR
- 使用外部 AI API 时请自行遵守服务商条款与数据合规要求
- 生产部署前请修改默认密码、限制网络访问、定期备份

---

## License

[MIT](LICENSE) · Copyright (c) 2026
