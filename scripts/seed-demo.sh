#!/usr/bin/env bash
# 发凤村卫生室 — 导入演示数据（可选，首次部署后执行一次）
# 用法：./scripts/seed-demo.sh
# 说明：若已存在演示药品则跳过，不会重复导入。

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL_FILE="$ROOT/scripts/seed-demo-data.sql"
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

DB_NAME="$(read_env POSTGRES_DB "clinic")"
DB_USER="$(read_env POSTGRES_USER "clinic")"
CONTAINER="clinic-postgres"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "找不到 $SQL_FILE" >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "PostgreSQL 容器未运行。请先: docker compose up -d" >&2
  exit 1
fi

echo "导入演示数据 ..."
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$SQL_FILE"
echo "完成。演示数据含示例药品、患者、脱敏样例病历（供 RAG 自测）。"
echo "请在浏览器完成 /setup 设置密码后登录体验。"
