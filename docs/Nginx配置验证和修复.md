# Nginx 配置验证和修复

## 问题确认

执行 `docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots` 仍然返回 404，说明：
1. Nginx 配置可能没有更新到容器中
2. 或者配置更新了但 Nginx 没有重新加载
3. 或者配置本身有问题

## 诊断步骤

### 1. 检查容器内的实际 Nginx 配置

```bash
# 查看容器内的 Nginx 配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf
```

**期望看到**：
- `location ^~ /api {` 配置存在
- `proxy_pass http://backend:8080;` 配置正确

### 2. 检查 Nginx 配置语法

```bash
# 测试 Nginx 配置语法
docker exec colorful-tibet-frontend nginx -t
```

应该显示 `syntax is ok` 和 `test is successful`。

### 3. 检查后端服务是否可访问

```bash
# 测试直接访问后端（应该成功）
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots | head -20
```

### 4. 检查容器网络

```bash
# 检查容器是否在同一个网络中
docker network inspect colorful-tibet_default | grep -A 5 backend
```

## 修复步骤

### 方案 1：重新构建前端容器（推荐）

```bash
cd /opt/colorful-tibet

# 停止服务
docker-compose down

# 删除旧的前端容器和镜像
docker rm colorful-tibet-frontend 2>/dev/null || true
docker rmi colorful-tibet-frontend 2>/dev/null || true

# 重新构建（确保使用最新的 nginx.conf）
docker-compose build --no-cache frontend

# 启动服务
docker-compose up -d

# 等待几秒让服务启动
sleep 5

# 验证
docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots | head -20
```

### 方案 2：手动更新容器内的 Nginx 配置（临时方案）

如果不想重新构建，可以手动更新：

```bash
# 1. 从主机复制 nginx.conf 到容器
docker cp /opt/colorful-tibet/frontend/nginx.conf colorful-tibet-frontend:/etc/nginx/conf.d/default.conf

# 2. 测试配置语法
docker exec colorful-tibet-frontend nginx -t

# 3. 重新加载 Nginx（不重启容器）
docker exec colorful-tibet-frontend nginx -s reload

# 4. 验证
docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots | head -20
```

### 方案 3：检查并修复配置问题

如果配置已经更新但仍然 404，可能是配置问题：

```bash
# 1. 查看完整配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf

# 2. 查看 Nginx 错误日志
docker logs colorful-tibet-frontend 2>&1 | grep -i error | tail -20

# 3. 查看 Nginx 访问日志
docker exec colorful-tibet-frontend tail -20 /var/log/nginx/access.log 2>/dev/null || echo "访问日志未启用"
```

## 常见问题排查

### 问题 1：配置文件没有复制到容器

**症状**：`cat /etc/nginx/conf.d/default.conf` 显示的内容与 `frontend/nginx.conf` 不一致。

**解决**：重新构建容器（方案 1）。

### 问题 2：Nginx 配置语法错误

**症状**：`nginx -t` 显示语法错误。

**解决**：
1. 检查 `frontend/nginx.conf` 文件语法
2. 修复后重新构建

### 问题 3：后端服务不可访问

**症状**：`wget http://backend:8080/api/spots` 失败。

**解决**：
1. 检查后端容器是否运行：`docker ps | grep backend`
2. 检查后端健康状态：`docker ps | grep backend` 应该显示 `Up` 和 `(healthy)`
3. 检查容器网络：确保 `backend` 和 `frontend` 在同一个 Docker 网络中

### 问题 4：Nginx 没有重新加载配置

**症状**：配置已更新但 Nginx 仍使用旧配置。

**解决**：
```bash
# 重新加载 Nginx
docker exec colorful-tibet-frontend nginx -s reload

# 或者重启容器
docker restart colorful-tibet-frontend
```

## 验证清单

修复后，请确认：

- [ ] `docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf` 显示正确的配置
- [ ] `docker exec colorful-tibet-frontend nginx -t` 显示 `syntax is ok`
- [ ] `docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots` 返回 JSON 数据
- [ ] `docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots` 返回 JSON 数据（不再 404）
- [ ] 浏览器访问 `http://1.15.29.168` 可以正常加载数据

## 快速修复命令（一键执行）

```bash
cd /opt/colorful-tibet && \
docker-compose down && \
docker rm colorful-tibet-frontend 2>/dev/null || true && \
docker rmi colorful-tibet-frontend 2>/dev/null || true && \
docker-compose build --no-cache frontend && \
docker-compose up -d && \
sleep 5 && \
echo "=== 验证配置 ===" && \
docker exec colorful-tibet-frontend nginx -t && \
echo "=== 测试后端直接访问 ===" && \
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1 | head -5 && \
echo "=== 测试 Nginx 代理 ===" && \
docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots 2>&1 | head -5
```

