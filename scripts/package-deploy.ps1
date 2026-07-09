# Clinic NAS/Docker deploy packager
# Usage:
#   .\scripts\package-deploy.ps1                  # pack current branch working tree
#   .\scripts\package-deploy.ps1 -Branch develop  # pack from git branch (recommended)
# Output: dist\clinic-deploy.zip

param(
    [string]$Branch = ''
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$DistDir = Join-Path $Root 'dist'
$Staging = Join-Path $DistDir '.pack-staging'
$ZipPath = Join-Path $DistDir 'clinic-deploy.zip'
$Timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'

$Required = @(
    'docker-compose.yml',
    'pom.xml',
    '.env.example',
    'backend/Dockerfile',
    'backend/pom.xml',
    'frontend/Dockerfile',
    'frontend/package.json'
)

$ExcludeTopDirs = @('docs', '.cursor', '.idea', '.git', '.vite', 'dist', 'node_modules')
$ExcludeSubDirs = @('target', '.gradle', 'build', 'node_modules', 'dist', '.vite')

function Test-RequiredFiles {
    param([string]$BasePath, [switch]$FromGit, [string]$GitRef)
    $Missing = @()
    foreach ($rel in $Required) {
        $relWin = $rel -replace '/', '\'
        if ($FromGit) {
            $null = git -C $BasePath cat-file -e "${GitRef}:${rel}" 2>$null
            if ($LASTEXITCODE -ne 0) { $Missing += $rel }
        }
        elseif (-not (Test-Path (Join-Path $BasePath $relWin))) {
            $Missing += $rel
        }
    }
    return $Missing
}

function Invoke-RoboCopy {
    param([string]$Source, [string]$Dest, [string[]]$ExcludeDirs = @())
    if (-not (Test-Path $Source)) { return }
    New-Item -ItemType Directory -Path $Dest -Force | Out-Null
    $xd = @()
    foreach ($d in $ExcludeDirs) { $xd += '/XD'; $xd += $d }
    & robocopy $Source $Dest /E /NFL /NDL /NJH /NJS /nc /ns /np @xd | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy failed: $Source -> $Dest (code $LASTEXITCODE)" }
}

function Remove-TreeIfExists {
    param([string]$Path)
    if (Test-Path $Path) { Remove-Item $Path -Recurse -Force }
}

function Import-FromGitBranch {
    param([string]$GitRef)
    $tarPath = Join-Path $DistDir '.pack-src.tar'
    Remove-TreeIfExists $tarPath
    $paths = @(
        'docker-compose.yml', 'pom.xml', '.env.example',
        'backend', 'frontend', 'scripts'
    )
    & git -C $Root archive --format=tar -o $tarPath $GitRef @paths
    if ($LASTEXITCODE -ne 0) { throw "git archive failed for branch '$GitRef'" }
    & tar -xf $tarPath -C $Staging
    if ($LASTEXITCODE -ne 0) { throw "tar extract failed" }
    Remove-TreeIfExists $tarPath

    foreach ($parent in @('backend', 'frontend')) {
        foreach ($dir in $ExcludeSubDirs) {
            Remove-TreeIfExists (Join-Path $Staging "$parent\$dir")
        }
    }
}

Write-Host '==> Preflight check...' -ForegroundColor Cyan
if ($Branch) {
    $Missing = Test-RequiredFiles -BasePath $Root -FromGit -GitRef $Branch
    if ($Missing.Count -gt 0) {
        Write-Host "ERROR: Branch '$Branch' missing deploy files:" -ForegroundColor Red
        $Missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
        exit 1
    }
    Write-Host "Source: git branch '$Branch'" -ForegroundColor DarkGray
}
else {
    $Missing = Test-RequiredFiles -BasePath $Root
    if ($Missing.Count -gt 0) {
        Write-Host 'ERROR: Missing required files in working tree:' -ForegroundColor Red
        $Missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
        Write-Host ''
        Write-Host 'Use develop branch, or run:' -ForegroundColor Yellow
        Write-Host '  .\scripts\package-deploy.ps1 -Branch develop' -ForegroundColor Yellow
        exit 1
    }
    Write-Host 'Source: current working tree' -ForegroundColor DarkGray
}

Write-Host '==> Clean staging...' -ForegroundColor Cyan
Remove-TreeIfExists $Staging
New-Item -ItemType Directory -Path $Staging -Force | Out-Null
New-Item -ItemType Directory -Path $DistDir -Force | Out-Null

Write-Host '==> Copy files...' -ForegroundColor Cyan
if ($Branch) {
    Import-FromGitBranch -GitRef $Branch
}
else {
    foreach ($file in @('docker-compose.yml', 'pom.xml', '.env.example')) {
        Copy-Item (Join-Path $Root $file) (Join-Path $Staging $file) -Force
    }
    Invoke-RoboCopy -Source (Join-Path $Root 'backend') -Dest (Join-Path $Staging 'backend') -ExcludeDirs $ExcludeSubDirs
    Invoke-RoboCopy -Source (Join-Path $Root 'frontend') -Dest (Join-Path $Staging 'frontend') -ExcludeDirs $ExcludeSubDirs
    if (Test-Path (Join-Path $Root 'scripts')) {
        $scriptStaging = Join-Path $Staging 'scripts'
        New-Item -ItemType Directory -Path $scriptStaging -Force | Out-Null
        Get-ChildItem (Join-Path $Root 'scripts') -File | Where-Object { $_.Name -ne 'package-deploy.ps1' } | ForEach-Object {
            Copy-Item $_.FullName (Join-Path $scriptStaging $_.Name) -Force
        }
    }
    foreach ($dir in $ExcludeTopDirs) {
        Remove-TreeIfExists (Join-Path $Staging $dir)
    }
}

$NasEnv = @'
# NAS deploy env - rename to .env in File Station
POSTGRES_DB=clinic
POSTGRES_USER=clinic
POSTGRES_PASSWORD=CHANGE_ME_STRONG_PASSWORD

CLINIC_DATA_DIR=/volume1/docker/clinic-data

POSTGRES_PORT=5433
BACKEND_PORT=8080
FRONTEND_PORT=8088

CLINIC_AI_ENABLED=false
CLINIC_AI_PROVIDER=noop
DEEPSEEK_API_KEY=
CLINIC_WHISPER_URL=
'@
[System.IO.File]::WriteAllText((Join-Path $Staging 'env.nas.txt'), $NasEnv, [System.Text.UTF8Encoding]::new($false))

$Readme = @'
Clinic NAS deploy package
=========================

1. Extract to /volume1/docker/clinic on Synology
2. Rename env.nas.txt to .env and set POSTGRES_PASSWORD
3. Ensure folders exist: /volume1/docker/clinic-data/postgres and uploads
4. Container Manager -> Project -> New -> path clinic -> Build & Start
5. Open http://NAS_LAN_IP:8088

PostgreSQL and pgvector are installed automatically by Docker. No manual DB setup.
'@
[System.IO.File]::WriteAllText((Join-Path $Staging 'NAS-DEPLOY.txt'), $Readme, [System.Text.UTF8Encoding]::new($false))

$fileCount = (Get-ChildItem $Staging -Recurse -File | Measure-Object).Count
if ($fileCount -lt 50) {
    throw "Staging too small ($fileCount files). Aborting."
}

Write-Host "==> Staged $fileCount files. Creating zip..." -ForegroundColor Cyan
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
Compress-Archive -Path (Join-Path $Staging '*') -DestinationPath $ZipPath -CompressionLevel Optimal

$ZipBackup = Join-Path $DistDir "clinic-deploy-$Timestamp.zip"
Copy-Item $ZipPath $ZipBackup -Force
Remove-TreeIfExists $Staging

$SizeMB = [math]::Round((Get-Item $ZipPath).Length / 1MB, 2)
Write-Host ''
Write-Host "OK: $ZipPath ($SizeMB MB, $fileCount files)" -ForegroundColor Green
Write-Host "Backup: $ZipBackup" -ForegroundColor DarkGray
Write-Host 'Upload to NAS /volume1/docker/clinic and extract.' -ForegroundColor Yellow
