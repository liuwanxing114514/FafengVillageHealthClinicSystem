# 发版打包（精简）：仅源码 + compose，NAS 上 docker compose build
# 用法：.\scripts\package-release.ps1
# 可选：-SkipTests  -ExcludeOcr（默认含 ocr-service 源码）

param(
    [switch]$SkipTests,
    [switch]$ExcludeOcr
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not $SkipTests) {
    Write-Host ">> mvn test..."
    Push-Location backend
    mvn -q test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Pop-Location
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$commit = (git rev-parse --short HEAD).Trim()
$staging = "dist\_staging"
$tarPath = "dist\clinic-deploy-${stamp}.tar.gz"

if (Test-Path $staging) { Remove-Item -Recurse -Force $staging }
New-Item -ItemType Directory -Force -Path $staging | Out-Null

@("docker-compose.yml", ".env.example", "pom.xml") | ForEach-Object {
    Copy-Item $_ $staging -Force
}

robocopy backend (Join-Path $staging "backend") /E /XD target .gradle build /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
robocopy frontend (Join-Path $staging "frontend") /E /XD node_modules dist .vite /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
robocopy scripts (Join-Path $staging "scripts") /E /XF package-release.ps1 /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null

if (-not $ExcludeOcr -and (Test-Path ocr-service)) {
    robocopy ocr-service (Join-Path $staging "ocr-service") /E /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
}

@"
# NAS 部署参考（复制为 .env 后编辑；已有 .env 时仅 diff 追加缺失项）
POSTGRES_DB=clinic
POSTGRES_USER=clinic
POSTGRES_PASSWORD=change_me
POSTGRES_PORT=5433
CLINIC_DATA_DIR=/volume1/docker/clinic-data
BACKEND_PORT=8080
FRONTEND_PORT=8088

# Docker Hub 镜像加速（build + postgres 拉取；默认 DaoCloud）
DOCKER_REGISTRY_PREFIX=docker.m.daocloud.io

CLINIC_SETTINGS_ENCRYPTION_KEY=

CLINIC_AI_ENABLED=true
CLINIC_AI_PROVIDER=deepseek
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.siliconflow.cn
DEEPSEEK_MODEL=deepseek-ai/DeepSeek-V4-Pro
DEEPSEEK_FALLBACK_API_KEY=
DEEPSEEK_FALLBACK_BASE_URL=https://api.deepseek.com
DEEPSEEK_FALLBACK_MODEL=deepseek-chat

CLINIC_EMBEDDING_ENABLED=true
CLINIC_EMBEDDING_API_KEY=
CLINIC_EMBEDDING_BASE_URL=https://api.siliconflow.cn
CLINIC_EMBEDDING_MODEL=BAAI/bge-m3
CLINIC_EMBEDDING_DIMENSIONS=1024

CLINIC_WHISPER_URL=
# 填下面 URL 后 deploy.sh 会自动 docker compose --profile ocr
CLINIC_OCR_URL=http://ocr-service:8000
"@ | Set-Content (Join-Path $staging "env.nas.txt") -Encoding UTF8

@"
NAS 解压说明（tar 包内文件在根目录，不会多一层文件夹）

【推荐：直接解压到 clinic 目录】
  mkdir -p /volume1/docker/clinic
  cd /volume1/docker/clinic
  tar -xzf clinic-deploy-${stamp}.tar.gz

【升级已有部署】
  1. ./scripts/backup.sh
  2. 保留旧 .env，diff env.nas.txt 追加缺失项
  3. tar -xzf clinic-deploy-${stamp}.tar.gz 覆盖程序文件
  4. sed -i 's/\r$//' scripts/*.sh
  5. sudo docker-compose -p clinic up -d --build
     # 不启 OCR 则不要 --profile ocr
     # .env 需含 DOCKER_REGISTRY_PREFIX（见 env.nas.txt）
     # 本包适用于 release/v*-nas-tar 分支（无 GHCR image 行）

Git: ${commit}  构建: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
含 ocr-service；基础镜像默认 docker.m.daocloud.io（Maven/npm/pip/apt 国内源已写入 Dockerfile）。
"@ | Set-Content (Join-Path $staging "EXTRACT.txt") -Encoding UTF8

@"
发凤村卫生室 NAS 部署包（源码，NAS 上 docker compose build）
Git ${commit} · Agent 分会话 / V4-Pro / UX 修复
解压见 EXTRACT.txt
"@ | Set-Content (Join-Path $staging "NAS-DEPLOY.txt") -Encoding UTF8

New-Item -ItemType Directory -Force -Path dist | Out-Null
if (Test-Path $tarPath) { Remove-Item $tarPath -Force }
Push-Location $staging
tar -czf "..\clinic-deploy-${stamp}.tar.gz" *
Pop-Location
Remove-Item -Recurse -Force $staging

# 清理旧的大包
Get-ChildItem dist -Filter "clinic-release-*.tar.gz" -ErrorAction SilentlyContinue | Remove-Item -Force

$item = Get-Item $tarPath
Write-Host ">> 完成: $($item.FullName) ($([math]::Round($item.Length/1MB, 2)) MB)"
