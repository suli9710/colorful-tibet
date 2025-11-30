# DNS 和网络问题排查

## ❌ 错误：所有镜像源都无法解析

### 问题描述

多个镜像源都出现 DNS 解析失败：
```
dial tcp: lookup hub-mirror.c.163.com on 127.0.0.53:53: no such host
dial tcp: lookup docker.mirrors.ustc.edu.cn on 127.0.0.53:53: no such host
```

### 可能原因

1. DNS 服务器配置问题
2. 网络连接问题
3. 防火墙阻止 DNS 查询
4. 服务器 DNS 配置错误

## 🔍 排查步骤

### 步骤 1：测试 DNS 解析

```bash
# 测试 DNS 解析
nslookup hub-mirror.c.163.com
nslookup docker.io
nslookup baidu.com

# 如果都无法解析，说明 DNS 配置有问题
```

### 步骤 2：检查 DNS 配置

```bash
# 查看当前 DNS 配置
cat /etc/resolv.conf

# 应该看到类似内容：
# nameserver 8.8.8.8
# nameserver 114.114.114.114
```

### 步骤 3：测试网络连接

```bash
# 测试能否访问外网
ping -c 3 8.8.8.8
ping -c 3 baidu.com

# 测试 HTTPS 连接
curl -I https://www.baidu.com
```

### 步骤 4：修复 DNS 配置

如果 DNS 配置有问题，修复：

```bash
# 编辑 DNS 配置
sudo nano /etc/resolv.conf
```

添加可靠的 DNS 服务器：
```
nameserver 8.8.8.8
nameserver 8.8.4.4
nameserver 114.114.114.114
nameserver 223.5.5.5
```

保存后测试：
```bash
nslookup docker.io
```

## 🔧 解决方案

### 方案一：直接使用 Docker Hub（推荐）

如果网络可以直接访问 Docker Hub，移除所有镜像源：

```bash
# 编辑配置
sudo nano /etc/docker/daemon.json
```

**选项 1：清空镜像源**
```json
{
  "registry-mirrors": []
}
```

**选项 2：删除配置文件**
```bash
sudo rm /etc/docker/daemon.json
sudo systemctl restart docker
```

然后测试：
```bash
docker pull hello-world
```

### 方案二：使用腾讯云内网镜像（如果在腾讯云）

```bash
sudo nano /etc/docker/daemon.json
```

```json
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com"
  ]
}
```

```bash
sudo systemctl restart docker
docker pull hello-world
```

### 方案三：修复 DNS 后使用镜像源

```bash
# 1. 修复 DNS
sudo nano /etc/resolv.conf
```

添加：
```
nameserver 8.8.8.8
nameserver 114.114.114.114
```

```bash
# 2. 测试 DNS
nslookup hub-mirror.c.163.com

# 3. 如果成功，配置镜像源
sudo nano /etc/docker/daemon.json
```

```json
{
  "registry-mirrors": [
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

```bash
# 4. 重启 Docker
sudo systemctl restart docker

# 5. 测试
docker pull hello-world
```

### 方案四：使用代理（如果有）

如果服务器有代理，配置 Docker 使用代理：

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

## 🚀 快速修复（推荐：直接使用 Docker Hub）

```bash
# 1. 移除镜像源配置，直接使用 Docker Hub
sudo rm /etc/docker/daemon.json

# 或者保留文件但清空镜像源
echo '{
  "registry-mirrors": []
}' | sudo tee /etc/docker/daemon.json

# 2. 重启 Docker
sudo systemctl restart docker

# 3. 测试
docker pull hello-world

# 4. 如果成功，继续构建
cd /opt/colorful-tibet
docker compose build
```

## 🔍 完整诊断命令

```bash
# 1. 检查 DNS
cat /etc/resolv.conf
nslookup docker.io

# 2. 测试网络
ping -c 3 8.8.8.8
curl -I https://www.baidu.com

# 3. 检查 Docker 配置
cat /etc/docker/daemon.json
docker info | grep "Registry Mirrors"

# 4. 测试 Docker
docker pull hello-world
```

## ⚠️ 重要提示

1. **如果所有镜像源都无法解析**：建议直接使用 Docker Hub
2. **DNS 问题**：检查 `/etc/resolv.conf` 配置
3. **网络问题**：确保服务器可以访问外网
4. **腾讯云服务器**：可以使用腾讯云内网镜像源

## 📝 推荐操作

**如果 DNS 无法解析所有镜像源，最简单的方法是直接使用 Docker Hub：**

```bash
# 移除镜像源配置
sudo rm /etc/docker/daemon.json
sudo systemctl restart docker

# 测试
docker pull hello-world

# 如果成功，说明可以直接访问 Docker Hub，继续构建即可
cd /opt/colorful-tibet
docker compose build
```

