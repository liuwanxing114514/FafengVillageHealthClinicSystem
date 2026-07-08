# 发凤村卫生室 — 从备份恢复
# 用法：.\scripts\restore.ps1 -BackupZip "D:\clinic-data\backup\clinic-backup-20260708-030000.zip"
# 警告：会覆盖当前数据库与 uploads，恢复前请先备份。

param(
    [Parameter(Mandatory = $true)]
    [string]$BackupZip,
    [switch]$SkipConfirm
)

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
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        $map[$key] = $val
    }
    return $map
}

function Get-ConfigValue {
    param(
        [hashtable]$EnvMap,
        [string]$Key,
        [string]$Default
    )
    if ($EnvMap.ContainsKey($Key) -and $EnvMap[$Key]) { return $EnvMap[$Key] }
    return $Default
}

$root = Split-Path -Parent $PSScriptRoot
Push-Location $root

try {
    if (-not (Test-Path $BackupZip)) {
        throw "备份文件不存在: $BackupZip"
    }

    if (-not $SkipConfirm) {
        Write-Host "即将从以下备份恢复，当前数据库与 uploads 将被覆盖："
        Write-Host "  $BackupZip"
        $answer = Read-Host "输入 YES 继续"
        if ($answer -ne "YES") {
            Write-Host "已取消。"
            return
        }
    }

    $envMap = Read-DotEnv (Join-Path $root ".env")
    $dataDir = Get-ConfigValue $envMap "CLINIC_DATA_DIR" "./clinic-data"
    if (-not [System.IO.Path]::IsPathRooted($dataDir)) {
        $dataDir = Join-Path $root $dataDir
    }

    $dbName = Get-ConfigValue $envMap "POSTGRES_DB" "clinic"
    $dbUser = Get-ConfigValue $envMap "POSTGRES_USER" "clinic"
    $containerName = "clinic-postgres"
    $uploadsDir = Join-Path $dataDir "uploads"

    $tempDir = Join-Path $dataDir "backup" ("restore-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    Expand-Archive -Path $BackupZip -DestinationPath $tempDir -Force

    $sqlPath = Join-Path $tempDir "postgres.sql"
    if (-not (Test-Path $sqlPath)) {
        throw "备份包中缺少 postgres.sql"
    }

    Write-Host "停止应用容器 ..."
    docker compose stop backend frontend 2>$null

    $running = docker ps --format "{{.Names}}" | Select-String -Pattern "^$([regex]::Escape($containerName))$"
    if (-not $running) {
        Write-Host "启动 PostgreSQL ..."
        docker compose up -d postgres
        Start-Sleep -Seconds 5
    }

    Write-Host "恢复数据库 ..."
    Get-Content $sqlPath -Raw | docker exec -i $containerName psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1

    $backupUploads = Join-Path $tempDir "uploads"
    if (Test-Path $backupUploads) {
        Write-Host "恢复 uploads ..."
        if (Test-Path $uploadsDir) {
            Remove-Item $uploadsDir -Recurse -Force
        }
        Copy-Item $backupUploads $uploadsDir -Recurse -Force
    }

    $backupEnv = Join-Path $tempDir ".env"
    if (Test-Path $backupEnv) {
        Write-Host "备份 .env 已保存为 .env.restored-$((Get-Date -Format 'yyyyMMdd-HHmmss'))，请手动对比合并。"
        Copy-Item $backupEnv (Join-Path $root ".env.restored-$(Get-Date -Format 'yyyyMMdd-HHmmss')") -Force
    }

    Remove-Item $tempDir -Recurse -Force

    Write-Host "启动全部服务 ..."
    docker compose up -d

    Write-Host "恢复完成。请登录系统验证库存、病历与上传文件。"
}
finally {
    Pop-Location
}
