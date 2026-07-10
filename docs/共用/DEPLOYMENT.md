# 部署与升级指南

本文档面向**群晖 DS920+ NAS** 上的生产部署与运维（备份、首次安装、版本更新、回滚）。  
开发机（Windows + IDEA）仅用于编码与测试，见 [第十三节](#十三开发机非生产)。

版本定义以 [`ROADMAP.md`](ROADMAP.md) 为准。Cursor 开发配合 [`../给Agent/版本任务指南.md`](../给Agent/版本任务指南.md)。

**快速入口**：[首次部署（空库）](#二首次部署空库) · [版本更新（已有数据）](#四版本更新已有数据) · [v3.0 验收清单](#九分版本验收清单)

---

## 一、目录与硬件建议

### 1.1 目录结构（程序与数据分离）

```text
/volume1/docker/clinic/          # 程序（git clone / 上传，升级时可整体替换）
  docker-compose.yml
  .env
  scripts/
    backup.sh
    restore.sh
    seed-demo.sh

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
| 编排 | Container Manager「项目」导入 compose，或 SSH 执行 `docker compose` |
| 备份 | DSM **控制面板 → 任务计划**，每日执行 `scripts/backup.sh` |
| HTTPS | **控制面板 → 登录门户 → 反向代理**（手机扫码、PWA 须 HTTPS） |

### 1.3 推荐容器组合

| 组合 | 命令 | 说明 |
| --- | --- | --- |
| **最低（默认）** | `docker compose up -d --build` | postgres + backend + frontend；AI 可全关 |
| **+ OCR** | `docker compose --profile ocr up -d --build` | 拍照进货单入库 |
| **+ Chat / RAG** | 上述 + `.env` 填 API Key | 无额外容器；Agent、整理、相似病例 |
| **不推荐** | `--profile whisper` | 生产默认**不部署**；手机用**输入法语音**录入 |

**Whisper 默认不部署**：页面内「语音」按钮在未配置时会提示手动输入；手机在 HTTPS 病历页聚焦文本框后，用搜狗/微信/系统键盘的**语音键**即可，无需 NAS 上跑 Whisper 容器。

---

## 二、首次部署（空库）

**适用**：新 NAS、从未装过本系统、`/volume1/clinic-data/postgres` 为空或不存在。

1. DSM 安装 **Container Manager**；建议开启 SSH（便于 `docker compose --profile`）。
2. 创建目录：
   ```bash
   mkdir -p /volume1/docker/clinic
   mkdir -p /volume1/clinic-data/{postgres,uploads,backup}
   ```
3. 部署代码到 `/volume1/docker/clinic`（`git clone` 或上传 zip 解压）。
4. **新建** `.env`：
   ```bash
   cd /volume1/docker/clinic
   cp .env.example .env
   # 编辑：POSTGRES_PASSWORD、CLINIC_DATA_DIR=/volume1/clinic-data、FRONTEND_PORT 等
   ```
5. 首次启动（**不加 whisper profile**）：
   ```bash
   chmod +x scripts/*.sh
   docker compose up -d --build
   # 若需 OCR：docker compose --profile ocr up -d --build
   ```
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
1. ./scripts/backup.sh                         # 必须
2. cd /volume1/docker/clinic
3. 更新程序（勿动 clinic-data）：
   - git fetch && git checkout v3.0-release    # 或指定 tag
   - 或上传新版本覆盖 clinic/ 目录
4. diff .env.example .env → 仅追加缺失项，禁止整文件覆盖 .env
5. 核对 docker-compose.yml 变更（新 profile、端口）
6. docker compose up -d --build
   # 若需 OCR：docker compose --profile ocr up -d --build
7. docker compose logs backend 2>&1 | grep -i flyway    # 确认迁移 SUCCESS
8. 浏览器冒烟：登录、核心页无 500
9. 按第七节该版本增量验收 + v1.0 基线回归
10. 稳定后再升下一版
```

**Container Manager 用户**：GUI「重新部署」前同样先 `backup.sh`；若 GUI 不支持 `--profile ocr`，请用 SSH 执行 compose 命令。

**v3.0 更新说明**：无新 Flyway 迁移；主要是文档、NAS 脚本与验收清单。合并 `.env` 注释项后 rebuild，跑一遍 v3.0 全清单即可。

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
docker compose up -d --build

# 基础 + OCR
docker compose --profile ocr up -d --build

# 不推荐：页面内 Whisper 录音
docker compose --profile whisper up -d --build
```

**群晖 DS920+**：若无 `docker compose` 子命令，用 `sudo docker-compose up -d --build`（连字符）。首次 build 较慢属正常；Dockerfile 已配置 Maven（阿里云）、npm（npmmirror）、pip（清华）国内镜像以加速 NAS 构建。

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
| **v2.6** | 仅程序更新；流水 Excel、病历 PDF | — |
| **v3.0** | 文档与 NAS 脚本；按全清单验收 | — |

### .env 合并示例（版本更新时追加缺失项）

```env
CLINIC_OCR_URL=http://ocr-service:8000
DEEPSEEK_API_KEY=
CLINIC_AI_ENABLED=false
CLINIC_AI_PROVIDER=noop
DEEPSEEK_BASE_URL=https://api.siliconflow.cn
DEEPSEEK_MODEL=deepseek-ai/DeepSeek-V3
CLINIC_EMBEDDING_ENABLED=false
CLINIC_EMBEDDING_API_KEY=
CLINIC_EMBEDDING_BASE_URL=https://api.siliconflow.cn
CLINIC_EMBEDDING_MODEL=BAAI/bge-m3
CLINIC_EMBEDDING_DIMENSIONS=1024
# Spring AI 会自动追加 /v1/...，base-url 勿带 /v1
# Whisper 生产留空即可
CLINIC_WHISPER_URL=
```

---

## 八、AI 能力独立开关

| 能力 | 开关 | 依赖 | 关闭时行为 |
| --- | --- | --- | --- |
| Chat / 整理 / Agent | `CLINIC_AI_ENABLED` + `CLINIC_AI_PROVIDER` | 外网 API | noop，基础业务正常 |
| Whisper 页面录音 | `CLINIC_WHISPER_URL` 空 + 不启 profile | whisper 容器 | 用手机**输入法语音**；按钮点按提示手动输入 |
| OCR 入库 | `CLINIC_OCR_URL` 空 + 不启 profile | ocr 容器 + Chat | OCR 入口不可用 |
| Embedding / RAG | `CLINIC_EMBEDDING_ENABLED=false` | API + pgvector | 相似病例空状态；设置页禁用同步 |

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
