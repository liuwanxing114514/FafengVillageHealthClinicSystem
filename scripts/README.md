# scripts 目录说明

| 脚本 | 环境 | 用途 |
| --- | --- | --- |
| `backup.sh` / `restore.sh` | **群晖 NAS / Linux** | 生产备份与恢复 |
| `seed-demo.sh` / `seed-demo-refresh.sh` | NAS / Git Bash | 演示数据导入与刷新 |
| `backup.ps1` / `restore.ps1` / `seed-demo.ps1` | **Windows 开发机** | 本地 Docker 联调（逻辑与 `.sh` 相同） |
| `seed-demo-data.sql` / `seed-demo-purge.sql` | 共用 | 演示 SQL |

生产运维以 **`.sh`** 为准，见 [`docs/共用/DEPLOYMENT.md`](../docs/共用/DEPLOYMENT.md)。  
本地测试见 [`docs/共用/测试清单.md`](../docs/共用/测试清单.md)。
