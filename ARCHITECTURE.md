# 架构说明（一页）

> 发凤村卫生室诊所辅助系统 · v3.0 · 模块化单体  
> 详细版本规划见 [`docs/共用/ROADMAP.md`](docs/共用/ROADMAP.md)，AI 约定见 [`docs/给Agent/AI架构.md`](docs/给Agent/AI架构.md)

---

## 1. 系统总览

```text
┌─────────────┐     /api      ┌──────────────────────────────────────────┐
│  Vue 3 SPA  │ ────────────► │           Spring Boot（单进程）            │
│ Element Plus│   Session     │  system │ medicine │ inventory │ clinic  │
│   Pinia PWA │               │  patient │ importexcel │ ai │ agent       │
└─────────────┘               └──────────────────┬───────────────────────┘
                                                 │ JDBC / Flyway
                                                 ▼
                                    ┌────────────────────────┐
                                    │ PostgreSQL 16          │
                                    │ pgvector + pg_trgm     │
                                    └────────────────────────┘
          可选：OCR 侧车、外部 OpenAI 兼容 API（Chat / Embedding）
```

**核心原则**：基础业务（v1.0）不依赖 AI；AI 只写 `ai_draft`，审批后调现有 Service；库存只经 `InventoryService` 变更。

---

## 2. 库存模型

### 2.1 数据关系

```text
medicine ──< inventory_batch   (medicine_id + batch_no 唯一，quantity 存基本单位)
         ──< inventory_flow     (INBOUND / OUTBOUND / ADJUST，审计流水)
         ──< medicine_unit_conversion
```

| 表 | 职责 |
| --- | --- |
| `inventory_batch` | 批次余额、`expiry_date` 供 FEFO、`status` ACTIVE/DEPLETED |
| `inventory_flow` | 每次变更必有流水；`quantity_before/after` 为**药品级总库存**快照 |

**双写，非纯流水账**：变更时同时更新 `inventory_batch.quantity` 并插入 `inventory_flow`。禁止绕过 `InventoryService` 直接改数。

### 2.2 单位换算

`InventoryUnitConverter.toBaseQuantity()`：出库/入库请求单位 → 药品 `baseUnit`。查 `medicine_unit_conversion` 正反向系数，或按 `packageUnit` 推导；无法换算则拒绝。

### 2.3 FEFO 出库（推荐 → 确认 → 扣减）

```text
previewOutbound ──► allocateFefo()     SQL: expiry_date ASC NULLS LAST, id ASC
                 ──► 用户确认/可改批次
confirmOutbound ──► deductOutbound()   库存不足 → 业务异常，阻止出库
```

| 阶段 | 类 |
| --- | --- |
| FEFO 排序 | `InventoryBatchMapper.listAvailableForFefo()` |
| 贪心分配 | `InventoryService.allocateFefo()` |
| 批量出库 | `previewBatchOutbound` / `confirmBatchOutbound`（整单校验，缺货拒绝） |

---

## 3. RAG 相似病例流程

### 3.1 索引（VisitEmbeddingService）

```text
ACTIVE clinic_visit
    → VisitEmbeddingTextBuilder.buildDesensitizedSummary()   # 含 PatientContext
    → EmbeddingModel.embed()  (BGE-M3, 1024 维)
    → visit_embedding UPSERT (pgvector)
```

增量 sync：`cv.updated_at > ve.source_updated_at` 或无 embedding 的记录；VOID 病历 embedding 清理。

### 3.2 检索（VisitSimilaritySearchService）

```text
主诉 + 现病史 + 诊断（脱敏）
    → embed 查询向量
    → pgvector: 1 - (embedding <=> query)  AS similarity
    → Top-3，可 excludeVisitId
```

**降级**：Embedding 未启用 / API 失败 → 返回空列表，**不阻塞病历保存**。

### 3.3 脱敏边界

| 场景 | 实现 | 库内数据 |
| --- | --- | --- |
| 向量化 / 相似检索 | `ai.util.Desensitizer` + PatientContext | **原文** |
| LLM ChatClient | `DesensitizationAdvisor`（正则兜底） | **原文** |
| 前端 / 打印 / PDF | 无脱敏 | **原文** |

出站 AI API 前脱敏；`visit_embedding.text_summary` 存的是已脱敏文本。

---

## 4. 安全边界

### 4.1 认证

| 项 | 方案 |
| --- | --- |
| 用户模型 | 单用户 `admin`，`sys_user` 一条记录 |
| 密码 | BCrypt；`PasswordValidator` 校验强度 |
| 会话 | HttpSession + Cookie（`http-only`） |
| 首次部署 | `GET setup-status` → `POST setup-password`（仅空库） |

### 4.2 端点

| 公开 | 需登录 |
| --- | --- |
| `/api/health` | 其余全部 `/api/**` |
| `/api/system/setup-*` | inventory / patient / visit / ai / agent … |
| `/api/auth/login`, `/api/auth/session` | `/api/auth/logout`, `/api/auth/change-password` |

未认证访问 `/api/**` → **401 JSON**（非 302 跳转）。

### 4.3 环境与 CORS

| Profile | Swagger | 说明 |
| --- | --- | --- |
| `dev` | 开放 `/swagger-ui/**` | 本地联调 |
| `prod` / `docker` | `springdoc.enabled=false` | 生产不可访问 API 文档 |

CORS 仅 `/api/**`：允许 localhost 与 RFC1918 私网段，`allowCredentials=true`（诊所局域网 + PWA）。

### 4.4 AI 写入约束

```text
用户输入 → AI 生成 ai_draft → 人工确认 → 现有 Service 写业务表
                              ✗ 禁止 AI 直写 patient / inventory / visit
```

---

## 5. 关键代码入口

| 领域 | 入口 |
| --- | --- |
| 库存 | `inventory/service/InventoryService.java` |
| RAG 索引 | `ai/service/VisitEmbeddingService.java` |
| RAG 检索 | `ai/service/VisitSimilaritySearchService.java` |
| 脱敏 | `ai/util/Desensitizer.java`, `ai/advisor/DesensitizationAdvisor.java` |
| 安全 | `config/SecurityConfig.java`, `system/service/AuthService.java` |
| Agent | `agent/tool/ClinicAgentTools.java`（Tool Calling 调现有 Service） |

---

## 6. 已知取舍

- **无 HNSW 索引**：病历量小，全表 `<=>` + LIMIT 3 可接受；数据量大时需加 IVFFlat/HNSW。
- **逻辑外键为主**：应用层校验；库存/向量表保留少量 DB FK。
- **数量/金额**：`numeric`，禁止 float。
- **AI 可全关**：`CLINIC_AI_ENABLED=false` + `CLINIC_EMBEDDING_ENABLED=false` → 等同 v1.0 稳定路径。
