# API 连接问题排查

## ❌ 错误：无法连接到后端服务 / 404 Not Found

### 问题描述

访问网站时出现错误：
- "无法连接到后端服务!"
- "错误: /api"
- "错误: 404"
- "Not Found"

### 原因

- nginx 无法解析 `backend` 服务名（Docker 网络问题）
- 后端服务未完全启动
- nginx 代理配置问题
- 超时时间设置过短

### 解决方案

#### 步骤 1：检查后端服务是否正常

```bash
# 在服务器上测试后端 API
curl http://localhost:8080/api/spots

# 或者从容器内测试
docker exec colorful-tibet-backend curl http://localhost:8080/api/spots
```

#### 步骤 2：检查 Docker 网络

```bash
# 检查 Docker 网络
docker network ls
docker network inspect colorful-tibet_default

# 检查容器是否在同一网络
docker inspect colorful-tibet-backend | grep NetworkMode
docker inspect colorful-tibet-frontend | grep NetworkMode
```

#### 步骤 3：测试 nginx 代理

```bash
# 进入前端容器
docker exec -it colorful-tibet-frontend sh

# 在容器内测试后端连接
wget -O- http://backend:8080/api/spots

# 或者使用 curl（如果容器内有）
curl http://backend:8080/api/spots
```

#### 步骤 4：检查 nginx 配置

```bash
# 查看前端容器内的 nginx 配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf

# 检查 nginx 错误日志
docker exec colorful-tibet-frontend cat /var/log/nginx/error.log
```

#### 步骤 5：修复 nginx 配置（如果需要）

如果 nginx 配置有问题，需要重新构建前端：

```bash
cd /opt/colorful-tibet

# 重新构建前端（使用更新后的 nginx.conf）
docker compose build frontend

# 重启前端容器
docker compose up -d frontend
```

## 🔧 快速修复步骤

### 方法一：检查并修复网络连接

```bash
# 1. 检查后端是否正常
curl http://localhost:8080/api/spots

# 2. 检查前端容器能否访问后端
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots

# 3. 如果失败，检查 Docker 网络
docker network inspect colorful-tibet_default | grep -A 10 "Containers"
```

### 方法二：使用 IP 地址替代服务名（临时方案）

如果 Docker 服务名无法解析，可以修改 nginx 配置使用 IP：

```bash
# 获取后端容器 IP
docker inspect colorful-tibet-backend | grep IPAddress

# 修改 nginx.conf，使用 IP 地址
# 但这不是推荐方案，应该使用服务名
```

### 方法三：重新构建前端（推荐）

```bash
cd /opt/colorful-tibet

# 重新构建前端（确保 nginx.conf 已更新）
docker compose build --no-cache frontend

# 重启前端
docker compose up -d frontend

# 查看日志
docker compose logs frontend
```

## 🔍 完整诊断命令

```bash
# 1. 测试后端
echo "=== 测试后端 ==="
curl http://localhost:8080/api/spots

# 2. 检查容器网络
echo "=== 检查网络 ==="
docker network inspect colorful-tibet_default | grep -A 5 "Containers"

# 3. 测试前端到后端连接
echo "=== 测试前端到后端 ==="
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1

# 4. 查看 nginx 配置
echo "=== Nginx 配置 ==="
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf

# 5. 查看 nginx 错误日志
echo "=== Nginx 错误日志 ==="
docker exec colorful-tibet-frontend cat /var/log/nginx/error.log 2>/dev/null || echo "无错误日志"
```

## ⚠️ 常见问题

### 问题 1：backend 服务名无法解析

**解决**：
```bash
# 确保两个容器在同一网络
docker compose down
docker compose up -d

# 检查网络
docker network inspect colorful-tibet_default
```

### 问题 2：后端服务未完全启动

**解决**：
```bash
# 等待后端完全启动
sleep 30

# 检查后端日志
docker compose logs backend | tail -20

# 测试后端
curl http://localhost:8080/api/spots
```

### 问题 3：nginx 配置未生效

**解决**：
```bash
# 重新构建前端
docker compose build --no-cache frontend

# 重启前端
docker compose restart frontend
```

## 📝 验证步骤

修复后，验证：

```bash
# 1. 测试后端
curl http://localhost:8080/api/spots

# 2. 测试前端容器到后端
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots

# 3. 测试前端页面
curl -I http://localhost

# 4. 在浏览器访问
# http://1.15.29.168
```

## 🎯 成功标志

修复成功后：
- `curl http://localhost:8080/api/spots` 返回 JSON 数据
- `docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots` 成功
- 浏览器访问 `http://1.15.29.168` 不再显示 API 连接错误

