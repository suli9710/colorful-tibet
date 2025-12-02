# 西藏旅游网站 - 部署说明

## 📦 项目结构

```
colorful-tibet/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue.js 前端
├── docker-compose.yml        # Docker Compose 配置（开发/测试环境）
├── docker-compose.prod.yml   # Docker Compose 配置（生产环境）
├── deploy.sh                 # 传统部署脚本
├── deploy-tencent-cloud.sh   # 腾讯云快速部署脚本
└── docs/             # 文档目录
    ├── 腾讯云部署详细教程.md    # 腾讯云详细部署教程（推荐）
    ├── 快速部署指南.md          # 快速部署指南
    └── 环境变量配置说明.md      # 环境变量配置说明
```

## 🚀 快速开始

### 方式一：一键部署（推荐）

```bash
# 1. 上传项目到服务器
cd /opt
git clone <your-repo-url> colorful-tibet
cd colorful-tibet

# 2. 运行部署脚本
chmod +x deploy-tencent-cloud.sh
./deploy-tencent-cloud.sh

# 3. 配置环境变量
nano .env  # 编辑配置文件，填入 API Key 等信息

# 4. 重启服务
docker-compose restart
```

### 方式二：手动部署

详细步骤请参考：[腾讯云部署详细教程.md](./docs/腾讯云部署详细教程.md)

## 📋 部署前准备

1. **服务器要求**
   - Ubuntu 22.04 或 CentOS 7.9
   - 至少 2核4GB 内存
   - 50GB 硬盘空间

2. **必需配置**
   - 豆包 API Key
   - 豆包 API URL
   - 豆包模型名称

3. **可选配置**
   - MySQL 数据库（生产环境推荐）
   - 域名和 SSL 证书

## 🔧 环境变量配置

创建 `.env` 文件（参考 `.env.example`）：

```bash
cp .env.example .env
nano .env
```

**必需配置：**
- `DOUBAO_API_KEY` - 豆包 API Key
- `DOUBAO_API_URL` - 豆包 API 地址
- `DOUBAO_MODEL` - 豆包模型名称
- `JWT_SECRET` - JWT 密钥（至少32字符）
- `ADMIN_ENCRYPTION_KEY` - 管理员加密密钥（至少32字符）

## 🐳 Docker 部署

### 开发/测试环境

```bash
docker-compose up -d
```

### 生产环境

```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 📝 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
docker-compose logs backend
docker-compose logs frontend

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 更新代码
git pull
docker-compose build
docker-compose up -d
```

## 🌐 访问地址

部署成功后，访问：
- **前端**: http://您的服务器IP
- **后端 API**: http://您的服务器IP:8080/api

## 🔒 安全建议

1. **修改默认密钥**：生产环境务必修改 `.env` 中的 `JWT_SECRET` 和 `ADMIN_ENCRYPTION_KEY`
2. **使用 HTTPS**：配置域名和 SSL 证书
3. **配置防火墙**：只开放必要的端口
4. **使用 MySQL**：生产环境使用 MySQL 替代 H2 数据库
5. **定期备份**：定期备份数据库和重要文件

## 📚 详细文档

- [腾讯云部署详细教程](./docs/腾讯云部署详细教程.md) ⭐ **推荐阅读**
- [快速部署指南](./docs/快速部署指南.md)
- [环境变量配置说明](./docs/环境变量配置说明.md)

## 🐛 问题排查

### 服务无法启动

```bash
# 查看日志
docker-compose logs

# 检查端口占用
netstat -tlnp | grep 8080
```

### 前端无法访问后端

1. 检查后端是否运行：`curl http://localhost:8080/api/spots`
2. 检查 `VITE_API_BASE_URL` 配置
3. 检查防火墙规则

### 数据库连接失败

```bash
# 检查 MySQL 服务
systemctl status mysql

# 测试连接
mysql -u tibet_user -p tibet_tourism
```

## 📞 技术支持

如遇到问题，请：
1. 查看日志文件
2. 检查服务器资源使用情况
3. 参考详细部署教程
4. 检查配置文件是否正确

---

**祝您部署顺利！** 🎉


