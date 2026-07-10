# 部署与升级指南

> **【NAS 生产部署 A+B — 后续 Agent 必读】**  
> - 升级：`git pull` + GHCR `pull` + `up -d`；**不用传 tar 包**  
> - 脚本：[`scripts/update.sh`](../../scripts/update.sh)；Windows：[`scripts/deploy-remote.ps1`](../../scripts/deploy-remote.ps1)  
> - **发布**：仅 push `release/vX.Y.Z-prod` 触发 GHCR 构建（`main`/`develop` 不触发）  
> - 镜像：`ghcr.io/liuwanxing114514/clinic-{backend,frontend}:sha-<commit>`（回退 `vX.Y.Z`）  
> - NAS `.env`：`GIT_BRANCH=release/vX.Y.Z-prod`（换版本时改此项）  
> - 升级前：**DSM 备份**；含 Flyway 禁止跳过；等 **Release Images** CI 绿勾  
> - 仅 **core 三容器**；不部署本地 `ocr-service`（OCR 走设置页 Vision）  
> - 回滚：`restore.sh` + 旧镜像 tag；**Flyway 不自动降级**

本文档面向**群晖 DS920+ NAS** 上的生产部署与运维（备份、首次安装、版本更新、回滚）。  
开发机（Windows + IDEA）仅用于编码与测试，见 [第十三节](#十三开发机非生产)。

版本定义以 [`ROADMAP.md`](ROADMAP.md) 为准。Cursor 开发配合 [`../给Agent/版本任务指南.md`](../给Agent/版本任务指南.md)。

**快速入口**：[首次部署（空库）](#二首次部署空库) · [版本更新（已有数据）](#四版本更新已有数据) · [v3.0 验收清单](#九分版本验收清单)

---

## 一、目录与硬件建议

### 1.1 目录结构（程序与数据分离）

```text
/volume1/docker/clinic/          # 程序（git clone；升级用 update.sh）
  docker-compose.yml
  .env
  scripts/
    update.sh                    # 日常升级（A+B）
    backup.sh
    restore.sh

/volume1/clinic-data/            # 数据（升级时保留，勿覆盖）
  postgres/
  uploads/
    images/                      # v1.4 OCR
    audio/                       # v1.1 语音（若启用 Whisper）
  backup/                        # backup.sh 输出的 tar.gz
```

原则：**升级只换 `/volume1/docker/clinic`，不动 `/volume1/clinic-data`**（除非执行 restore）。

### 1.2 群晖 DS920+ 建议

| 项 | 说明 |
| --- | --- |
| 系统 | DSM 7+，安装 **Container Manager** |
| CPU | Intel Celeron J4125（x86_64），可运行 postgres / backend / frontend |
| 内存 | 出厂 4GB 可跑 **core 三容器**；启用 OCR 或 Chat 建议 **8GB+** |
| 存储 | 程序 + 数据建议 ≥ 50GB 可用空间 |
| 编排 | SSH 执行 `./scripts/update.sh`（推荐）；或 Container Manager |
| 镜像 | GHCR 预构建（[`.github/workflows/release-images.yml`](../../.github/workflows/release-images.yml)）；仅 `release/v*-prod` push 触发；NAS **不 build** |
| 备份 | DSM **控制面板 → 任务计划**，每日执行 `scripts/backup.sh` |
| HTTPS | **控制面板 → 登录门户 → 反向代理**（手机扫码、PWA 须 HTTPS） |

### 1.3 推荐容器组合

| 组合 | 命令 | 说明 |
| --- | --- | --- |
| **最低（默认）** | `./scripts/update.sh` 或 `docker compose pull && up -d` | postgres + backend + frontend；镜像来自 GHCR |
| **本地开发** | `docker compose up -d --build` | Windows / IDEA 联调 |
| **+ Chat / RAG** | 上述 + `.env` 填 API Key | 无额外容器 |
| **不推荐** | `--profile ocr` / `--profile whisper` | 生产 OCR 用设置页 **Vision**；语音用手机输入法 |

**Whisper 默认不部署**：页面内「语音」按钮在未配置时会提示手动输入；手机在 HTTPS 病历页聚焦文本框后，用搜狗/微信/系统键盘的**语音键**即可，无需 NAS 上跑 Whisper 容器。

### 1.4 发布分支（GHCR 构建触发）

日常 `develop` / `main` push **不会**触发镜像构建。准备上 NAS 时：

```bash
git checkout -b release/v3.1.0-prod    # 从待发布代码拉出
git push -u origin release/v3.1.0-prod
```

- GitHub Actions **Release Images** 自动 build 并推 GHCR
- 镜像 tag：`sha-<commit>` + `v3.1.0`（从分支名解析）
- NAS `.env` 设 `GIT_BRANCH=release/v3.1.0-prod`
- 换版本：新建 `release/v3.2.0-prod` → push → 改 NAS `GIT_BRANCH` → `update.sh`

---

## 二、首次部署（空库）

**适用**：新 NAS、从未装过本系统、`/volume1/clinic-data/postgres` 为空或不存在。

1. DSM 安装 **Container Manager**；建议开启 SSH（便于 `docker compose --profile`）。
2. 创建目录：
   ```bash
   mkdir -p /volume1/docker/clinic
   mkdir -p /volume1/clinic-data/{postgres,uploads,backup}
   ```
3. 克隆代码到 `/volume1/docker/clinic`：
   ```bash
   cd /volume1/docker/clinic
   git clone https://github.com/liuwanxing114514/FafengVillageHealthClinicSystem.git .
   ```
4. **新建** `.env`：
   ```bash
   cd /volume1/docker/clinic
   cp .env.example .env
   # 编辑：POSTGRES_PASSWORD、CLINIC_DATA_DIR=/volume1/clinic-data、FRONTEND_PORT、GIT_BRANCH=release/vX.Y.Z-prod 等
   ```
5. 首次启动（拉 GHCR 镜像；需先 push `release/vX.Y.Z-prod` 且 **Release Images** 绿勾）：
   ```bash
   chmod +x scripts/*.sh
   ./scripts/update.sh
   ```
   若 GHCR 尚无镜像，可临时：`docker compose -p clinic up -d --build`（仅首次应急）。
6. 浏览器访问 `http://<NAS_IP>:8088`（或反代 HTTPS 域名）→ **`/setup` 设置管理员密码**（仅空库一次）。
7. （可选）导入演示数据：`./scripts/seed-demo.sh`（含脱敏样例病历，供 RAG 自测）。
8. DSM **任务计划** → 用户定义的脚本 → 每日 03:00：
   ```bash
   /volume1/docker/clinic/scripts/backup.sh
   ```
9. （推荐）配置 **HTTPS 反向代理**，供手机扫码与 PWA。
10. 按 [第九节 v3.0](#v30-全功能基线) 做首次验收。

**首次部署不要做的事**

- 不要用 `restore.sh`（尚无备份）。
- 不要用 `seed-demo-refresh.sh`（会删 DEMO 数据；生产库禁用）。
- 不要覆盖非空的 `clinic-data/postgres`（若已有数据，走 [第四节版本更新](#四版本更新已有数据)）。

---

## 三、备份与恢复

### 3.1 自动备份

- 脚本：`/volume1/docker/clinic/scripts/backup.sh`
- **触发**：DSM 任务计划每日执行（脚本不会自启动）
- 保留：最近 **7 天**（`./scripts/backup.sh --keep-days 14` 可改）
- 输出：`/volume1/clinic-data/backup/clinic-backup-YYYYMMDD-HHMMSS.tar.gz`
- 内容：`postgres.sql`、`uploads/`、`.env`

### 3.2 升级前必须手动备份

```bash
cd /volume1/docker/clinic
./scripts/backup.sh
```

记录 tar.gz 文件名，便于回滚。

### 3.3 恢复

```bash
cd /volume1/docker/clinic
./scripts/restore.sh /volume1/clinic-data/backup/clinic-backup-YYYYMMDD-HHMMSS.tar.gz
```

按提示输入 `YES`。若备份含 `.env`，会另存为 `.env.restored-*`，请手动对比合并。

---

## 四、版本更新（已有数据）

**适用**：系统已在运行，库内有真实业务数据。

```text
1. 从待发布代码创建并 push 发布分支：release/vX.Y.Z-prod
2. 确认 GitHub Actions「Release Images」对本次 push 已成功（绿勾）
3. NAS .env 设 GIT_BRANCH=release/vX.Y.Z-prod（换版本时改此项）
4. 确认今日 DSM 备份已完成（clinic-daily-backup）
5. SSH 到 NAS 执行：
     cd /volume1/docker/clinic
     ./scripts/update.sh
   或在 Windows 开发机：
     .\scripts\deploy-remote.ps1
6. update.sh 自动：git pull → 按 commit 拉 sha 镜像 → compose up → 健康检查 → Flyway 日志
7. diff .env.example .env → 仅追加缺失项（脚本会提示缺项）
8. 浏览器冒烟：登录、核心页无 500
9. 按该版本验收 + v1.0 基线回归
```

**Flyway 注意**：迁移在 backend 启动时自动执行；**升级前必须备份**；失败时用 `restore.sh` 恢复库，不能只换旧镜像。

**`package-release.ps1` tar 包**：仅离线应急，日常不用。

**更新时不要**：执行 `seed-demo.sh` / `seed-demo-refresh.sh`（会写入或删除演示数据）。

---

## 五、回滚

升级后出现严重问题时：

1. `docker compose down`（或 Container Manager 停止项目）
2. 将 `/volume1/docker/clinic` 恢复为升级前的 git tag / 代码副本
3. **若 Flyway 已执行新迁移**：必须用升级前的备份 `./scripts/restore.sh ...`（仅回滚代码不够）
4. `docker compose up -d --build`
5. 验证 v1.0 基线

Flyway **不会自动降级**；数据库回滚靠 pg_dump 备份。

---

## 六、Docker Compose Profiles

| Profile | 服务 | 内网 URL（`.env`） | 默认 |
| --- | --- | --- | --- |
| （无） | postgres, backend, frontend | — | **必启** |
| `ocr` | ocr-service | `CLINIC_OCR_URL=http://ocr-service:8000` | 按需 |
| `whisper` | whisper-service | `CLINIC_WHISPER_URL=http://whisper-service:9000` | **不部署** |

**Chat / Agent / Embedding** 无独立容器，通过 `.env` 开关与外网 API（硅基流动 / DeepSeek）。

示例：

```bash
# 仅基础业务
sudo docker-compose -p clinic up -d --build

# 基础 + OCR
sudo docker-compose -p clinic --profile ocr up -d --build

# 不推荐：页面内 Whisper 录音
sudo docker-compose -p clinic --profile whisper up -d --build
```

**群晖 DS920+**：使用 **`sudo docker-compose`**（带连字符），不是 `docker compose`。首次 build 较慢属正常。

**镜像加速（已写入 Dockerfile / compose，无需仅配 DSM 注册表）**：

| 层级 | 配置 |
| --- | --- |
| Docker 基础镜像 FROM | `.env` 的 `DOCKER_REGISTRY_PREFIX`（默认 `docker.m.daocloud.io`） |
| Maven | `backend/settings.xml` → 阿里云 |
| npm | `frontend/Dockerfile` → npmmirror |
| pip | `ocr-service/Dockerfile` → 清华 PyPI |
| apt（OCR 容器） | `ocr-service/Dockerfile` → 清华 Debian 源 |

海外或直连 Docker Hub 时，在 `.env` 设 `DOCKER_REGISTRY_PREFIX=docker.io`，并相应调整 `POSTGRES_IMAGE` / `WHISPER_IMAGE`（见 `.env.example` 注释）。

---

## 七、分版本升级差异

| 版本 | 除程序外还需做什么 | 不启用时是否影响基础业务 |
| --- | --- | --- |
| **v1.0** | 无 | — |
| **v1.1** | ~~Whisper 容器~~ **生产默认跳过**；用手机输入法语音 | 是，不影响 |
| **v1.2** | 仅程序更新 | — |
| **v1.3** | `.env` 填 `DEEPSEEK_API_KEY`，`CLINIC_AI_PROVIDER=deepseek` | `noop` / `enabled=false` 时不影响 |
| **v1.4** | `--profile ocr`；`CLINIC_OCR_URL=http://ocr-service:8000` | 是，不影响 |
| **v1.5** | 仅程序更新 | — |
| **v1.6** | Flyway V11（收费字段、打印模板） | — |
| **v2.0** | 依赖 v1.3 Chat | 关闭 AI 时不影响 |
| **v2.1** | 仅程序更新 | — |
| **v2.2** | Flyway V13；`.env` 增 Embedding | 关闭 Embedding 时不影响 |
| **v2.3** | 仅程序更新 | — |
| **v2.4** | 设置页向量化 UI；PWA / 手机抽屉 | 关闭 Embedding 时不影响 |
| **v2.5** | Flyway V14；`.env` 增 `CLINIC_SETTINGS_ENCRYPTION_KEY`；设置页 AI 外部服务/多通道 | DB 空时仍可用 legacy env |
| **v2.6** | 仅程序更新；流水 Excel、病历 PDF | — |
| **v3.0** | 文档与 NAS 脚本；按全清单验收 | — |

### .env 合并示例（版本更新时追加缺失项）

```env
CLINIC_OCR_URL=http://ocr-service:8000
DEEPSEEK_API_KEY=
CLINIC_AI_ENABLED=false
CLINIC_AI_PROVIDER=noop
DEEPSEEK_BASE_URL=https://api.siliconflow.cn
DEEPSEEK_MODEL=deepseek-ai/DeepSeek-V4-Pro
CLINIC_EMBEDDING_ENABLED=false
CLINIC_EMBEDDING_API_KEY=
CLINIC_EMBEDDING_BASE_URL=https://api.siliconflow.cn
CLINIC_EMBEDDING_MODEL=BAAI/bge-m3
CLINIC_EMBEDDING_DIMENSIONS=1024
CLINIC_SETTINGS_ENCRYPTION_KEY=
# 设置页保存 API Key 前必需；生成：openssl rand -base64 32
# Spring AI 会自动追加 /v1/...，base-url 勿带 /v1
# Whisper 生产留空即可
CLINIC_WHISPER_URL=
```

---

## 八、AI 能力独立开关

**v2.5 起**：可在系统设置页「AI 外部服务」统一管理开关与多通道 API；保存后立即生效。DB 无记录时仍读 `.env` bootstrap（NAS 首次部署零改动）。在设置页保存 API Key 前，须在 `.env` 配置 `CLINIC_SETTINGS_ENCRYPTION_KEY`（`openssl rand -base64 32`）。

| 能力 | 开关（env bootstrap） | 设置页（DB 有记录后） | 依赖 | 关闭时行为 |
| --- | --- | --- | --- | --- |
| Chat / 整理 / Agent | `CLINIC_AI_ENABLED` | AI 对话与助手 | 外网 API | 基础业务正常 |
| Embedding / RAG | `CLINIC_EMBEDDING_ENABLED` | 病历向量化 | API + pgvector | 相似病例空；同步禁用 |
| Whisper 页面录音 | `CLINIC_WHISPER_URL` | 语音转写 + URL | whisper 容器 | 用手机输入法语音 |
| OCR 入库 | `CLINIC_OCR_URL` + `clinic.ocr.mode` | 进货单识别 + 模式/URL | ocr 容器（local）或 Chat（vision） | OCR 入口不可用 |

**AI 全关**：上述全部关闭时，系统应与 v1.0 基线同等稳定（断网可用基础业务）。

---

## 九、分版本验收清单

### v1.0 基线（每版升级后都应仍正常）

- [ ] 登录 / 改密码
- [ ] 药品列表、搜索
- [ ] 手动入库、出库
- [ ] 库存流水、临期/不足提醒
- [ ] 患者、病历、处方打印
- [ ] 断网时上述功能仍可用（须能访问 NAS 上的 backend）

### v1.1 语音（默认：不部署 Whisper）

**生产默认（推荐）**

- [ ] `CLINIC_WHISPER_URL` 为空，未启 `--profile whisper`
- [ ] 病历页可手动录入并保存
- [ ] 手机 HTTPS：输入法语音填入主诉/诊断 → 保存成功
- [ ] `GET /api/ai/voice/status` → `available: false`；后端无 Whisper ERROR

**可选（仅当部署 Whisper 容器时）**

- [ ] 录音 → 转写 → 填入字段（需人工保存）
- [ ] 停止 whisper 容器后，其余功能正常

### v1.2 快捷语

- [ ] 录入页出现候选语
- [ ] 管理页可增删改快捷语

### v1.3 DeepSeek

- [ ] 自由文本可整理为字段草稿
- [ ] 草稿确认前不写入正式病历
- [ ] `enabled=false` 时系统无报错

### v1.4 OCR 入库

- [ ] 上传打印版单据 → 生成待审核表格
- [ ] 批准后才有入库流水

### v1.5 批量出库

- [ ] 多药品一次出库，各有流水；须填原因、二次确认

### v2.0 Agent

- [ ] 自然语言查库存/临期/病历
- [ ] 出库类操作只生成待确认卡片

### v2.1 流程自动化

- [ ] 处方 → 待出库清单 → 确认 → 扣库存 → 打印
- [ ] 中途取消不会半扣库存

### v2.2 / v2.3 RAG

- [ ] 病历页相似病例 Top-3（脱敏）；「仅供参考，不替代诊断」
- [ ] `CLINIC_EMBEDDING_ENABLED=false` 时侧边栏友好空状态

### v2.4 RAG 运维与移动端

- [ ] 设置 → 病历向量化：状态、全量/增量同步
- [ ] 手机 ≤768px 抽屉菜单；PWA 可添加到主屏幕

### v2.5 本地 LLM（已取消）

不再验收。

### v2.6 导出 / PDF

- [ ] 库存流水导出 Excel 与筛选一致
- [ ] 病历可保存 PDF

### v3.0 全功能基线

- [ ] 本文第九节清单全部勾选（v1.1 走「不部署 Whisper」分支）
- [ ] core 三容器 healthy；数据在 `/volume1/clinic-data`
- [ ] `backup.sh` 可产出 tar.gz；任务计划已配置
- [ ] HTTPS 反代后手机扫码入库可用
- [ ] AI 全关时等同 v1.0 稳定
- [ ] （若开 Embedding）演示 seed + 全量同步后相似检索可用

---

## 十、Smoke 验收步骤

详细分档（本地 🤖/💻/🐳 vs NAS 🗄️ vs 手机 📱）见 **[`测试清单.md`](测试清单.md)**。

**发布前最低要求（开发机）**

1. IDEA MCP：`backend` 目录 `mvn test` 全绿
2. `cd frontend && npm run build` 通过
3. IDEA dev + 浏览器：v1.0 基线 7 项（见测试清单 L-B1–B10）

**有 NAS 时**：再按 DEPLOYMENT 第二节（首次）或第四节（更新）+ 测试清单第二节 v3.0 汇总。

---

## 十一、演示数据与 RAG 自测

**仅首次部署或演示库使用**；**版本更新禁止 seed**。

1. `./scripts/seed-demo.sh`（10 条脱敏样例病历，多主诉簇）
2. 配置 `CLINIC_EMBEDDING_*` + API Key，重启 backend
3. 登录 → **设置 → 病历向量化 → 全量同步**
4. 新建病历，主诉输入「咽痛发热」等 → 侧边栏应出现 Top-3 相似病例（脱敏）
5. 刷新演示数据（**仅演示库**）：`./scripts/seed-demo-refresh.sh`

样例量仅供开发自测；生产 RAG 效果依赖真实病历积累。

---

## 十二、HTTPS、手机与输入法语音

1. DSM **控制面板 → 登录门户 → 反向代理**：例如 `https://clinic.example.com` → `http://localhost:8088`
2. 手机浏览器 HTTPS 登录；入库/出库**扫码须 HTTPS**
3. **语音录入（不部署 Whisper）**：病历页 → 点击主诉等文本框 → 输入法工具栏**麦克风** → 核对文字 → 保存
4. PWA：Chrome「添加到主屏幕」；仅缓存静态资源，不缓存 API
5. 开发机真机：`npm run dev`（HTTPS）→ `https://<电脑IP>:5173`

---

## 十三、开发机（非生产）

| 环境 | 用途 |
| --- | --- |
| Windows + IDEA | 编码、`mvn test`（profile=dev） |
| 群晖 DS920+ | 生产 Docker，存真实数据 |

**PostgreSQL（dev）**：见 `application-dev.yml`（localhost/postgres/admin）。

**Windows 可选脚本**：`scripts/*.ps1` 与 `.sh` 逻辑相同，仅供开发机本地 docker 联调，**生产文档以 `.sh` 为准**。

不要把开发库覆盖 NAS 生产库。发布：开发机测试通过 → git tag → NAS backup → 拉 tag → rebuild → 验收。

---

## 十四、首次启用 RAG SOP

1. `./scripts/backup.sh`
2. `.env` 合并 `CLINIC_EMBEDDING_*`（硅基流动 `BAAI/bge-m3`）
3. `docker compose up -d --build backend`
4. **设置 → 病历向量化 → 全量同步**
5. 新建/编辑病历验证相似病例；日常用「增量同步」

---

## 十五、相关文档

| 文档 | 用途 |
| --- | --- |
| [`ROADMAP.md`](ROADMAP.md) | 版本定义 |
| [`../给Agent/版本任务指南.md`](../给Agent/版本任务指南.md) | Cursor Prompt |
| [`../给Agent/AI架构.md`](../给Agent/AI架构.md) | AI 模块设计 |
| [`../给人看/后续版本操作说明.md`](../给人看/后续版本操作说明.md) | 白话操作 |
| [`../../README.md`](../../README.md) | 项目入口 |
