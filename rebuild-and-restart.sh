#!/bin/bash
# 重新构建和重启服务脚本

echo "=== 步骤 1: 停止服务 ==="
cd /opt/colorful-tibet
docker compose down

echo ""
echo "=== 步骤 2: 清理旧容器和镜像（可选） ==="
docker rm colorful-tibet-frontend colorful-tibet-backend 2>/dev/null || true
docker rmi colorful-tibet-frontend colorful-tibet-backend 2>/dev/null || true

echo ""
echo "=== 步骤 3: 重新构建所有服务 ==="
docker compose build --no-cache

echo ""
echo "=== 步骤 4: 启动服务 ==="
docker compose up -d

echo ""
echo "=== 步骤 5: 等待服务启动 ==="
sleep 10

echo ""
echo "=== 步骤 6: 检查服务状态 ==="
docker compose ps

echo ""
echo "=== 步骤 7: 查看日志（最后 20 行） ==="
echo "后端日志："
docker compose logs --tail=20 backend
echo ""
echo "前端日志："
docker compose logs --tail=20 frontend

echo ""
echo "=== 步骤 8: 测试后端 API ==="
curl -s http://localhost:8080/api/spots | head -5 || echo "后端未就绪"

echo ""
echo "=== 步骤 9: 测试前端代理 ==="
curl -s http://localhost/api/spots | head -5 || echo "前端代理未就绪"

echo ""
echo "=== 完成 ==="
echo "如果服务正常，可以访问："
echo "  前端: http://$(curl -s ifconfig.me || echo 'YOUR_SERVER_IP')"
echo "  后端: http://$(curl -s ifconfig.me || echo 'YOUR_SERVER_IP'):8080/api/spots"

