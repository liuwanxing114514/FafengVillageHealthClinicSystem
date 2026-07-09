# 部署与升级指南

本文档面向**诊所机器上的运维操作**（备份、升级、回滚）。  
每次让 Cursor 开发新版本时，请配合阅读 [`../给Agent/版本任务指南.md`](../给Agent/版本任务指南.md)。  
给人看的白话操作说明见 [`../给人看/后续版本操作说明.md`](../给人看/后续版本操作说明.md)。

版本定义以 [`ROADMAP.md`](ROADMAP.md)（本目录）为准。

---

## 一、目录结构

```text
D:\clinic\                    # 程序（可整体替换）
  docker-compose.yml
  .env                        # 敏感配置，升级时保留或合并
  backend\
  frontend\
  scripts\
    backup.ps1
    restore.ps1

D:\clinic-data\               # 数据（升级时保留）
  postgres\                   # 数据库文件
  uploads\
    images\                   # v1.4 OCR 图片
    audio\                    # v1.1 语音
    invoices\
  backup\                     # 自动备份输出
  logs\

D:\ai-models\                 # 可选，本地 AI 模型权重
  whisper\
  paddleocr\
  llm\
```

原则：**程序与数据分离**。升级只换 `D:\clinic\`，不动 `D:\clinic-data\`（除非恢复备份）。

---

## 二、首次部署（v1.0）

1. 安装 Docker Desktop + WSL2。
2. 创建 `D:\clinic`、`D:\clinic-data` 目录。
3. 复制项目代码到 `D:\clinic\`，复制 `.env.example` 为 `.env` 并填写密码等配置。
4. 首次启动：

```powershell
cd D:\clinic
docker compose up -d --build
```

5. 浏览器访问前端地址，完成首次设置密码。
6. （可选）导入演示数据：`.\scripts\seed-demo.ps1`
7. 配置 Windows 任务计划，每日执行 `scripts\backup.ps1`。
8. 验收：登录、药品、入库、出库、病历、处方打印、断网可用、关闭 AI 无报错。

---

## 三、备份

### 自动备份

- 脚本：`D:\clinic\scripts\backup.ps1`（项目根目录执行 `.\scripts\backup.ps1`）
- **触发方式**：脚本本身不会自启动；需在 **Windows 任务计划** 中配置每日执行（建议凌晨 3:00）
- 建议时间：每日凌晨 3:00
- 保留：最近 **7 天**（参数 `-KeepDays`，例如 `.\scripts\backup.ps1 -KeepDays 14`）
- **过期清理**：每次执行备份脚本时，自动删除 `backup\` 下超过 `KeepDays` 的 `clinic-backup-*.zip`
- 输出目录：`D:\clinic-data\backup\`（zip 包，内含 `postgres.sql`、uploads、`.env`）

### 备份内容

| 内容 | 说明 |
| --- | --- |
| PostgreSQL dump | `pg_dump` 导出 |
| `uploads/` | 上传的图片、音频等 |
| `.env` | 配置文件（含密码 hash 外的配置项） |

### 升级前必须手动备份

任何版本升级前，**除自动备份外，建议再手动执行一次**：

```powershell
cd D:\clinic
.\scripts\backup.ps1
```

记录备份文件名，便于回滚。

---

## 四、恢复

1. `docker compose stop backend frontend`（或 `docker compose down`）
2. 执行恢复脚本：

```powershell
cd D:\clinic
.\scripts\restore.ps1 -BackupZip "D:\clinic-data\backup\clinic-backup-YYYYMMDD-HHMMSS.zip"
```

3. 按提示输入 `YES` 确认
4. 脚本自动恢复 PostgreSQL、uploads，并 `docker compose up -d`
5. 若备份含 `.env`，会另存为 `.env.restored-*`，请手动对比合并
6. 检查库存、病历、上传文件是否正常

---

## 五、常规升级流程

适用于 v1.0 之后的**每一个**小版本（v1.1、v1.2、v1.3 …）。

```text
┌─────────────────────────────────────────────────────────┐
│ 1. 手动备份                                              │
│ 2. docker compose down                                   │
│ 3. 更新 D:\clinic\ 代码（git pull / 复制新版本）          │
│ 4. 对比 .env.example，合并新增配置到 .env                │
│ 5. 若 docker-compose.yml 有变化，按说明启用新服务        │
│ 6. docker compose up -d --build                          │
│ 7. 查看日志，确认 Flyway 迁移成功                        │
│ 8. 按 ROADMAP 该版本「交付」项做验收                     │
│ 9. 稳定运行一段时间后再升下一版                          │
└─────────────────────────────────────────────────────────┘
```

### 查看 Flyway 是否成功

```powershell
docker compose logs backend | Select-String -Pattern "Flyway"
```

或登录系统，确认无 500 错误、核心功能正常。

---

## 六、分版本升级差异

| 版本 | 除程序外还需做什么 | 不启用时是否影响基础业务 |
| --- | --- | --- |
| **v0.1** | 仅程序与 Flyway V0；本地 dev 需自建 PostgreSQL | — |
| **v0.2** | Flyway V1–V3（`sys_user`、`sys_setting`、`audit_log`）；首次访问走 `/setup` 设密 | — |
| **v1.0** | 无 | — |
| **v1.1** | 添加 `whisper-service` 容器；`.env` 配置 Whisper 地址 | 是，不影响 |
| **v1.2** | 仅程序更新 | — |
| **v1.3** | `.env` 填 `DEEPSEEK_API_KEY`，设 `clinic.ai.provider=deepseek` | 设 `noop` 或 `enabled=false` 时不影响 |
| **v1.4** | 添加 `ocr-service` 容器；`.env` 配置 OCR 地址 | 是，不影响 |
| **v1.5** | 仅程序更新 | — |
| **v1.6** | Flyway V11（病历收费字段、打印模板配置）；需纸质处方样本 | — |
| **v2.0** | 依赖 v1.3；实装 AI 助手页 | 关闭 AI 时不影响 |
| **v2.1** | 仅程序更新 | — |
| **v2.2** | Flyway V13 `visit_embedding`；`.env` 增 Embedding 项；硅基流动 `BAAI/bge-m3` | 关闭 Embedding 时不影响 |
| **v2.3** | 仅程序更新 | — |
| **v2.4** | 仅程序更新；设置页「病历向量化」可查看状态并触发同步；手机侧栏抽屉 + PWA「添加到主屏幕」 | 关闭 Embedding 时不影响 |

### .env 合并示例

升级后若 `.env.example` 新增了：

```env
CLINIC_WHISPER_URL=http://whisper-service:8080
CLINIC_OCR_URL=http://ocr-service:8080
DEEPSEEK_API_KEY=
CLINIC_AI_ENABLED=false
CLINIC_AI_PROVIDER=noop
CLINIC_EMBEDDING_ENABLED=false
CLINIC_EMBEDDING_API_KEY=
CLINIC_EMBEDDING_BASE_URL=https://api.siliconflow.cn/v1
CLINIC_EMBEDDING_MODEL=BAAI/bge-m3
CLINIC_EMBEDDING_DIMENSIONS=1024
```

将缺失项追加到现有 `.env`，**不要直接覆盖整个 .env**（会丢失已设置的密码和路径）。

### docker-compose 新增服务

v1.1 / v1.4 等版本可能在 `docker-compose.yml` 中用 `profiles` 或注释块标记可选服务。升级说明会写「取消注释 `whisper-service` 段」。不启用时可保持注释，系统照常运行。

---

## 七、回滚

升级后若出现严重问题：

1. `docker compose down`
2. 将 `D:\clinic\` 恢复为升级前的代码（git tag 或备份副本）
3. 若 Flyway 已执行新迁移：**必须用升级前的 pg_dump 恢复数据库**（仅回滚代码不够）
4. `docker compose up -d`
5. 验证 v1.0 核心功能

**注意**：Flyway 默认不自动降级。回滚数据库靠备份，不靠降级脚本。

---

## 八、分版本验收清单（诊所侧）

升级后在浏览器中快速检查：

### v0.2 登录与基础模块（开发期，v1.0 前）

- [ ] 全新库首次打开自动进入 `/setup`，可设置密码（至少 8 位，含字母与数字）
- [ ] `/login` 仅用密码登录（单用户，无用户名）
- [ ] 登录后首页显示当前用户与后端健康状态
- [ ] `/settings` 可修改密码、保存诊所名称、退出登录
- [ ] 未登录访问受保护页会跳转登录；未登录调 `/api/**` 返回 401

### v1.0 基线（每版升级后都应仍正常）

- [ ] 登录 / 改密码
- [ ] 药品列表、搜索
- [ ] 手动入库、出库
- [ ] 库存流水、临期/不足提醒
- [ ] 患者、病历、处方打印
- [ ] 断网时上述功能仍可用

### v1.1 语音

- [ ] 病历字段旁有语音按钮
- [ ] 录音 → 转写 → 填入字段（需人工保存）
- [ ] 关闭 Whisper 容器后，其余功能正常

### v1.2 快捷语

- [ ] 录入页出现候选语
- [ ] 管理页可增删改快捷语

### v1.3 DeepSeek

- [ ] 自由文本可整理为字段草稿
- [ ] 草稿在确认前不写入正式病历
- [ ] `enabled=false` 时系统无报错

### v1.4 OCR 入库

- [ ] 上传打印版单据 → 生成待审核表格
- [ ] 批准后才有入库流水
- [ ] 拒绝/不批准时不改库存

### v1.5 批量出库

- [x] 多药品一次出库，各有流水
- [x] 须填原因，有二次确认

### v2.0 Agent

- [ ] 自然语言查库存/临期/病历
- [ ] 助手页展示工具调用过程
- [ ] 出库类操作只生成待确认卡片

### v2.1 流程自动化

- [ ] 处方 → 待出库清单 → 确认 → 扣库存 → 打印
- [ ] 中途取消不会半扣库存

### v2.2 / v2.3 RAG

- [x] 病历页显示相似病例（脱敏）
- [x] 有「仅供参考，不替代诊断」提示
- [x] `CLINIC_EMBEDDING_ENABLED=false` 时病历录入正常，侧边栏友好空状态
- [x] 相似结果不含患者姓名、电话

### v2.4 RAG 运维与移动端

- [ ] **设置 → 病历向量化**：可见 Embedding 状态、`pendingCount`、最近同步时间
- [ ] 可触发全量/增量同步，toast 显示成功/跳过/失败条数与耗时
- [ ] `CLINIC_EMBEDDING_ENABLED=false` 或未配置时按钮禁用、有说明文案、页面不报错
- [ ] 手机宽度（≤768px）下顶栏汉堡菜单可打开/关闭侧栏抽屉
- [ ] 设置页、病历页（含相似病例）无异常横向滚动
- [ ] PWA 可「添加到主屏幕」；刷新后登录 session 仍有效（Cookie）

### v2.5 本地 LLM

- [ ] `provider=local` 时 Agent/整理可用（本地 Ollama 或 LM Studio）
- [ ] 外网断开、本地服务正常时基础+AI 可用

### v2.6 导出 / PDF

- [ ] 库存流水导出 Excel 与筛选一致
- [ ] 病历可保存 PDF

### v3.0 全功能基线

- [ ] DEPLOYMENT v3.0 清单全部勾选
- [ ] AI 全关时等同 v1.0 稳定

---

## 九、推荐升级节奏

| 时间点 | 建议 |
| --- | --- |
| v1.0 上线后 | 稳定使用 2–4 周，熟悉备份恢复 |
| 第一次升 v1.1 或 v1.2 | 不依赖外网，风险最低 |
| 再升 v1.3 | 先 `enabled=false` 部署，再开 API |
| v1.4 | 进货高峰前上线 OCR |
| v2.0+ | 前面 AI 用顺了再上 Agent |
| v2.2+ | 病历积累足够多再开 RAG |

---

## 十、开发机与诊所机

| 环境 | 用途 |
| --- | --- |
| 开发机 | Cursor 写代码、跑测试 |
| 诊所机 | 只跑 Docker 成品，存真实数据 |

不要把开发库覆盖生产库。发布流程：

1. 开发机完成版本 + 测试
2. 打 git tag（如 `v1.1.0`）
3. 诊所机：备份 → 拉取 tag → `docker compose up -d --build` → 验收

### 本地开发快速启动（v0.2 起）

配置见 `backend/src/main/resources/application-dev.yml`（Spring Profile 填 `dev`）。

**PostgreSQL（开发默认）**

| 项 | 值 |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `postgres` |
| User | `postgres` |
| Password | `admin` |

DataGrip / DBeaver 按上表新建数据源即可；表在 **数据库 `postgres` → schema `public`** 下（勿连到空的 `test1` 等其它库）。

**启动命令**

```powershell
# 后端：IDEA 运行 ClinicApplication，Active profiles = dev → http://localhost:8080
# 前端：
cd frontend
npm install   # 首次
npm run dev     # http://localhost:5173，/api 代理到 8080
```

**登录与密码（v0.2）**

- 系统为**单用户**：登录页只填密码，无用户名。
- **首次使用**（`sys_user` 表为空）：浏览器自动进入 `/setup` 设置密码。
- **已初始化**：访问 `/login` 输入已设密码；首页可进 `/settings` 改密、保存诊所名称、退出登录。
- **密码规则**：至少 8 位，须同时包含字母和数字。
- **忘记密码**（仅开发库）：在数据库执行 `DELETE FROM sys_user;` 后刷新页面，重新走 `/setup`（会清空管理员账号，需重新设密）。

**Docker 本地联调**（profile `docker`）数据源为 `jdbc:postgresql://postgres:5432/clinic`，用户 `clinic`，密码见 `.env` 中 `POSTGRES_PASSWORD`（默认 `change_me`）。首次部署同样在浏览器完成 `/setup`。

---

## 十二、首次启用 RAG（病历向量化）SOP

适用：从 v2.2 起已部署 pgvector，但尚未向量化历史病历，或升级至 v2.4 后首次开启 Embedding。

1. **备份**（见第三节）：`.\scripts\backup.ps1`
2. **配置 `.env`**：合并 `CLINIC_EMBEDDING_*`（见第六节示例）。推荐硅基流动 `BAAI/bge-m3`（1024 维）；本地模型见 v2.5 文档。
3. **重启 backend** 使配置生效：
   ```powershell
   cd D:\clinic
   docker compose up -d --build backend
   ```
   IDEA 开发环境：Run Configuration 重载 `.env` 后重启 Spring Boot（profile `dev`）。
4. **登录系统 → 设置 → 病历向量化**：确认服务状态为「已启用」；若显示未启用或配置不完整，按页面提示检查 `.env`。
5. **执行全量同步**：点击「全量同步」，确认二次提示后等待完成（病历多时可能数分钟）。完成后 `pendingCount` 应趋近 0，`syncedCount` 接近 `activeVisitCount`。
6. **验收**：
   - 新建/编辑病历，侧边栏应出现相似病例（需有一定历史病历量；结果已脱敏）。
   - 手机浏览器打开设置页，同样可查看状态并触发同步。
   - `CLINIC_EMBEDDING_ENABLED=false` 时：设置页友好禁用、病历录入与基础业务正常。
7. **日常运维**：新增或修改病历后，在设置页执行「增量同步」；或在 `.env` 配置 `CLINIC_EMBEDDING_SYNC_CRON` 定时增量（v2.2 已支持，cron 非空时自动启用）。

---

## 十三、手机浏览器与 PWA（v2.4）

1. 手机 Chrome / Safari 访问诊所前端地址（生产 HTTPS 或局域网 `http://<诊所机IP>`）并登录。
2. **添加到主屏幕**：
   - Android Chrome：菜单 →「安装应用」或「添加到主屏幕」
   - iOS Safari：分享 →「添加到主屏幕」
3. **典型手机场景**：入库/出库扫码（v0.7）、设置页向量同步、病历录入与相似病例参考。
4. **重要说明**：PWA 仅缓存静态资源（JS/CSS/图标），**不缓存业务 API**；断网时无法操作库存/病历（与 v1.0 断网基线一致，须能访问 backend）。
5. **开发验收**：Chrome DevTools 设备模拟（宽度 ≤768px）检查抽屉菜单；真机与开发机同一局域网访问 `npm run dev` 或 Docker 前端端口。

---

## 十四、相关文档

| 文档 | 用途 |
| --- | --- |
| [`ROADMAP.md`](ROADMAP.md) | 版本定义与交付物 |
| [`../给Agent/版本任务指南.md`](../给Agent/版本任务指南.md) | 每次开发时给 Cursor 的 Prompt |
| [`../给Agent/AI架构.md`](../给Agent/AI架构.md) | AI 模块设计 |
| [`../给Agent/开发交接.md`](../给Agent/开发交接.md) | 第一期开发总原则 |
| [`../给人看/后续版本操作说明.md`](../给人看/后续版本操作说明.md) | 后续版本白话操作指南 |
| [`../README.md`](../README.md) | 文档总目录 |
