# 安装 Docker Compose 指南

## ❌ 错误：Command 'docker-compose' not found

### 问题描述

执行 `docker-compose` 命令时出现：
```
Command 'docker-compose' not found, but can be installed with:
sudo snap install docker # version 28.4.0, or
sudo apt install docker-compose # version 1.29.2-1
```

### 解决方案

#### 方案一：安装 Docker Compose v2（推荐）

Docker Compose v2 是官方推荐的新版本，作为 Docker 的插件使用：

```bash
# 1. 安装 Docker（如果还没有安装）
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo systemctl start docker
sudo systemctl enable docker

# 2. 安装 Docker Compose v2（作为 Docker 插件）
sudo apt update
sudo apt install docker-compose-plugin -y

# 3. 验证安装
docker compose version
```

**注意**：v2 版本使用 `docker compose`（有空格），而不是 `docker-compose`（有连字符）。

#### 方案二：安装 Docker Compose v1（传统方式）

如果习惯使用 `docker-compose` 命令：

```bash
# 安装 Docker Compose v1
sudo apt update
sudo apt install docker-compose -y

# 验证安装
docker-compose --version
```

#### 方案三：手动安装 Docker Compose v2（最新版本）

```bash
# 下载最新版本的 Docker Compose
DOCKER_COMPOSE_VERSION="v2.20.0"
sudo curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

## 🔄 使用方式

### Docker Compose v1（传统方式）

```bash
docker-compose build
docker-compose up -d
docker-compose ps
docker-compose logs -f
```

### Docker Compose v2（新方式）

```bash
docker compose build
docker compose up -d
docker compose ps
docker compose logs -f
```

## 📝 推荐做法

**推荐使用方案一（Docker Compose v2）**，因为：
- ✅ 官方推荐的新版本
- ✅ 更好的性能和功能
- ✅ 与 Docker CLI 集成更好

**如果项目中使用的是 `docker-compose.yml`，两种方式都兼容。**

## 🔍 检查当前安装

```bash
# 检查 Docker Compose v1
docker-compose --version

# 检查 Docker Compose v2
docker compose version

# 检查 Docker
docker --version
```

## ⚠️ 注意事项

1. **版本兼容性**：v1 和 v2 的命令格式不同，但配置文件（`docker-compose.yml`）兼容
2. **同时安装**：可以同时安装 v1 和 v2，但推荐只使用一个版本
3. **权限问题**：如果遇到权限问题，将用户添加到 docker 组：
   ```bash
   sudo usermod -aG docker $USER
   newgrp docker
   ```

## 🚀 安装后继续部署

安装完成后，继续执行：

```bash
cd /opt/colorful-tibet

# 如果使用 v1
docker-compose build
docker-compose up -d

# 如果使用 v2
docker compose build
docker compose up -d
```

