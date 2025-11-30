# Nginx 代理问题排查指南

## 问题现象
前端页面可以访问，但 API 请求返回 404 错误。

## 诊断步骤

### 1. 检查前端容器内的 Nginx 配置

```bash
# 进入前端容器
docker exec -it colorful-tibet-frontend sh

# 查看 Nginx 配置
cat /etc/nginx/conf.d/default.conf

# 测试 Nginx 配置语法
nginx -t

# 退出容器
exit
```

### 2. 检查 Nginx 错误日志

```bash
# 查看前端容器的 Nginx 错误日志
docker logs colorful-tibet-frontend 2>&1 | grep -i error

# 或者实时查看日志
docker logs -f colorful-tibet-frontend
```

### 3. 测试从容器内部访问后端

```bash
# 从前端容器内部测试后端连接
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots

# 测试 Nginx 代理
docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots
```

### 4. 检查容器网络连接

```bash
# 检查前端容器是否能解析 backend 主机名
docker exec colorful-tibet-frontend ping -c 3 backend

# 检查后端容器是否在运行
docker ps | grep backend

# 检查容器网络
docker network inspect colorful-tibet_default
```

### 5. 检查浏览器网络请求

在浏览器中打开开发者工具（F12），查看 Network 标签：
- 查看 API 请求的完整 URL
- 查看响应状态码和错误信息
- 查看请求头信息

### 6. 验证 Nginx 配置是否正确加载

```bash
# 重新加载 Nginx 配置（如果配置已修改）
docker exec colorful-tibet-frontend nginx -s reload

# 或者重启前端容器
docker restart colorful-tibet-frontend
```

## 常见问题及解决方案

### 问题1：Nginx 配置未正确复制

**症状**：`cat /etc/nginx/conf.d/default.conf` 显示的内容与 `frontend/nginx.conf` 不一致。

**解决**：重新构建前端镜像
```bash
cd /opt/colorful-tibet
docker-compose down
docker-compose build --no-cache frontend
docker-compose up -d
```

### 问题2：Nginx 配置语法错误

**症状**：`nginx -t` 显示语法错误。

**解决**：检查 `frontend/nginx.conf` 文件，确保语法正确。

### 问题3：容器网络问题

**症状**：`ping backend` 失败或 `wget http://backend:8080/api/spots` 失败。

**解决**：
1. 确保 `docker-compose.yml` 中服务名称是 `backend`
2. 确保两个容器在同一个 Docker 网络中
3. 检查 `depends_on` 配置

### 问题4：后端未启动或未就绪

**症状**：后端容器状态为 `unhealthy` 或 `restarting`。

**解决**：
1. 查看后端日志：`docker logs colorful-tibet-backend`
2. 检查后端健康检查：`curl http://localhost:8080/api/spots`
3. 等待后端完全启动后再测试前端

### 问题5：路径不匹配

**症状**：请求 `/api/spots` 但后端期望不同路径。

**解决**：确保 Nginx `proxy_pass` 配置正确：
```nginx
location /api {
    proxy_pass http://backend:8080;  # 注意：没有尾随斜杠，会保留 /api 前缀
    ...
}
```

## 快速修复命令

如果确认是配置问题，可以尝试以下快速修复：

```bash
# 1. 停止服务
cd /opt/colorful-tibet
docker-compose down

# 2. 重新构建前端（确保最新配置）
docker-compose build --no-cache frontend

# 3. 启动服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f frontend
```

## 验证步骤

修复后，按以下步骤验证：

1. **检查容器状态**：
   ```bash
   docker ps
   ```
   应该看到 `colorful-tibet-backend` 和 `colorful-tibet-frontend` 都是 `Up` 状态。

2. **测试后端 API**：
   ```bash
   curl http://localhost:8080/api/spots
   ```
   应该返回 JSON 数据。

3. **测试前端容器内的代理**：
   ```bash
   docker exec colorful-tibet-frontend wget -O- http://localhost/api/spots
   ```
   应该返回 JSON 数据。

4. **在浏览器中访问**：
   - 打开 `http://1.15.29.168`
   - 打开开发者工具（F12）
   - 查看 Network 标签中的 API 请求
   - 应该看到请求成功（状态码 200）

