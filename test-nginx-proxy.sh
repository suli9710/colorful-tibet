#!/bin/bash
# 测试 Nginx 代理配置

echo "=== 1. 检查容器内的完整配置 ==="
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf

echo ""
echo "=== 2. 测试后端直接访问 ==="
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1 | head -10

echo ""
echo "=== 3. 测试通过 Nginx 代理 ==="
curl -v http://localhost/api/spots 2>&1 | head -30

echo ""
echo "=== 4. 查看 Nginx 错误日志 ==="
docker logs colorful-tibet-frontend 2>&1 | grep -i error | tail -10

