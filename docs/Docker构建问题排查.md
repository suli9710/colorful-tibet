# Docker 构建问题排查

## ❌ 错误：无法加载镜像元数据

### 问题描述

执行 `docker compose build` 时出现：
```
ERROR [frontend internal] load metadata for docker.io/library/node:18-alpine
ERROR [backend internal] load metadata for docker.io/library/maven:3.9-eclipse-temurin-17
```

### 排查步骤

#### 步骤 1：验证镜像加速器配置

```bash
# 检查配置是否生效
docker info | grep -A 10 "Registry Mirrors"

# 如果没有显示镜像源，说明配置未生效
```

如果未生效，重新配置：
```bash
sudo nano /etc/docker/daemon.json
# 确保内容正确，保存后重启
sudo systemctl restart docker
```

#### 步骤 2：测试网络连接

```bash
# 测试能否访问 Docker Hub
curl -I https://registry-1.docker.io/v2/

# 测试镜像加速器
curl -I https://docker.mirrors.ustc.edu.cn/v2/
```

#### 步骤 3：手动拉取镜像测试

```bash
# 尝试手动拉取一个镜像
docker pull hello-world

# 如果成功，说明 Docker 配置正常
# 如果失败，检查网络和镜像加速器配置
```

#### 步骤 4：清理 Docker 构建缓存

```bash
# 清理所有构建缓存
docker builder prune -a

# 清理未使用的镜像
docker image prune -a
```

#### 步骤 5：检查 Dockerfile 版本

确保 Dockerfile 中的镜像版本正确：

**backend/Dockerfile** 应该使用：
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
```

**frontend/Dockerfile** 应该使用：
```dockerfile
FROM node:20-alpine AS builder
```

如果服务器上的 Dockerfile 还是旧版本，需要更新。

## 🔧 解决方案

### 方案一：重新配置镜像加速器

```bash
# 1. 编辑配置
sudo nano /etc/docker/daemon.json
```

确保内容为：
```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

```bash
# 2. 重启 Docker
sudo systemctl daemon-reload
sudo systemctl restart docker

# 3. 验证
docker info | grep "Registry Mirrors"
```

### 方案二：使用阿里云镜像加速器（推荐）

1. **登录阿里云**：https://cr.console.aliyun.com/
2. **获取加速地址**：容器镜像服务 → 镜像加速器
3. **配置**：

```bash
sudo nano /etc/docker/daemon.json
```

```json
{
  "registry-mirrors": [
    "https://您的专属地址.mirror.aliyuncs.com"
  ]
}
```

```bash
sudo systemctl restart docker
```

### 方案三：手动拉取镜像后构建

```bash
# 手动拉取所需的基础镜像
docker pull maven:3.9-eclipse-temurin-17
docker pull eclipse-temurin:17-jre-alpine
docker pull node:20-alpine
docker pull nginx:alpine

# 然后重新构建
cd /opt/colorful-tibet
docker compose build
```

### 方案四：使用代理（如果有）

如果服务器有代理，可以配置：

```bash
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo nano /etc/systemd/system/docker.service.d/http-proxy.conf
```

添加：
```ini
[Service]
Environment="HTTP_PROXY=http://proxy.example.com:8080"
Environment="HTTPS_PROXY=http://proxy.example.com:8080"
Environment="NO_PROXY=localhost,127.0.0.1"
```

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

## 🔍 完整排查流程

```bash
# 1. 检查 Docker 配置
docker info | grep -A 10 "Registry Mirrors"

# 2. 测试网络
curl -I https://docker.mirrors.ustc.edu.cn/v2/

# 3. 测试拉取镜像
docker pull hello-world

# 4. 清理缓存
docker builder prune -a

# 5. 检查 Dockerfile
cat backend/Dockerfile
cat frontend/Dockerfile

# 6. 重新构建
cd /opt/colorful-tibet
docker compose build --no-cache
```

## ⚠️ 常见问题

### 问题 1：配置文件格式错误

确保 `/etc/docker/daemon.json` 是有效的 JSON 格式：
```bash
# 验证 JSON 格式
cat /etc/docker/daemon.json | python3 -m json.tool
```

### 问题 2：Docker 服务未重启

修改配置后必须重启：
```bash
sudo systemctl restart docker
```

### 问题 3：镜像版本不匹配

检查 Dockerfile 中的镜像版本是否与错误信息一致。

## 📝 推荐操作顺序

1. **验证镜像加速器配置**
2. **重启 Docker 服务**
3. **测试拉取镜像**
4. **清理构建缓存**
5. **重新构建**

## 🚀 快速修复命令

```bash
# 一键修复（如果镜像加速器未配置）
sudo mkdir -p /etc/docker
echo '{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}' | sudo tee /etc/docker/daemon.json

sudo systemctl daemon-reload
sudo systemctl restart docker

# 验证
docker info | grep "Registry Mirrors"

# 重新构建
cd /opt/colorful-tibet
docker compose build
```

