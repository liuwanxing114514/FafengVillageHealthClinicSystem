# 发凤村卫生室 — 诊所辅助系统

模块化单体诊所系统：药品库存、患者病历、处方打印，可选 AI（整理、OCR、Agent、RAG）。  
**生产部署目标：群晖 DS920+ NAS**（Container Manager + Docker Compose）。

| 文档 | 说明 |
| --- | --- |
| [首次部署](docs/共用/DEPLOYMENT.md#二首次部署空库) | 空库安装、/setup、可选演示数据 |
| [版本更新](docs/共用/DEPLOYMENT.md#四版本更新已有数据) | 已有数据升级、backup 必做 |
| [v3.0 验收清单](docs/共用/DEPLOYMENT.md#九分版本验收清单) | 全功能基线逐项勾选 |
| [功能与路线图](docs/共用/ROADMAP.md) | 版本权威定义 |
| [使用者说明](docs/给人看/使用者功能确认.md) | 给医生看 |

代码目录：`backend/`、`frontend/`、`scripts/`、`ocr-service/`、`docker-compose.yml`。

---

## 功能一览

| 阶段 | 版本 | 功能 |
| --- | --- | --- |
| 基础 MVP | v0.1–v1.0 | 登录、药品、库存入出库、扫码、Excel 导入、患者病历、处方打印、Docker 部署 |
| AI 辅助 | v1.1 | 语音录入（**生产默认不部署 Whisper**，用手机输入法语音） |
| | v1.2 | 快捷语候选 |
| | v1.3 | DeepSeek 结构化整理（草稿确认） |
| | v1.4 | OCR 拍照入库（可选 ocr 容器） |
| 业务扩展 | v1.5–v1.6 | 批量出库、预印处方、就诊收费、病历菜单 |
| Agent | v2.0–v2.1 | 自然语言查询、处方→出库→打印 |
| RAG | v2.2–v2.4 | 脱敏向量化、相似病例 Top-3、设置页同步、PWA/手机 |
| 导出 | v2.6 | 库存流水 Excel、病历保存 PDF |
| 基线 | **v3.0** | NAS 部署文档、验收清单、演示数据（无新业务） |
| 已取消 | ~~v2.5~~ | ~~本地 LLM~~（沿用硅基流动 API） |

---

## 最低配置（群晖 DS920+）

| 项 | 要求 |
| --- | --- |
| 设备 | Synology DS920+（或同架构 x86 NAS） |
| 系统 | DSM 7+，Container Manager |
| 内存 | 4GB 可跑 **core**；开 OCR 或 AI 建议 **8GB+** |
| 磁盘 | ≥ 50GB 可用（程序 + `/volume1/clinic-data`） |
| 网络 | 诊所局域网；手机扫码/PWA 需 **HTTPS 反代** |

---

## 推荐部署组合

| 组合 | 容器 / 配置 | 适用 |
| --- | --- | --- |
| **默认** | postgres + backend + frontend | 日常业务，AI 全关 |
| **+ OCR** | 上述 + `--profile ocr` | 拍照进货单 |
| **+ Chat / RAG** | 上述 + `.env` API Key | Agent、整理、相似病例 |
| **不推荐** | + `--profile whisper` | 手机输入法语音即可 |

**AI 可全部关闭**：`CLINIC_AI_ENABLED=false`、`CLINIC_EMBEDDING_ENABLED=false`、`CLINIC_WHISPER_URL` 与 `CLINIC_OCR_URL` 留空 → 与 v1.0 同等稳定。详见 [DEPLOYMENT AI 开关矩阵](docs/共用/DEPLOYMENT.md#八ai-能力独立开关)。

---

## 快速开始（NAS 首次）

```bash
# 1. 代码放到 /volume1/docker/clinic，数据目录 /volume1/clinic-data
cp .env.example .env   # 改 POSTGRES_PASSWORD、CLINIC_DATA_DIR

# 2. 启动（默认不启 Whisper / OCR）
chmod +x scripts/*.sh
docker compose up -d --build

# 3. 浏览器打开 http://<NAS_IP>:8088 → /setup
# 4. （可选）./scripts/seed-demo.sh
```

完整步骤见 [`docs/共用/DEPLOYMENT.md`](docs/共用/DEPLOYMENT.md)。

---

## 演示数据与 RAG 自测

```bash
./scripts/seed-demo.sh          # 首次可选；含 10 条样例病历
# 开启 CLINIC_EMBEDDING_* 后：设置 → 全量同步 → 新建病历测相似 Top-3
```

**版本更新时不要执行 seed**。

---

## Agent / Cursor 开发

- 入口：[`docs/给Agent/README.md`](docs/给Agent/README.md)
- 后端测试：对模型说「**用 IDEA MCP 跑 mvn test**」
- 进度：[`docs/给Agent/功能点与实现记录.md`](docs/给Agent/功能点与实现记录.md)
