# Docker 镜像源 DNS 解析问题解决

## ❌ 错误：no such host

### 问题描述

执行 `docker pull` 或 `docker compose build` 时出现：
```
dial tcp: lookup docker.mirrors.ustc.edu.cn on 127.0.0.53:53: no such host
```

### 原因

- DNS 无法解析镜像源域名
- 镜像源服务器可能暂时不可用
- 网络连接问题

### 解决方案

#### 方案一：更换可用的镜像源（推荐）

编辑 Docker 配置文件，移除无法访问的镜像源：

```bash
sudo nano /etc/docker/daemon.json
```

**推荐配置**（移除中科大镜像源）：

```json
{
  "registry-mirrors": [
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://dockerproxy.com"
  ]
}
```

保存后重启：
```bash
sudo systemctl restart docker
```

#### 方案二：使用阿里云镜像加速器（最稳定）

1. **登录阿里云**：https://cr.console.aliyun.com/
2. **获取加速地址**：
   - 容器镜像服务 → 镜像加速器
   - 复制您的专属加速地址
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

#### 方案三：直接使用 Docker Hub（如果网络允许）

如果网络可以直接访问 Docker Hub，可以移除所有镜像源：

```bash
sudo nano /etc/docker/daemon.json
```

```json
{
  "registry-mirrors": []
}
```

或者删除镜像源配置：
```bash
sudo rm /etc/docker/daemon.json
sudo systemctl restart docker
```

#### 方案四：测试并选择可用的镜像源

```bash
# 测试各个镜像源
curl -I https://hub-mirror.c.163.com/v2/
curl -I https://mirror.baidubce.com/v2/
curl -I https://dockerproxy.com/v2/

# 选择可以访问的镜像源配置
```

## 🔧 快速修复命令

```bash
# 1. 编辑配置，移除无法访问的镜像源
sudo nano /etc/docker/daemon.json
# 只保留可用的镜像源，保存退出

# 2. 重启 Docker
sudo systemctl restart docker

# 3. 验证配置
docker info | grep "Registry Mirrors"

# 4. 测试拉取镜像
docker pull hello-world

# 5. 如果成功，继续构建
cd /opt/colorful-tibet
docker compose build
```

## 📝 推荐的镜像源配置

### 配置 1：使用网易和百度云（推荐）

```json
{
  "registry-mirrors": [
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

### 配置 2：使用 Docker 代理

```json
{
  "registry-mirrors": [
    "https://dockerproxy.com"
  ]
}
```

### 配置 3：使用阿里云（最稳定）

```json
{
  "registry-mirrors": [
    "https://您的专属地址.mirror.aliyuncs.com"
  ]
}
```

## 🔍 验证步骤

```bash
# 1. 检查配置
cat /etc/docker/daemon.json

# 2. 验证镜像源
docker info | grep -A 5 "Registry Mirrors"

# 3. 测试拉取
docker pull hello-world

# 4. 如果成功，清理缓存并构建
docker builder prune -a
cd /opt/colorful-tibet
docker compose build
```

## ⚠️ 注意事项

1. **DNS 问题**：如果所有镜像源都无法解析，检查 DNS 配置
2. **网络问题**：确保服务器可以访问外网
3. **镜像源选择**：优先使用阿里云镜像加速器（最稳定）

## 🚀 一键修复（推荐配置）

```bash
# 使用网易和百度云镜像源
sudo mkdir -p /etc/docker
echo '{
  "registry-mirrors": [
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}' | sudo tee /etc/docker/daemon.json

sudo systemctl restart docker

# 验证
docker info | grep "Registry Mirrors"
docker pull hello-world
```

