# Nginx 配置未生效问题修复

## 问题确认

从错误日志可以看到：
```
open() "/usr/share/nginx/html/api/spots" failed (2: No such file or directory)
```

这说明 Nginx 正在尝试将 `/api/spots` 作为静态文件处理，而不是代理到后端。

**根本原因**：容器内的 `nginx.conf` 配置没有正确更新，或者配置规则没有正确匹配。

## 诊断步骤

### 1. 检查容器内的实际配置

```bash
# 查看容器内的 Nginx 配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf
```

**期望看到**：
- `location ^~ /api {` 配置存在
- `proxy_pass http://backend:8080;` 配置正确
- `location /api` 在 `location /` 之前

### 2. 检查配置文件是否正确复制

```bash
# 在服务器上检查源文件
cd /opt/colorful-tibet
cat frontend/nginx.conf | grep -A 5 "location.*api"
```

### 3. 检查 Nginx 配置语法

```bash
docker exec colorful-tibet-frontend nginx -t
```

## 修复步骤

### 方案 1：手动更新容器内的配置（快速修复）

```bash
# 1. 从主机复制配置文件到容器
docker cp /opt/colorful-tibet/frontend/nginx.conf colorful-tibet-frontend:/etc/nginx/conf.d/default.conf

# 2. 测试配置语法
docker exec colorful-tibet-frontend nginx -t

# 3. 重新加载 Nginx（不重启容器）
docker exec colorful-tibet-frontend nginx -s reload

# 4. 验证
curl http://localhost/api/spots
```

### 方案 2：重新构建容器（推荐，彻底解决）

```bash
cd /opt/colorful-tibet

# 1. 停止服务
docker compose down

# 2. 删除旧容器和镜像
docker rm colorful-tibet-frontend 2>/dev/null || true
docker rmi colorful-tibet-frontend 2>/dev/null || true

# 3. 确认配置文件存在且正确
cat frontend/nginx.conf | grep -A 10 "location.*api"

# 4. 重新构建
docker compose build --no-cache frontend

# 5. 启动服务
docker compose up -d

# 6. 等待启动
sleep 5

# 7. 验证配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 10 "location.*api"

# 8. 测试
curl http://localhost/api/spots
```

## 配置验证清单

修复后，请确认：

- [ ] `docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf` 包含 `location ^~ /api` 配置
- [ ] `docker exec colorful-tibet-frontend nginx -t` 显示 `syntax is ok`
- [ ] `curl http://localhost/api/spots` 返回 JSON 数据（不是 404 HTML）
- [ ] 浏览器访问 `http://1.15.29.168` 可以正常加载数据

## 如果问题仍然存在

### 检查配置顺序

确保 `location /api` 在 `location /` 之前：

```nginx
# 正确的顺序
location ^~ /api {
    proxy_pass http://backend:8080;
    ...
}

location / {
    try_files $uri $uri/ /index.html;
}
```

### 检查是否有其他 location 规则干扰

```bash
# 查看完整配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf
```

### 检查后端服务是否可访问

```bash
# 测试后端直接访问
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots

# 检查容器网络
docker network inspect colorful-tibet_default | grep -A 5 backend
```

