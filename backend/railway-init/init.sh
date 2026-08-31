#!/bin/sh
# PerfFlow Railway 建表执行器
# 连接 Railway 内网 MySQL，执行建表脚本后退出
set -e

MYSQL_BIN="$(command -v mysql || true)"
if [ -z "$MYSQL_BIN" ]; then
  echo "ERROR: mysql client not found"
  exit 1
fi

echo "=== 开始执行建表脚本 ==="
# 库名由脚本内 CREATE DATABASE IF NOT EXISTS railway 保证；这里显式连到 railway
"$MYSQL_BIN" -h mysql.railway.internal -P 3306 -uroot \
  -p"LracuWMOykskTrNLUlGKbcecLnrQhgaV" \
  --default-character-set=utf8mb4 \
  < /app/perfflow.sql
echo "=== 建表脚本执行完成 ==="

# 验证：列出所有表
echo "=== 验证：railway 库中的表 ==="
"$MYSQL_BIN" -h mysql.railway.internal -P 3306 -uroot \
  -p"LracuWMOykskTrNLUlGKbcecLnrQhgaV" \
  -N -e "SHOW TABLES FROM railway;"
echo "=== 完成 ==="
