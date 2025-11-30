#!/bin/bash
# 快速修复 Nginx 配置脚本

echo "=== 步骤 1: 检查容器内的当前配置 ==="
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 5 "location.*api" || echo "未找到 /api 配置"

echo ""
echo "=== 步骤 2: 从主机复制最新配置到容器 ==="
cd /opt/colorful-tibet
docker cp frontend/nginx.conf colorful-tibet-frontend:/etc/nginx/conf.d/default.conf

echo ""
echo "=== 步骤 3: 验证配置语法 ==="
docker exec colorful-tibet-frontend nginx -t

echo ""
echo "=== 步骤 4: 重新加载 Nginx ==="
docker exec colorful-tibet-frontend nginx -s reload

echo ""
echo "=== 步骤 5: 验证配置已更新 ==="
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 5 "location.*api"

echo ""
echo "=== 步骤 6: 测试 API 代理 ==="
curl -s http://localhost/api/spots | head -10 || echo "测试失败"

echo ""
echo "=== 完成 ==="

