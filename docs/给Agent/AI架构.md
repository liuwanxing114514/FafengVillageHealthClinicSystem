# AI 架构说明

本文档随 AI 相关版本迭代更新。当前为**占位骨架**，v0.9 / v1.3 开发时填充具体设计。

版本规划见 [`../共用/ROADMAP.md`](../共用/ROADMAP.md)。Cursor 开发 Prompt 见 [`版本任务指南.md`](版本任务指南.md)。

---

## 1. 设计原则

- 基础业务（库存、病历、处方）**不依赖** AI 可用。
- AI 输出统一进入 `ai_draft`，**人工确认**后才写入正式业务表。
- 外部 API 调用前**脱敏**患者隐私字段。
- Provider 可拔插：`noop` / `deepseek` / `local`。

---

## 2. AiProvider 与 Spring AI（v0.9 / v2.0.2）

`AiProvider` 接口保留，供业务层按 `noop` / `deepseek` / `local` 切换。v2.0.2 起 DeepSeek 实装走 **Spring AI** `ChatClient`（OpenAI 兼容 `base-url`），不再使用自研 HTTP 客户端。

| 能力 | 接口 / 实现 | 版本 |
| --- | --- | --- |
| 聊天补全 | `AiProvider.chatCompletion` → `AiChatClient` → `ChatClient` | v1.3 / v2.0.2 |
| 结构化 JSON 抽取 | 同上（提示词约束 JSON 输出） | v1.3 |
| Agent 工具调用 | `@Tool` + `ClinicAgentTools` + `ChatClient.tools()` | v2.0.2 |
| 文本向量化 | `VisitEmbeddingService` + `OpenAiEmbeddingModel` | v2.2 |

实现类：

| 类 | 版本 | 说明 |
| --- | --- | --- |
| `NoopAiProvider` | v0.9 | 默认，空实现 |
| `DeepSeekAiProvider` | v1.3 / v2.0.2 | 委托 `SpringAiChatClient` |
| `LocalAiProvider` | 远期 | Ollama / 本地模型 |
| `SpringAiChatClient` | v2.0.2 | ~~已删除~~ v2.5 起由 `ResilientAiChatClient` 替代 |
| `ResilientAiChatClient` | v2.5 | DB/env 多通道 Chat + failover |
| `ResilientEmbeddingModel` | v2.5 | DB/env 多通道 Embedding + failover |
| `ExternalServiceConfigService` | v2.5 | 四类外部服务开关（DB 优先 + env bootstrap） |
| `ChannelRegistry` | v2.5 | Chat/Embedding 通道链热刷新 |
| `VisitEmbeddingService` | v2.2 | 脱敏拼接 → Embedding → `visit_embedding` |
| `VisitSimilaritySearchService` | v2.3 | 脱敏查询 → embed → pgvector 余弦 Top-3 |

---

## 3. ai_draft 草稿模型（v0.9 定义）

| 字段 | 说明 |
| --- | --- |
| `draft_type` | `INBOUND` / `VISIT` / `OUTBOUND` / `QUERY` |
| `status` | `PENDING` / `APPROVED` / `REJECTED` |
| `payload` | JSONB，草稿内容 |

各类型审批后调用现有 Service，不重复写库存/病历逻辑。

---

## 4. 脱敏策略（T1 已确认，v1.3 实装）

> 原则：**部分屏蔽 + 星号**，在保护隐私的同时保留 AI 可用的上下文。  
> 调用 DeepSeek 等**外部 API** 前必须执行；v2.2 本地向量化前同样适用。  
> 完整规则见本文档第 4 节。

### 4.1 必须脱敏（部分屏蔽）

| 字段 | 规则 | 示例 |
| --- | --- | --- |
| **姓名** | 保留姓，名用 `*` 替代。单姓留 1 字，复姓（欧阳、司马等）留 2 字 | 张三 → `张*`；张三丰 → `张**`；欧阳娜娜 → `欧阳**` |
| **手机** | 保留前 3 位 + 后 4 位，中间用 `****` | 13812345678 → `138****5678` |
| **固定电话** | 保留区号，末 4 位，中间屏蔽 | 0571-12345678 → `0571-****5678` |
| **住址** | 保留到村/组/路/街；**只屏蔽门牌、栋、单元、室** | 发凤村3组12号 → `发凤村3组**号`；XX路88号301室 → `XX路**号***室` |
| **身份证号** | 保留前 6 + 后 4，中间 8 位用 `********` | 330102199001011234 → `330102********1234` |

### 4.2 可以原文发送

| 字段 | 说明 |
| --- | --- |
| 主诉、现病史、诊断、处理意见 | 正文发送，但其中夹带的姓名/电话/住址/身份证仍要先按 4.1 处理 |
| 药品名称、规格、数量、用法 | 整理处方、查库存需要 |
| 库存、批号、有效期、进货单价 | 入库 OCR 整理需要 |
| 生命体征数值 | 体温、血压等 |

### 4.3 OCR 进货单 / 发票 / 送货单（v1.4，T1 已确认）

单据 OCR 文本在送 DeepSeek 整理前，对下列内容**同样执行 4.1 规则**：

- 供应商联系人姓名
- 供应商电话、手机
- 含门牌的详细地址
- 单据上出现的身份证号

**可原文发送**：药品名、规格、数量、单价、金额、批号、有效期、单据编号、日期。

### 4.4 实现要求

- 后端统一 `Desensitizer` 工具类，发送 API 前对字符串与结构化字段逐字段处理。
- 脱敏仅作用于**出站请求**；数据库仍存原文，草稿确认页展示**原文**供医生核对。
- 单元测试覆盖：姓名、手机、住址门牌、身份证、混合正文各至少一例。

### 4.5 示例

**原文：**

> 患者张三，电话13812345678，住发凤村3组12号。主诉：咳嗽3天。

**发送 DeepSeek 前：**

> 患者张*，电话138****5678，住发凤村3组**号。主诉：咳嗽3天。

---

## 5. 外部服务

| 服务 | 版本 | 部署 |
| --- | --- | --- |
| DeepSeek API | v1.3 / v2.0.2 | Spring AI `ChatClient`（OpenAI 兼容），无独立容器 |
| Whisper | v1.1 | `whisper-service` 容器 |
| PaddleOCR | v1.4 | `ocr-service` 容器 |
| pgvector | v2.2 | PostgreSQL 扩展 + `visit_embedding` 表 |
| 硅基流动 Embedding | v2.2 | `BAAI/bge-m3`，OpenAI 兼容 `/v1/embeddings` |

---

## 6. Agent 工具（v2.0 实装，v2.0.2 Spring AI 化）

Agent 通过 `AgentOrchestrator` 编排：用户消息脱敏 → Spring AI `ChatClient` + `@Tool` 自动工具调用 → 结果摘要。

### 6.1 受控工具

| 工具 | 说明 | 写库 | 注册方式 |
| --- | --- | --- | --- |
| `searchMedicine` | 按名称/条码查药品 | 只读 | `ClinicAgentTools.@Tool` |
| `queryInventory` | 查库存数量、批次 | 只读 | 同上 |
| `queryExpiringMedicine` | 查临期药品（3 个月内） | 只读 | 同上 |
| `searchPatient` | 查患者（返回脱敏） | 只读 | 同上 |
| `searchPatientVisit` | 查历史病历 | 只读 | 同上 |
| `generateOutboundDraft` | 生成待确认出库清单 | 写 `ai_draft`（OUTBOUND） | 同上 |

业务逻辑仍由 `AgentToolRegistry` + 6 个 `AgentTool` 实现类承载；`ClinicAgentTools` 仅作 Spring AI 工具注册层。

### 6.2 脱敏 Advisor

`DesensitizationAdvisor`（`BaseAdvisor`）在 `ChatClient` 链中对用户消息执行 `Desensitizer` 规则，服务层调用前也会脱敏（双保险）。

禁止：直接 UPDATE 库存/病历、任意 SQL、未注册工具。

### 6.3 API

- `POST /api/agent/chat` — 自然语言查询
- `GET /api/agent/logs` — 执行日志（`agent_execution_log` 表）
- `POST /api/prescriptions/{id}/outbound-draft` — 处方生成 OUTBOUND 草稿（v2.1）
- `POST /api/ai/drafts/{id}/approve-outbound` — 医生确认批次后批量出库（v2.1）
- `GET /api/ai/embeddings/status` — 向量化状态（v2.2）
- `POST /api/ai/embeddings/sync-full` — 全量向量化同步（v2.2）
- `POST /api/ai/embeddings/sync-incremental` — 增量向量化同步（v2.2）
- `POST /api/ai/embeddings/search-similar` — 相似病例 Top-3（v2.3，脱敏摘要 + 相似度）

### 6.4 前端

- `AiAssistantView`：对话、工具调用时间线、待确认出库卡片
- `OutboundDraftView`：核对 OUTBOUND 草稿、FEFO 批次、确认出库后跳转处方打印（v2.1）
- `PrescriptionFormView`：保存处方后「生成待出库清单」→ 跳转出库草稿页（v2.1）
- `VisitFormView`：病历录入页侧边相似病例 Top-3（v2.3，仅供参考）
- `SettingsView`：病历向量化状态与全量/增量同步（v2.4）

### 6.5 相似病例检索（v2.3）

1. 前端根据当前主诉、现病史、诊断（及患者上下文）调用 `search-similar`。
2. 后端 `VisitEmbeddingTextBuilder.buildDesensitizedSearchQuery` 脱敏后本地 embed。
3. PostgreSQL `visit_embedding` 表按余弦距离（`<=>`）取 Top-3，返回已脱敏 `text_summary` 与相似度。
4. 未启用向量化、无查询文本或 API 失败时返回空列表，**不阻塞病历保存**。

---

## 7. 配置项（.env 与设置页）

### 7.1 配置优先级（v2.5）

1. **DB 无记录**：读 `.env` bootstrap（NAS 首次部署零改动）
2. **DB 有记录**：完全走 DB；设置页保存后 `refresh()` 热生效，不必重启容器
3. **加密主密钥** `CLINIC_SETTINGS_ENCRYPTION_KEY` **始终在 env**（设置页保存 API Key 时必需）

表结构：

| 表 | 用途 |
| --- | --- |
| `external_service` | Chat/Embedding/Whisper/OCR 总开关 + Whisper/OCR URL |
| `ai_chat_channel` | 对话模型多通道（priority、api_key_enc） |
| `ai_embedding_channel` | 向量模型多通道（含 dimensions 一致性校验） |

管理 API（需 Session 登录）：

- `GET/PUT /api/ai/services` — 外部服务总览与开关
- `/api/ai/channels/chat` / `/embedding` — CRUD、reorder、test、import-from-env

业务层仍只注入 `AiChatClient` / `EmbeddingModel` / `OcrClient` / `WhisperClient`。

### 7.2 Legacy env（bootstrap，DB 空时用）

**Chat / Agent**（可用 DeepSeek 官网或硅基流动 OpenAI 兼容地址）：

```env
CLINIC_AI_ENABLED=false
CLINIC_AI_PROVIDER=noop
DEEPSEEK_API_KEY=
# 硅基流动：DEEPSEEK_BASE_URL=https://api.siliconflow.cn  DEEPSEEK_MODEL=deepseek-ai/DeepSeek-V4-Pro
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_FALLBACK_API_KEY=
DEEPSEEK_FALLBACK_BASE_URL=https://api.deepseek.com
DEEPSEEK_FALLBACK_MODEL=deepseek-chat
```

**向量化**：

```env
CLINIC_EMBEDDING_ENABLED=false
CLINIC_EMBEDDING_API_KEY=
CLINIC_EMBEDDING_BASE_URL=https://api.siliconflow.cn/v1
CLINIC_EMBEDDING_MODEL=BAAI/bge-m3
CLINIC_EMBEDDING_DIMENSIONS=1024
```

**设置页加密主密钥**（在设置页保存 API Key 前配置）：

```env
CLINIC_SETTINGS_ENCRYPTION_KEY=
```

生成示例：`openssl rand -base64 32`

**IDEA dev**：Spring Boot 不自动读 `.env`；Run Configuration 需 EnvFile 或手动环境变量。数据库见 `application-dev.yml`。

**向量化**：`CLINIC_EMBEDDING_ENABLED=false` 时不影响基础业务；BGE 模型勿传 `dimensions` 参数给 API。

详见 [`.env.example`](../../.env.example) 与系统设置页「AI 外部服务」。

---

## 8. 变更记录

| 版本 | 变更 |
| --- | --- |
| — | 占位文档创建 |
| T1 | 脱敏规则定稿：部分屏蔽（姓/手机首尾/门牌/身份证） |
| T2–T6 | 出库 FEFO 推荐+手动确认；年龄推算；身份证；库存下限单位 |
| v0.9 | 待填：AiProvider 接口、ai_draft 表 |
| v1.3 | DeepSeek、Desensitizer、VISIT 草稿确认 |
| v1.4 | PaddleOCR 容器、OCR 入库 INBOUND 草稿、批准入库 |
| v2.0 | Agent 6 工具、AgentOrchestrator、agent_execution_log、AI 助手页 |
| v2.0.2 | Spring AI：ChatClient 替换 HttpDeepSeekClient；@Tool 替换 JSON 编排；DesensitizationAdvisor |
| v2.1 | 处方→出库→打印：`approveOutbound`、处方页生成 OUTBOUND 草稿、`OutboundDraftView` 确认出库 |
| v2.2 | `visit_embedding`、脱敏向量化、`VisitEmbeddingService`、硅基流动/本地 Embedding API |
| v2.3 | 相似病例检索：`VisitSimilaritySearchService`、病历页 `SimilarVisitPanel` |
| v2.5 | AI 外部服务 DB 管理：`external_service`、多通道 Chat/Embedding、`ResilientAiChatClient`、设置页配置与热刷新 |
