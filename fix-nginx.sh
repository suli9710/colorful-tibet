#!/bin/bash
# Nginx 配置修复脚本

echo "=== 步骤 1: 检查当前配置 ==="
echo "检查容器内的 Nginx 配置..."
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 10 "location.*api"

echo ""
echo "=== 步骤 2: 测试后端直接访问 ==="
echo "测试 http://backend:8080/api/spots"
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1 | head -5

echo ""
echo "=== 步骤 3: 测试 Nginx 代理 ==="
echo "测试 http://localhost/api/spots"
docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots 2>&1 | head -5

echo ""
echo "=== 步骤 4: 检查 Nginx 错误日志 ==="
docker logs colorful-tibet-frontend 2>&1 | grep -i error | tail -10

echo ""
echo "=== 如果仍然 404，请执行以下命令重新构建 ==="
echo "cd /opt/colorful-tibet"
echo "docker compose down"
echo "docker rm colorful-tibet-frontend 2>/dev/null || true"
echo "docker rmi colorful-tibet-frontend 2>/dev/null || true"
echo "docker compose build --no-cache frontend"
echo "docker compose up -d"

