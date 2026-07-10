# 发凤村卫生室 — 从 Windows 远程触发 NAS 更新（A+B）
# 用法：.\scripts\deploy-remote.ps1
# 前提：SSH 配置 Host nas；GitHub Actions Release Images 已绿勾

param(
    [string]$SshHost = "nas",
    [string]$RemoteScript = "/volume1/docker/clinic/scripts/update.sh"
)

$ErrorActionPreference = "Stop"

Write-Host ">> 远程更新 NAS：$SshHost"
Write-Host ">> 执行：$RemoteScript"
Write-Host ">> 提示：请确认 GitHub Actions「Release Images」已成功，且今日 DSM 备份已完成"
Write-Host ""

ssh $SshHost "chmod +x '$RemoteScript' 2>/dev/null; '$RemoteScript'"

if ($LASTEXITCODE -ne 0) {
    throw "远程更新失败，退出码 $LASTEXITCODE"
}

Write-Host ""
Write-Host ">> 远程更新命令已执行，请在浏览器验收系统。"
