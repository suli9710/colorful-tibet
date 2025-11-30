# Nginx 404 问题深度排查

## 问题现象

即使重新构建后，访问 `/api/spots` 仍然返回 404 Not Found（Nginx 默认 404 页面）。

## 诊断步骤

### 1. 检查容器内的实际配置

```bash
# 查看容器内的完整配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf

# 只查看 location 规则
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 15 "location"
```

**期望看到**：
- `location ^~ /api {` 配置存在
- `proxy_pass http://backend:8080;` 配置正确
- `/api` 规则在 `/` 规则之前

### 2. 检查配置语法

```bash
docker exec colorful-tibet-frontend nginx -t
```

应该显示 `syntax is ok` 和 `test is successful`。

### 3. 检查主机上的配置文件

```bash
cd /opt/colorful-tibet
cat frontend/nginx.conf | grep -A 15 "location"
```

### 4. 检查后端服务

```bash
# 测试后端直接访问
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots

# 检查后端容器状态
docker compose ps backend

# 检查后端日志
docker compose logs --tail=20 backend
```

### 5. 检查容器网络

```bash
# 检查容器是否在同一个网络
docker network inspect colorful-tibet_default | grep -A 10 backend

# 测试容器间连接
docker exec colorful-tibet-frontend ping -c 3 backend
```

### 6. 查看 Nginx 错误日志

```bash
# 查看详细错误日志
docker logs colorful-tibet-frontend 2>&1 | grep -i error | tail -20

# 查看访问日志
docker logs colorful-tibet-frontend 2>&1 | grep "GET /api" | tail -10
```

## 可能的原因和解决方案

### 原因 1：配置文件未正确复制到容器

**症状**：容器内的配置与主机上的配置不一致。

**解决**：
```bash
# 方法 1：手动复制并重新加载
docker cp /opt/colorful-tibet/frontend/nginx.conf colorful-tibet-frontend:/etc/nginx/conf.d/default.conf
docker exec colorful-tibet-frontend nginx -t
docker exec colorful-tibet-frontend nginx -s reload

# 方法 2：重新构建
docker compose down
docker compose build --no-cache frontend
docker compose up -d
```

### 原因 2：配置语法错误

**症状**：`nginx -t` 显示语法错误。

**解决**：
1. 检查配置文件语法
2. 修复错误
3. 重新构建或重新加载

### 原因 3：location 规则顺序问题

**症状**：`location /` 规则在 `location /api` 之前匹配。

**解决**：确保配置顺序正确：
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

### 原因 4：后端服务不可访问

**症状**：`wget http://backend:8080/api/spots` 失败。

**解决**：
```bash
# 检查后端容器状态
docker compose ps

# 检查后端日志
docker compose logs backend

# 重启后端
docker compose restart backend
```

### 原因 5：容器网络问题

**症状**：`ping backend` 失败。

**解决**：
```bash
# 检查 docker-compose.yml 中的服务名称
cat docker-compose.yml | grep -A 5 "services:"

# 确保服务名称是 "backend"（不是 "colorful-tibet-backend"）
# 在 nginx.conf 中使用的是 "backend"，这是 Docker Compose 的服务名
```

### 原因 6：Nginx 配置缓存

**症状**：配置已更新但 Nginx 仍使用旧配置。

**解决**：
```bash
# 强制重新加载
docker exec colorful-tibet-frontend nginx -s reload

# 或者重启容器
docker compose restart frontend
```

## 完整修复流程

```bash
cd /opt/colorful-tibet

# 1. 诊断
bash diagnose-nginx.sh

# 2. 检查配置差异
echo "=== 主机配置 ==="
cat frontend/nginx.conf | grep -A 10 "location.*api"
echo ""
echo "=== 容器配置 ==="
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 10 "location.*api"

# 3. 如果配置不一致，手动修复
docker cp frontend/nginx.conf colorful-tibet-frontend:/etc/nginx/conf.d/default.conf
docker exec colorful-tibet-frontend nginx -t
docker exec colorful-tibet-frontend nginx -s reload

# 4. 验证
curl -v http://localhost/api/spots 2>&1 | head -30
```

## 验证清单

修复后，请确认：

- [ ] `docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf` 包含正确的 `/api` 配置
- [ ] `docker exec colorful-tibet-frontend nginx -t` 显示 `syntax is ok`
- [ ] `docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots` 返回 JSON 数据
- [ ] `curl http://localhost/api/spots` 返回 JSON 数据（不是 404 HTML）
- [ ] 浏览器访问 `http://1.15.29.168` 可以正常加载数据

## 如果问题仍然存在

请运行诊断脚本并提供完整输出：

```bash
cd /opt/colorful-tibet
bash diagnose-nginx.sh > nginx-diagnosis.txt 2>&1
cat nginx-diagnosis.txt
```

然后将输出内容发给我，我会进一步分析。

