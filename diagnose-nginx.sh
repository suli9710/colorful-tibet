#!/bin/bash
# Nginx 配置诊断脚本

echo "=== 步骤 1: 检查容器内的实际 Nginx 配置 ==="
echo ""
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf

echo ""
echo "=== 步骤 2: 检查 Nginx 配置语法 ==="
docker exec colorful-tibet-frontend nginx -t

echo ""
echo "=== 步骤 3: 检查主机上的配置文件 ==="
echo ""
cat /opt/colorful-tibet/frontend/nginx.conf | head -30

echo ""
echo "=== 步骤 4: 检查后端服务是否可访问 ==="
echo ""
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1 | head -10

echo ""
echo "=== 步骤 5: 检查容器网络 ==="
echo ""
docker network inspect colorful-tibet_default 2>/dev/null | grep -A 5 backend || echo "网络检查失败"

echo ""
echo "=== 步骤 6: 测试 Nginx 代理 ==="
echo ""
curl -v http://localhost/api/spots 2>&1 | head -20



