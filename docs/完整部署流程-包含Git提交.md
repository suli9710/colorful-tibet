# 完整部署流程（包含 Git 提交）

## 📋 完整流程概览

1. **本地修改代码** ✅
2. **提交到 Git** ⬅️ 当前步骤
3. **推送到 GitHub**
4. **服务器拉取代码**
5. **重新构建和部署**

## 🔄 当前需要提交的文件

根据 `git status`，需要提交以下文件：

### 已修改的文件
- `deploy-tencent-cloud.sh` - 更新为使用 `docker compose`
- `docs/前端API路径问题修复.md` - 文档更新

### 新增的文件
- `frontend/nginx.conf` - 如果之前未提交，需要提交（包含 API 代理和重写规则）
- `docs/Docker-Compose-V2使用说明.md` - 新文档
- `docs/Nginx配置未生效问题修复.md` - 新文档
- `docs/快速重建和重启.md` - 新文档
- `fix-nginx-config.sh` - 修复脚本
- `fix-nginx.sh` - 诊断脚本
- `rebuild-and-restart.sh` - 重建脚本

## 📝 步骤 1：在本地提交代码

```bash
# 在本地项目目录
cd C:\Users\Suli\Desktop\colorful-tibet

# 查看修改状态
git status

# 添加所有修改的文件
git add frontend/nginx.conf
git add deploy-tencent-cloud.sh
git add fix-nginx-config.sh
git add fix-nginx.sh
git add rebuild-and-restart.sh
git add docs/

# 提交
git commit -m "fix: 修复 Nginx 配置，添加 API 代理和重写规则，更新为 Docker Compose V2 语法"

# 推送到 GitHub
git push origin main
```

## 📥 步骤 2：在服务器上拉取代码

```bash
# 在服务器上
cd /opt/colorful-tibet

# 拉取最新代码
git pull

# 验证文件已更新
git log -1
ls -la frontend/nginx.conf
```

## 🔧 步骤 3：修复 Nginx 配置（快速方案）

```bash
cd /opt/colorful-tibet

# 方法 1：手动复制配置并重新加载（快速）
docker cp frontend/nginx.conf colorful-tibet-frontend:/etc/nginx/conf.d/default.conf
docker exec colorful-tibet-frontend nginx -t
docker exec colorful-tibet-frontend nginx -s reload

# 方法 2：使用脚本
chmod +x fix-nginx-config.sh
bash fix-nginx-config.sh
```

## 🏗️ 步骤 4：重新构建（彻底方案，推荐）

```bash
cd /opt/colorful-tibet

# 停止服务
docker compose down

# 删除旧容器和镜像
docker rm colorful-tibet-frontend 2>/dev/null || true
docker rmi colorful-tibet-frontend 2>/dev/null || true

# 重新构建
docker compose build --no-cache frontend

# 启动服务
docker compose up -d

# 等待启动
sleep 10

# 验证
docker compose ps
curl http://localhost/api/spots
```

## ✅ 步骤 5：验证部署

```bash
# 1. 检查服务状态
docker compose ps

# 2. 检查 Nginx 配置
docker exec colorful-tibet-frontend cat /etc/nginx/conf.d/default.conf | grep -A 10 "location.*api"

# 3. 测试后端直接访问
docker exec colorful-tibet-frontend wget -O- http://backend:8080/api/spots 2>&1 | head -5

# 4. 测试 Nginx 代理
curl http://localhost/api/spots

# 5. 浏览器访问
# 打开 http://1.15.29.168
```

## 🚀 一键执行脚本

### 本地（Windows PowerShell）

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet
git add .
git commit -m "fix: 修复 Nginx 配置，添加 API 代理和重写规则"
git push origin main
```

### 服务器（Linux Bash）

```bash
cd /opt/colorful-tibet
git pull
docker compose down
docker compose build --no-cache frontend
docker compose up -d
sleep 10
docker compose ps
curl http://localhost/api/spots
```

## 📋 检查清单

提交前确认：
- [ ] `frontend/nginx.conf` 包含 `location ^~ /api` 配置
- [ ] `frontend/nginx.conf` 包含重写规则（处理缺少 /api 前缀的请求）
- [ ] 所有脚本使用 `docker compose`（V2 语法）
- [ ] 文档已更新

部署后验证：
- [ ] `git pull` 成功拉取最新代码
- [ ] `docker compose ps` 显示服务正常运行
- [ ] `curl http://localhost/api/spots` 返回 JSON 数据
- [ ] 浏览器可以正常访问网站

## ⚠️ 注意事项

1. **提交前检查**：确保所有修改都是正确的
2. **测试本地**：如果可能，先在本地测试配置
3. **备份数据**：如果服务器有重要数据，先备份
4. **逐步部署**：先更新代码，再重新构建，最后重启服务

## 🔍 故障排查

### 问题：Git push 失败

```bash
# 检查远程仓库
git remote -v

# 检查分支
git branch

# 强制推送（谨慎使用）
git push origin main --force
```

### 问题：服务器 git pull 失败

```bash
# 检查是否有未提交的修改
git status

# 暂存本地修改
git stash

# 拉取代码
git pull

# 恢复本地修改（如果需要）
git stash pop
```

### 问题：配置文件未更新

```bash
# 检查文件内容
cat frontend/nginx.conf | grep -A 10 "location.*api"

# 检查 Git 历史
git log --oneline -5 -- frontend/nginx.conf

# 强制拉取
git fetch origin
git reset --hard origin/main
```

