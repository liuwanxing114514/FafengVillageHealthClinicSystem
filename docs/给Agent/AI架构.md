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
| 文本向量化 | 待 v2.2 | v2.2 |

实现类：

| 类 | 版本 | 说明 |
| --- | --- | --- |
| `NoopAiProvider` | v0.9 | 默认，空实现 |
| `DeepSeekAiProvider` | v1.3 / v2.0.2 | 委托 `SpringAiChatClient` |
| `LocalAiProvider` | 远期 | Ollama / 本地模型 |
| `UnconfiguredAiChatClient` | v2.0.2 | AI 未启用时的占位 |

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
| pgvector | v2.2 | PostgreSQL 扩展 |

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

### 6.4 前端

- `AiAssistantView`：对话、工具调用时间线、待确认出库卡片
- `OutboundDraftView`：核对 OUTBOUND 草稿、FEFO 批次、确认出库后跳转处方打印（v2.1）
- `PrescriptionFormView`：保存处方后「生成待出库清单」→ 跳转出库草稿页（v2.1）

---

## 7. 配置项（.env）

```env
CLINIC_AI_ENABLED=false
CLINIC_AI_PROVIDER=noop
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
CLINIC_WHISPER_URL=
CLINIC_OCR_URL=
```

详见 `.env.example`（v1.0 起维护）。

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
| v2.2 | 待填：向量化 pipeline |
