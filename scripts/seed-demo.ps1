# 发凤村卫生室 — 导入演示数据（可选，首次部署后执行一次）
# 用法：.\scripts\seed-demo.ps1
# 说明：若已存在演示药品则跳过，不会重复导入。

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Read-DotEnv {
    param([string]$Path)
    $map = @{}
    if (-not (Test-Path $Path)) { return $map }
    Get-Content $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $map[$line.Substring(0, $idx).Trim()] = $line.Substring($idx + 1).Trim()
    }
    return $map
}

$root = Split-Path -Parent $PSScriptRoot
$sqlFile = Join-Path $PSScriptRoot "seed-demo-data.sql"
Push-Location $root

try {
    if (-not (Test-Path $sqlFile)) {
        throw "找不到 $sqlFile"
    }

    $envMap = Read-DotEnv (Join-Path $root ".env")
    $dbName = if ($envMap["POSTGRES_DB"]) { $envMap["POSTGRES_DB"] } else { "clinic" }
    $dbUser = if ($envMap["POSTGRES_USER"]) { $envMap["POSTGRES_USER"] } else { "clinic" }
    $containerName = "clinic-postgres"

    $running = docker ps --format "{{.Names}}" | Select-String -Pattern "^$([regex]::Escape($containerName))$"
    if (-not $running) {
        throw "PostgreSQL 容器未运行。请先: docker compose up -d"
    }

    Write-Host "导入演示数据 ..."
    Get-Content $sqlFile -Raw -Encoding UTF8 | docker exec -i $containerName psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1
    Write-Host "完成。演示数据包含示例药品、患者、库存批次（含临期/不足预警样例）。"
    Write-Host "请在浏览器完成 /setup 设置密码后登录体验。"
}
finally {
    Pop-Location
}
