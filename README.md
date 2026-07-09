# 发凤村卫生室 — 诊所辅助系统

项目文档已整理到 [`docs/`](docs/README.md) 目录，按**读者**分为三类：

| 目录 | 给谁看 | 入口 |
| --- | --- | --- |
| [`docs/给人看/`](docs/给人看/README.md) | 诊所医生、管理员、非技术人员 | 功能说明、后续怎么升级 |
| [`docs/给Agent/`](docs/给Agent/README.md) | Cursor / AI 开发 | 开发规则、每版 Prompt |
| [`docs/共用/`](docs/共用/README.md) | 人 + Agent 都要看 | 路线图、大纲、部署 |

**快速跳转**

- 我是使用者，想了解系统做什么 → [`docs/给人看/使用者功能确认.md`](docs/给人看/使用者功能确认.md)
- 一期做完了，后面几期怎么操作 → [`docs/给人看/后续版本操作说明.md`](docs/给人看/后续版本操作说明.md)
- 我要用 Cursor 开始开发 → [`docs/给Agent/README.md`](docs/给Agent/README.md)
- 版本规划和交付物 → [`docs/共用/ROADMAP.md`](docs/共用/ROADMAP.md)

代码目录：`backend/`、`frontend/`、`scripts/`、`ocr-service/`、`docker-compose.yml`。

**v1.6 新增**：预印处方纸对齐打印、就诊收费（建议零售价/应收/实收/欠款）、病历独立菜单。更早版本含 v1.4 OCR 入库、v1.5 批量出库等，见 [`docs/共用/ROADMAP.md`](docs/共用/ROADMAP.md)。

## Agent / Cursor 开发提示

- 开新版本：从 [`docs/给Agent/README.md`](docs/给Agent/README.md) 和 [`版本任务指南.md`](docs/给Agent/版本任务指南.md) 进入。
- **后端跑测试**：对模型说「**用 IDEA MCP 跑 mvn test**」——不要指望 Cursor 终端里的 `mvn`（缺 `JAVA_HOME`）。
- **收尾一句话**：`可以了，自测（前端 build + IDEA MCP mvn test），通过后 push 合并 develop，更新功能点记录。`
