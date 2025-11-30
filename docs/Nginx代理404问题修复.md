# Nginx 代理 404 问题修复

## 问题描述

前端容器内的 Nginx 无法正确代理 `/api` 请求到后端服务，返回 404 错误。

**诊断结果**：
- ✅ 直接访问后端成功：`docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots` 返回数据
- ❌ 通过 Nginx 代理失败：`docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots` 返回 404

## 问题原因

Nginx 的 `location` 匹配规则可能导致 `/api` 请求被 `location /` 规则捕获，而不是被 `location /api` 处理。

## 解决方案

已优化 `frontend/nginx.conf` 配置：
1. 将 `location /api` 移到 `location /` 之前
2. 使用 `^~` 修饰符确保 `/api` 优先匹配

## 修复步骤

### 1. 在本地提交代码更改

```bash
# 在本地项目目录
git add frontend/nginx.conf
git commit -m "修复 Nginx 代理配置，确保 /api 请求正确转发"
git push
```

### 2. 在服务器上更新代码并重新部署

```bash
# 进入项目目录
cd /opt/colorful-tibet

# 拉取最新代码
git pull

# 停止服务
docker-compose down

# 重新构建前端镜像（确保使用最新的 nginx.conf）
docker-compose build --no-cache frontend

# 启动服务
docker-compose up -d

# 查看日志确认服务正常
docker-compose logs -f frontend
```

### 3. 验证修复

```bash
# 测试后端直接访问（应该成功）
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots

# 测试 Nginx 代理（现在应该成功）
docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots

# 检查容器状态
docker ps
```

### 4. 浏览器验证

1. 打开浏览器访问：`http://1.15.29.168`
2. 打开开发者工具（F12）
3. 切换到 Network 标签
4. 刷新页面
5. 查看 API 请求应该返回 200 状态码，而不是 404

## 配置说明

修复后的 `nginx.conf` 关键配置：

```nginx
# API代理（使用 ^~ 确保优先匹配）
location ^~ /api {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # 增加超时时间（适应 AI 生成）
    proxy_connect_timeout 120s;
    proxy_send_timeout 120s;
    proxy_read_timeout 120s;
    
    # 错误处理
    proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
}

# 前端路由支持（Vue Router history模式）
location / {
    try_files $uri $uri/ /index.html;
}
```

**关键点**：
- `^~` 修饰符表示如果这个规则匹配，停止搜索其他规则
- `location /api` 在 `location /` 之前，确保更具体的规则优先匹配
- `proxy_pass http://backend:8080` 会保留原始路径，所以 `/api/spots` 会被转发到 `http://backend:8080/api/spots`

## 如果问题仍然存在

如果修复后问题仍然存在，请检查：

1. **确认配置已更新**：
   ```bash
   docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf
   ```
   应该看到 `location ^~ /api` 配置。

2. **检查 Nginx 配置语法**：
   ```bash
   docker exec colorful-tibet-frontend nginx -t
   ```
   应该显示 `syntax is ok`。

3. **重新加载 Nginx**：
   ```bash
   docker exec colorful-tibet-frontend nginx -s reload
   ```

4. **查看 Nginx 错误日志**：
   ```bash
   docker logs colorful-tibet-frontend 2>&1 | grep -i error
   ```

5. **检查容器网络**：
   ```bash
   docker network inspect colorful-tibet_default | grep -A 10 backend
   ```
   确认 `backend` 服务在同一个网络中。

