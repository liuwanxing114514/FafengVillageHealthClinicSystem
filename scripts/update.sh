#!/usr/bin/env bash
# 发凤村卫生室 — NAS 生产更新（A+B：git pull + GHCR pull）
# 用法：/volume1/docker/clinic/scripts/update.sh [--skip-backup-check]
# Windows：.\scripts\deploy-remote.ps1

set -euo pipefail

SKIP_BACKUP_CHECK=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-backup-check) SKIP_BACKUP_CHECK=1; shift ;;
    -h|--help)
      echo "用法: $0 [--skip-backup-check]"
      exit 0
      ;;
    *) echo "未知参数: $1" >&2; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

read_env() {
  local key="$1" default="$2"
  if [[ -f .env ]]; then
    local val
    val="$(grep -E "^${key}=" .env | tail -1 | cut -d= -f2- | tr -d '\r' | sed -e 's/^"//' -e 's/"$//' || true)"
    if [[ -n "$val" ]]; then
      echo "$val"
      return
    fi
  fi
  echo "$default"
}

resolve_docker() {
  if command -v docker >/dev/null 2>&1; then
    command -v docker
    return
  fi
  if [[ -x /usr/local/bin/docker ]]; then
    echo /usr/local/bin/docker
    return
  fi
  echo "docker" >&2
  return 1
}

resolve_compose() {
  if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    echo "docker-compose"
    return
  fi
  echo "docker compose" >&2
  return 1
}

GIT_BRANCH="$(read_env GIT_BRANCH "main")"
COMPOSE_PROJECT="$(read_env COMPOSE_PROJECT_NAME "clinic")"
BACKEND_PORT="$(read_env BACKEND_PORT "8080")"
DB_USER="$(read_env POSTGRES_USER "clinic")"
DB_NAME="$(read_env POSTGRES_DB "clinic")"

DOCKER="$(resolve_docker)"
COMPOSE="$(resolve_compose)"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

warn_env_diff() {
  if [[ ! -f .env.example ]] || [[ ! -f .env ]]; then
    return
  fi
  local missing=()
  while IFS= read -r key; do
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    if ! grep -qE "^${key}=" .env; then
      missing+=("$key")
    fi
  done < <(grep -E '^[A-Z][A-Z0-9_]*=' .env.example | cut -d= -f1)
  if [[ ${#missing[@]} -gt 0 ]]; then
    log "警告：.env 缺少以下项（请对比 .env.example 手动追加）："
    printf '  - %s\n' "${missing[@]}"
  fi
}

wait_health() {
  local url="http://127.0.0.1:${BACKEND_PORT}/api/health"
  local i
  for i in $(seq 1 30); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "健康检查通过: $url"
      return 0
    fi
    sleep 2
  done
  log "健康检查超时: $url"
  return 1
}

show_flyway_version() {
  if "$DOCKER" ps --format '{{.Names}}' | grep -qx "clinic-postgres"; then
    local ver
    ver="$("$DOCKER" exec clinic-postgres psql -U "$DB_USER" -d "$DB_NAME" -tAc \
      "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1" \
      2>/dev/null | tr -d '[:space:]' || true)"
    if [[ -n "$ver" ]]; then
      log "当前 Flyway 版本: V${ver}"
    fi
  fi
}

if [[ "$SKIP_BACKUP_CHECK" -eq 0 ]]; then
  log "请确认今日 DSM 备份（clinic-daily-backup）已完成；紧急跳过：$0 --skip-backup-check"
fi

if ! "$DOCKER" ps --format '{{.Names}}' | grep -qx "clinic-postgres"; then
  log "错误：clinic-postgres 未运行"
  exit 1
fi

log "git fetch + pull origin ${GIT_BRANCH}"
git fetch origin
git checkout "$GIT_BRANCH"
git pull origin "$GIT_BRANCH"

COMMIT_SHA="$(git rev-parse --short HEAD)"
export CLINIC_IMAGE_TAG="sha-${COMMIT_SHA}"
export BACKEND_IMAGE="ghcr.io/liuwanxing114514/clinic-backend:${CLINIC_IMAGE_TAG}"
export FRONTEND_IMAGE="ghcr.io/liuwanxing114514/clinic-frontend:${CLINIC_IMAGE_TAG}"

log "目标镜像 tag: ${CLINIC_IMAGE_TAG} (commit ${COMMIT_SHA})"
warn_env_diff

log "拉取镜像..."
set +e
$COMPOSE -p "$COMPOSE_PROJECT" pull backend frontend
PULL_RC=$?
set -e
if [[ $PULL_RC -ne 0 ]]; then
  log "sha 标签拉取失败，回退到 main"
  export CLINIC_IMAGE_TAG="main"
  export BACKEND_IMAGE="ghcr.io/liuwanxing114514/clinic-backend:main"
  export FRONTEND_IMAGE="ghcr.io/liuwanxing114514/clinic-frontend:main"
  $COMPOSE -p "$COMPOSE_PROJECT" pull backend frontend
fi

log "启动服务..."
$COMPOSE -p "$COMPOSE_PROJECT" up -d

log "等待 backend 就绪..."
wait_health || true

log "Flyway / 启动日志（最近 40 行）："
$COMPOSE -p "$COMPOSE_PROJECT" logs backend --tail 40 2>&1 | grep -iE 'flyway|error|started|success' || true

show_flyway_version
log "更新完成。请浏览器冒烟：登录、库存、病历。"
