#!/usr/bin/env bash
# 发凤村卫生室 — 数据库与文件备份（群晖 NAS / Linux）
# 用法：在项目根目录 ./scripts/backup.sh
# 建议：DSM 控制面板 → 任务计划，每日 03:00 执行

set -euo pipefail

KEEP_DAYS=7
while [[ $# -gt 0 ]]; do
  case "$1" in
    --keep-days) KEEP_DAYS="$2"; shift 2 ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

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

BACKUP_ROOT="$DATA_DIR/backup"
UPLOADS_DIR="$DATA_DIR/uploads"
STAMP="$(date +%Y%m%d-%H%M%S)"
WORK_DIR="$BACKUP_ROOT/$STAMP"
ARCHIVE="$BACKUP_ROOT/clinic-backup-$STAMP.tar.gz"

mkdir -p "$WORK_DIR"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "PostgreSQL 容器未运行。请先: docker compose up -d" >&2
  exit 1
fi

echo "导出 PostgreSQL ..."
docker exec "$CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --no-acl > "$WORK_DIR/postgres.sql"

if [[ ! -s "$WORK_DIR/postgres.sql" ]]; then
  echo "pg_dump 输出异常" >&2
  exit 1
fi

if [[ -d "$UPLOADS_DIR" ]]; then
  echo "复制 uploads ..."
  cp -a "$UPLOADS_DIR" "$WORK_DIR/uploads"
else
  echo "uploads 目录不存在，跳过。"
fi

if [[ -f .env ]]; then
  cp .env "$WORK_DIR/.env"
fi

cat > "$WORK_DIR/README.txt" <<EOF
发凤村卫生室备份
时间: $(date '+%Y-%m-%d %H:%M:%S')
数据库: $DB_NAME
容器: $CONTAINER
EOF

tar -czf "$ARCHIVE" -C "$WORK_DIR" .
rm -rf "$WORK_DIR"

echo "备份完成: $ARCHIVE"

find "$BACKUP_ROOT" -maxdepth 1 -name 'clinic-backup-*.tar.gz' -mtime +"$KEEP_DAYS" -print -delete
echo "保留最近 ${KEEP_DAYS} 天备份。"
