# 前端 API 路径问题修复

## 问题确认

浏览器 Network 标签显示：
- 请求 URL：`spots?locale=zh`（缺少 `/api` 前缀）
- 响应：`Content-Type: text/html`（Nginx 404 页面，不是 JSON）

**根本原因**：前端构建时 `VITE_API_BASE_URL` 环境变量没有正确注入，导致构建后的代码中 `baseURL` 为空或错误。

## 解决方案

### 方案 1：添加 Nginx 重写规则（快速修复，已实施）

已在 `frontend/nginx.conf` 中添加重写规则，自动将缺少 `/api` 前缀的请求重写为 `/api/xxx`。

**优点**：
- 无需重新构建前端
- 立即生效

**缺点**：
- 只是临时方案，不能解决根本问题

### 方案 2：修复构建配置（推荐，根本解决）

确保构建时 `VITE_API_BASE_URL` 正确传递。

#### 步骤 1：检查 .env 文件

在服务器上检查 `.env` 文件：

```bash
cd /opt/colorful-tibet
cat .env | grep VITE_API_BASE_URL
```

如果不存在或为空，创建/更新 `.env` 文件：

```bash
cat > .env << 'EOF'
# 前端 API 基础路径（必须设置）
VITE_API_BASE_URL=/api

# 后端配置
SPRING_PROFILES_ACTIVE=local
DOUBAO_API_KEY=e23d1e75-53b0-473d-92dc-8d70395b077b
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/chat/completions
DOUBAO_MODEL=ep-20251124231555-hdr7j
JWT_SECRET=tibetTourismSecretKey12345678901234567890
ADMIN_ENCRYPTION_KEY=tibetTourismSecretKey12345678901234567890
EOF
```

#### 步骤 2：重新构建前端

```bash
cd /opt/colorful-tibet

# 停止服务
docker-compose down

# 删除旧容器和镜像
docker rm colorful-tibet-frontend 2>/dev/null || true
docker rmi colorful-tibet-frontend 2>/dev/null || true

# 重新构建（确保使用 .env 中的环境变量）
docker-compose build --no-cache frontend

# 启动服务
docker-compose up -d

# 等待服务启动
sleep 5
```

#### 步骤 3：验证构建结果

```bash
# 检查构建后的代码中是否包含正确的 baseURL
docker exec colorful-tibet-frontend sh -c "grep -r 'baseURL' /usr/share/nginx/html/assets/*.js 2>/dev/null | head -3"
```

应该看到类似 `baseURL:"/api"` 的内容。

#### 步骤 4：浏览器验证

1. **清除浏览器缓存**：
   - 按 `Ctrl+Shift+Delete`
   - 选择"缓存的图片和文件"
   - 点击"清除数据"

2. **打开开发者工具**：
   - 按 `F12`
   - 切换到 `Network` 标签
   - 勾选 `Disable cache`

3. **访问网站**：
   - 打开 `http://1.15.29.168`
   - 查看 Network 标签

4. **检查 API 请求**：
   - 找到 `spots?locale=zh` 请求
   - 点击查看详情
   - **Request URL** 应该是 `http://1.15.29.168/api/spots?locale=zh`
   - **Status Code** 应该是 `200`
   - **Content-Type** 应该是 `application/json`

## 诊断命令

### 检查环境变量传递

```bash
# 检查 docker-compose 配置
docker-compose config | grep VITE_API_BASE_URL

# 检查构建时的环境变量（需要在构建过程中）
# 在 Dockerfile 中添加：RUN echo "VITE_API_BASE_URL=$VITE_API_BASE_URL"
```

### 检查构建后的代码

```bash
# 在服务器上
docker exec colorful-tibet-frontend sh -c "grep -o 'baseURL[^,}]*' /usr/share/nginx/html/assets/*.js | head -5"
```

### 测试 API 请求

```bash
# 测试直接访问后端
curl http://localhost:8080/api/spots | head -20

# 测试通过 Nginx 代理（带 /api 前缀）
curl http://localhost/api/spots | head -20

# 测试通过 Nginx 代理（不带 /api 前缀，应该被重写）
curl http://localhost/spots?locale=zh | head -20
```

## 完整修复流程（推荐）

```bash
# 1. 在本地提交代码
git add frontend/nginx.conf
git commit -m "添加 Nginx 重写规则，兼容缺少 /api 前缀的请求"
git push

# 2. 在服务器上更新
cd /opt/colorful-tibet
git pull

# 3. 确保 .env 文件存在且包含 VITE_API_BASE_URL
cat .env | grep VITE_API_BASE_URL || echo "VITE_API_BASE_URL=/api" >> .env

# 4. 重新构建
docker-compose down
docker rm colorful-tibet-frontend 2>/dev/null || true
docker rmi colorful-tibet-frontend 2>/dev/null || true
docker-compose build --no-cache frontend
docker-compose up -d

# 5. 验证
sleep 5
echo "=== 测试后端直接访问 ==="
curl -s http://localhost:8080/api/spots | head -5
echo ""
echo "=== 测试 Nginx 代理（带 /api） ==="
curl -s http://localhost/api/spots | head -5
echo ""
echo "=== 测试 Nginx 代理（不带 /api，重写规则） ==="
curl -s http://localhost/spots?locale=zh | head -5
```

## 验证清单

修复完成后，请确认：

- [ ] `.env` 文件中包含 `VITE_API_BASE_URL=/api`
- [ ] `docker-compose config | grep VITE_API_BASE_URL` 显示正确的值
- [ ] 构建后的代码中包含 `baseURL:"/api"` 或类似内容
- [ ] `curl http://localhost/api/spots` 返回 JSON 数据
- [ ] `curl http://localhost/spots?locale=zh` 返回 JSON 数据（重写规则生效）
- [ ] 浏览器 Network 标签中请求的完整 URL 是 `http://1.15.29.168/api/spots?locale=zh`
- [ ] 浏览器中 API 请求返回 200 状态码和 JSON 数据

## 注意事项

1. **Nginx 重写规则是临时方案**：虽然可以解决问题，但最好修复前端构建配置，确保 `baseURL` 正确设置。

2. **清除浏览器缓存很重要**：旧的 JavaScript 文件可能仍在使用错误的 `baseURL`，必须清除缓存。

3. **环境变量命名**：Vite 只会在构建时注入以 `VITE_` 开头的环境变量，确保使用 `VITE_API_BASE_URL` 而不是 `API_BASE_URL`。

4. **构建时 vs 运行时**：`VITE_API_BASE_URL` 是在构建时注入的，运行时修改 `.env` 不会影响已构建的前端代码，必须重新构建。

