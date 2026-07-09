#!/usr/bin/env bash
# 发凤村卫生室 — 从备份恢复（群晖 NAS / Linux）
# 用法：./scripts/restore.sh /volume1/clinic-data/backup/clinic-backup-YYYYMMDD-HHMMSS.tar.gz
# 警告：会覆盖当前数据库与 uploads

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "用法: $0 <backup.tar.gz>" >&2
  exit 1
fi

BACKUP_ARCHIVE="$1"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f "$BACKUP_ARCHIVE" ]]; then
  echo "备份文件不存在: $BACKUP_ARCHIVE" >&2
  exit 1
fi

read_env() {
  local key="$1" default="$2"
  if [[ -f .env ]]; then
    local val
    val="$(grep -E "^${key}=" .env | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
    if [[ -n "$val" ]]; then
      echo "$val"
      return
    fi
  fi
  echo "$default"
}

DATA_DIR="$(read_env CLINIC_DATA_DIR "./clinic-data")"
if [[ "$DATA_DIR" != /* ]]; then
  DATA_DIR="$ROOT/$DATA_DIR"
fi

DB_NAME="$(read_env POSTGRES_DB "clinic")"
DB_USER="$(read_env POSTGRES_USER "clinic")"
CONTAINER="clinic-postgres"
UPLOADS_DIR="$DATA_DIR/uploads"

echo "即将从以下备份恢复，当前数据库与 uploads 将被覆盖："
echo "  $BACKUP_ARCHIVE"
read -r -p "输入 YES 继续: " answer
if [[ "$answer" != "YES" ]]; then
  echo "已取消。"
  exit 0
fi

TEMP_DIR="$DATA_DIR/backup/restore-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$TEMP_DIR"
tar -xzf "$BACKUP_ARCHIVE" -C "$TEMP_DIR"

if [[ ! -f "$TEMP_DIR/postgres.sql" ]]; then
  echo "备份包中缺少 postgres.sql" >&2
  rm -rf "$TEMP_DIR"
  exit 1
fi

echo "停止应用容器 ..."
docker compose stop backend frontend 2>/dev/null || true

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "启动 PostgreSQL ..."
  docker compose up -d postgres
  sleep 5
fi

echo "恢复数据库 ..."
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$TEMP_DIR/postgres.sql"

if [[ -d "$TEMP_DIR/uploads" ]]; then
  echo "恢复 uploads ..."
  rm -rf "$UPLOADS_DIR"
  cp -a "$TEMP_DIR/uploads" "$UPLOADS_DIR"
fi

if [[ -f "$TEMP_DIR/.env" ]]; then
  RESTORED=".env.restored-$(date +%Y%m%d-%H%M%S)"
  cp "$TEMP_DIR/.env" "$ROOT/$RESTORED"
  echo "备份 .env 已保存为 $RESTORED，请手动对比合并。"
fi

rm -rf "$TEMP_DIR"

echo "启动全部服务 ..."
docker compose up -d

echo "恢复完成。请登录系统验证库存、病历与上传文件。"
