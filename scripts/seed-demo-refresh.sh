#!/usr/bin/env bash
# 发凤村卫生室 — 刷新演示数据（仅开发/演示库；生产环境勿用）
# 用法：./scripts/seed-demo-refresh.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "警告：将删除 DEMO 标记的演示药品、患者、病历与库存批次。"
read -r -p "输入 YES 继续: " answer
if [[ "$answer" != "YES" ]]; then
  echo "已取消。"
  exit 0
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

DB_NAME="$(read_env POSTGRES_DB "clinic")"
DB_USER="$(read_env POSTGRES_USER "clinic")"
CONTAINER="clinic-postgres"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "PostgreSQL 容器未运行。" >&2
  exit 1
fi

echo "清除旧演示数据 ..."
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$ROOT/scripts/seed-demo-purge.sql"

echo "重新导入 ..."
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$ROOT/scripts/seed-demo-data.sql"

echo "演示数据刷新完成。"
