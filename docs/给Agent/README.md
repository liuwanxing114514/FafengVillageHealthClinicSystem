# 给 Agent 的文档

面向 **Cursor / AI 编程助手**。按版本开发时从这里进入。

---

## 本目录文件

| 文件 | 用途 |
| --- | --- |
| [角色-架构师.md](角色-架构师.md) | **v0.1 设计**：架构师身份 + 开工 Prompt（配合规则 `architect`） |
| [角色-开发者.md](角色-开发者.md) | **v0.2 起编码**：开发者身份 + 开工 Prompt（配合规则 `developer`） |
| [开发交接.md](开发交接.md) | 第一期总原则、包结构、不可破坏的业务规则、任务 0–11 |
| [版本任务指南.md](版本任务指南.md) | **每个版本**该 @ 什么、复制即用 Prompt、验收清单 |
| [功能点与实现记录.md](功能点与实现记录.md) | **各版本功能点**、实现状态与未实现规划（开对话 @ 引用） |
| [版本任务指南.md](版本任务指南.md) | 每版本 Prompt + **版本速查表**（分支、进度、开对话模板） |
| [前端布局约定.md](前端布局约定.md) | **v1.0.1 起必 read**：主布局、左侧菜单、多标签、新页 Checklist |
| [AI架构.md](AI架构.md) | AiProvider、ai_draft、T1 脱敏、Agent 边界 |

---

## 每次开新对话的标准流程

1. **新开** Cursor Agent 对话（不要在一个对话里跨很多版本）。  
2. **@ 引用**（从项目根路径）：
   ```
   @docs/共用/ROADMAP.md
   @docs/给Agent/开发交接.md
   @docs/给Agent/版本任务指南.md
   @docs/给Agent/功能点与实现记录.md
   @docs/给Agent/前端布局约定.md
   ```
3. 若已有代码，再 @ `backend/`、`frontend/`、`docker-compose.yml` 等。  
4. 打开 [版本任务指南.md](版本任务指南.md)，找到**当前版本**（如 v0.4、v1.3），复制 Prompt。  
5. 在 Prompt 里填 `【当前项目状态】`。  
6. 完成后要求 Agent 更新：`../共用/DEPLOYMENT.md`、`.env.example`、`AI架构.md`（若动 AI）。

---

## 第一期（还没有代码）

1. `git checkout v0.1-skeleton`
2. 新开 Agent 对话，勾选规则 **`architect`**
3. 阅读 [角色-架构师.md](角色-架构师.md)，复制 **「v0.1 开工 Prompt」**

额外 @：

```
@docs/共用/大纲.md
@docs/给人看/使用者功能确认.md
@docs/给Agent/角色-架构师.md
```

设计文档确认后，再新开对话做 **任务 1 项目骨架**（不勾选 architect）。

---

## 第二期起编码（v0.2 示例）

1. `git checkout v0.2-system`（或对应版本分支）
2. 新开 Agent 对话，勾选规则 **`developer`**
3. 阅读 [角色-开发者.md](角色-开发者.md)，复制 **「v0.2 开工 Prompt」**
4. 或从 Cursor Automations 运行 **「发凤村卫生室 v0.2 开发者」**

---

## 第三期及以后（当前基线：v2.3 已合并 develop）

**下一版本：v2.4 RAG 运维与 Embedding 管理** → 分支 `v2.4-rag-ops`，详见 [版本任务指南.md](版本任务指南.md) **版本速查表** 与 [功能点与实现记录.md](功能点与实现记录.md)「当前工作点」。

1. 人先看 [`../给人看/后续版本操作说明.md`](../给人看/后续版本操作说明.md) 决定是否升级。  
2. Agent 只开发**一个版本**（当前做 v2.0，不要顺带 v2.1）。  
3. 开发完成 → 人在诊所机按 [`../共用/DEPLOYMENT.md`](../共用/DEPLOYMENT.md) 升级 → 按指南验收。

---

## MCP 工具（Cursor 已配置时优先使用）

| MCP | 用途 |
| --- | --- |
| **user-jetbrains** | IDEA 运行/编译/测试、查项目错误、重构 |
| **user-datagrip** | 数据库连接、执行 SQL、验证 Flyway 迁移、预览表数据 |

### 后端 `mvn test` 必须用 IDEA MCP

本机 **Cursor 自带终端** 往往没有 `JAVA_HOME`，直接 `mvn test` 会失败。应使用 **user-jetbrains** 的 `execute_terminal_command`，在 IDEA 环境里跑测试。

Agent 标准做法：

1. 读 `.idea/misc.xml` 确认 `project-jdk-name`（当前为 `corretto-21` → `C:\Users\lwx\.jdks\corretto-21.0.11`）
2. 调用 `execute_terminal_command`，`executeInShell: true`，示例：

```powershell
$env:JAVA_HOME='C:\Users\lwx\.jdks\corretto-21.0.11'; Set-Location 'D:\xiangmu\发凤村卫生室\backend'; mvn test
```

3. 失败则修代码/测试后重跑，直到全部通过

**您要对模型说的话术**见 [版本任务指南.md § 自测与收尾](版本任务指南.md#自测与收尾您可以这样对模型说)。

详见项目根 [`README.md`](../../README.md)「Agent / Cursor 开发提示」。

---

## 必须遵守的硬规则（摘要）

完整版见 [开发交接.md](开发交接.md) 第三节。

- 库存只通过入库/出库/盘点流水变更  
- 出库绑患者和处方；库存不足阻止；FEFO **推荐**批次，**用户确认**后才扣  
- AI 只写 `ai_draft`，人确认后才写正式表  
- Flyway 管数据库；不用 float 存金额数量  
- 版本号以 [`../共用/ROADMAP.md`](../共用/ROADMAP.md) 为准  

---

## 共用文档（开发时经常要 @）

| 文件 | 路径 |
| --- | --- |
| 路线图 | `docs/共用/ROADMAP.md` |
| 业务大纲 | `docs/共用/大纲.md` |
| 部署升级 | `docs/共用/DEPLOYMENT.md` |
| 使用者需求 | `docs/给人看/使用者功能确认.md` |
