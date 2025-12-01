#!/bin/bash
# 检查后端服务状态脚本

echo "=== 1. 检查后端容器状态 ==="
docker compose ps backend

echo ""
echo "=== 2. 检查后端是否真的在运行 ==="
# 直接测试 API，不依赖健康检查
curl -s http://localhost:8080/api/spots | head -5

if [ $? -eq 0 ]; then
    echo "✅ 后端 API 可以访问！"
else
    echo "❌ 后端 API 无法访问"
fi

echo ""
echo "=== 3. 查看后端日志（最后 20 行）==="
docker compose logs --tail=20 backend

echo ""
echo "=== 4. 检查后端进程 ==="
docker exec colorful-tibet-backend ps aux | grep java || echo "Java 进程未找到"

echo ""
echo "=== 5. 检查端口监听 ==="
docker exec colorful-tibet-backend netstat -tlnp 2>/dev/null | grep 8080 || echo "端口 8080 未监听"

