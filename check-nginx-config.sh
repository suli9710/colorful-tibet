#!/bin/bash
# 检查 Nginx 配置和后端服务

echo "=== 1. 检查容器内的 location 配置 ==="
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -B 2 -A 15 "location"

echo ""
echo "=== 2. 测试后端服务是否可访问 ==="
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1 | head -20

echo ""
echo "=== 3. 检查后端容器状态 ==="
docker compose ps backend

echo ""
echo "=== 4. 检查容器网络 ==="
docker exec colorful-tibet-frontend ping -c 2 backend 2>&1

echo ""
echo "=== 5. 查看 Nginx 错误日志 ==="
docker logs colorful-tibet-frontend 2>&1 | tail -10



