# 发凤村卫生室 — 数据库与文件备份
# 用法：在项目根目录执行 .\scripts\backup.ps1
# 建议：Windows 任务计划每日 03:00 执行

param(
    [int]$KeepDays = 7
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
        if ($val.StartsWith('"') -and $val.EndsWith('"')) {
            $val = $val.Substring(1, $val.Length - 2)
        }
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
    $envMap = Read-DotEnv (Join-Path $root ".env")
    $dataDir = Get-ConfigValue $envMap "CLINIC_DATA_DIR" "./clinic-data"
    if (-not [System.IO.Path]::IsPathRooted($dataDir)) {
        $dataDir = Join-Path $root $dataDir
    }

    $dbName = Get-ConfigValue $envMap "POSTGRES_DB" "clinic"
    $dbUser = Get-ConfigValue $envMap "POSTGRES_USER" "clinic"
    $containerName = "clinic-postgres"

    $backupRoot = Join-Path $dataDir "backup"
    $uploadsDir = Join-Path $dataDir "uploads"
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $workDir = Join-Path $backupRoot $stamp
    $archivePath = Join-Path $backupRoot "clinic-backup-$stamp.zip"

    New-Item -ItemType Directory -Force -Path $workDir | Out-Null

    Write-Host "备份目录: $backupRoot"
    Write-Host "时间戳: $stamp"

    $running = docker ps --format "{{.Names}}" | Select-String -Pattern "^$([regex]::Escape($containerName))$"
    if (-not $running) {
        throw "PostgreSQL 容器 '$containerName' 未运行。请先执行: docker compose up -d"
    }

    $sqlPath = Join-Path $workDir "postgres.sql"
    Write-Host "导出 PostgreSQL ..."
    docker exec $containerName pg_dump -U $dbUser -d $dbName --no-owner --no-acl `
        | Out-File -FilePath $sqlPath -Encoding utf8

    if ((Get-Item $sqlPath).Length -lt 100) {
        throw "pg_dump 输出异常，请检查数据库连接与权限。"
    }

    if (Test-Path $uploadsDir) {
        Write-Host "复制 uploads ..."
        Copy-Item -Path $uploadsDir -Destination (Join-Path $workDir "uploads") -Recurse -Force
    } else {
        Write-Host "uploads 目录不存在，跳过。"
    }

    $envFile = Join-Path $root ".env"
    if (Test-Path $envFile) {
        Copy-Item $envFile (Join-Path $workDir ".env") -Force
    }

    @"
发凤村卫生室备份
时间: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
数据库: $dbName
容器: $containerName
"@ | Out-File (Join-Path $workDir "README.txt") -Encoding utf8

    if (Test-Path $archivePath) { Remove-Item $archivePath -Force }
    Compress-Archive -Path (Join-Path $workDir "*") -DestinationPath $archivePath -Force
    Remove-Item $workDir -Recurse -Force

    Write-Host "备份完成: $archivePath"

    $cutoff = (Get-Date).AddDays(-$KeepDays)
    Get-ChildItem $backupRoot -Filter "clinic-backup-*.zip" | Where-Object {
        $_.LastWriteTime -lt $cutoff
    } | ForEach-Object {
        Write-Host "删除过期备份: $($_.Name)"
        Remove-Item $_.FullName -Force
    }

    Write-Host "保留最近 $KeepDays 天备份。"
}
finally {
    Pop-Location
}
