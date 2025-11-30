# Nginx 代理 404 问题排查

## ❌ 错误：前端访问后端 API 返回 404

### 问题描述

访问网站时，前端无法连接到后端：
- "错误: 无法连接到后端服务!"
- "错误: /api"
- "错误: 404"
- "Not Found"

### 原因

- 后端服务未完全启动
- nginx 无法解析 `backend` 服务名
- nginx 代理配置问题
- 后端 API 路径不匹配

### 解决方案

#### 步骤 1：检查后端是否完全启动

```bash
# 查看后端日志，确认已启动
docker compose logs backend | grep "Started TibetTourismApplication"

# 如果看到这行，说明后端已启动
# 如果没有，等待后端启动（可能需要 30-60 秒）
```

#### 步骤 2：测试后端 API（直接访问）

```bash
# 在服务器上直接测试后端
curl http://localhost:8080/api/spots

# 如果返回 JSON 数据，说明后端正常
# 如果连接失败，说明后端未启动
```

#### 步骤 3：测试前端容器到后端连接

```bash
# 测试前端容器能否访问后端
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots

# 如果成功，说明 Docker 网络正常
# 如果失败，检查 Docker 网络配置
```

#### 步骤 4：检查 nginx 配置

```bash
# 查看前端容器内的 nginx 配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf

# 检查 nginx 错误日志
docker exec colorful-tibet-frontend cat /var/log/nginx/error.log 2>/dev/null
```

#### 步骤 5：修复 nginx 代理配置（如果需要）

如果 nginx 配置有问题，需要重新构建前端：

```bash
cd /opt/colorful-tibet

# 重新构建前端
docker compose build frontend

# 重启前端
docker compose restart frontend
```

## 🔧 快速排查步骤

```bash
# 1. 检查后端状态
echo "=== 后端状态 ==="
docker compose ps backend

# 2. 查看后端日志（最后 20 行）
echo "=== 后端日志 ==="
docker compose logs backend | tail -20

# 3. 测试后端 API
echo "=== 测试后端 ==="
curl http://localhost:8080/api/spots

# 4. 测试前端到后端连接
echo "=== 测试前端到后端 ==="
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1

# 5. 检查 Docker 网络
echo "=== Docker 网络 ==="
docker network inspect colorful-tibet_default | grep -A 5 "Containers"

# 6. 检查 nginx 配置
echo "=== Nginx 配置 ==="
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 10 "location /api"
```

## 🚀 快速修复

### 如果后端未启动

```bash
# 等待后端启动
docker compose logs -f backend
# 看到 "Started TibetTourismApplication" 后按 Ctrl+C

# 然后测试
curl http://localhost:8080/api/spots
```

### 如果 Docker 网络问题

```bash
# 重新创建网络
docker compose down
docker compose up -d

# 等待服务启动
sleep 30

# 测试
curl http://localhost:8080/api/spots
```

### 如果 nginx 配置问题

```bash
# 重新构建前端
docker compose build frontend
docker compose restart frontend

# 测试
curl -I http://localhost/api/spots
```

## 📝 验证步骤

修复后，验证：

```bash
# 1. 后端直接访问
curl http://localhost:8080/api/spots
# 应该返回 JSON 数据

# 2. 前端容器访问后端
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots
# 应该返回 JSON 数据

# 3. 通过 nginx 代理访问
curl http://localhost/api/spots
# 应该返回 JSON 数据

# 4. 浏览器访问
# http://1.15.29.168
# 应该不再显示 API 连接错误
```

## ⚠️ 常见问题

### 问题 1：后端还在启动中

Spring Boot 启动需要时间，等待看到 "Started TibetTourismApplication"。

### 问题 2：backend 服务名无法解析

检查 Docker 网络：
```bash
docker network inspect colorful-tibet_default
```

### 问题 3：nginx 配置未生效

需要重新构建前端镜像：
```bash
docker compose build frontend
docker compose restart frontend
```

