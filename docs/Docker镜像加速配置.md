# Docker 镜像加速配置

## ❌ 错误：i/o timeout

### 问题描述

执行 `docker compose build` 时出现错误：
```
failed to resolve source metadata for docker.io/library/maven:3.9-eclipse-temurin-17: 
failed to do request: Head "https://registry-1.docker.io/v2/library/maven/manifests/3.9-eclipse-temurin": 
i/o timeout
```

### 原因

- Docker Hub 访问速度慢或不稳定
- 网络连接超时
- 在中国大陆访问 Docker Hub 可能受限

### 解决方案：配置 Docker 镜像加速器

#### 步骤 1：创建或编辑 Docker 配置文件

```bash
sudo mkdir -p /etc/docker
sudo nano /etc/docker/daemon.json
```

#### 步骤 2：添加镜像加速器配置

在文件中添加以下内容（如果文件已存在，合并配置）：

```json
{
  "registry-mirrors": [
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://dockerproxy.com"
  ]
}
```

**常用国内镜像源**（按可用性排序）：
- 网易镜像：`https://hub-mirror.c.163.com` ✅ 推荐
- 百度云镜像：`https://mirror.baidubce.com` ✅ 推荐
- Docker 代理：`https://dockerproxy.com` ✅ 推荐
- 中科大镜像：`https://docker.mirrors.ustc.edu.cn` ⚠️ 可能无法访问
- 阿里云镜像：需要登录阿里云获取专属加速地址 ✅ 最稳定

#### 步骤 3：重启 Docker 服务

```bash
# 重新加载配置
sudo systemctl daemon-reload

# 重启 Docker
sudo systemctl restart docker

# 验证配置
docker info | grep -A 10 "Registry Mirrors"
```

#### 步骤 4：重新构建

```bash
cd /opt/colorful-tibet
docker compose build
```

## 🔧 其他解决方案

### 方案一：使用阿里云镜像加速器（推荐）

1. **登录阿里云**：https://cr.console.aliyun.com/
2. **获取加速地址**：容器镜像服务 → 镜像加速器
3. **配置加速器**：

```bash
sudo nano /etc/docker/daemon.json
```

添加您的专属加速地址：
```json
{
  "registry-mirrors": [
    "https://您的专属地址.mirror.aliyuncs.com"
  ]
}
```

### 方案二：使用腾讯云镜像加速器

```json
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com"
  ]
}
```

### 方案三：配置多个镜像源（推荐）

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://mirror.ccs.tencentyun.com"
  ]
}
```

## 🔍 验证配置

```bash
# 查看 Docker 信息
docker info

# 查看镜像加速器配置
docker info | grep -A 10 "Registry Mirrors"

# 测试拉取镜像
docker pull hello-world
```

## 📝 完整配置示例

```bash
# 1. 创建配置目录
sudo mkdir -p /etc/docker

# 2. 编辑配置文件
sudo nano /etc/docker/daemon.json

# 3. 添加以下内容
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}

# 4. 保存文件（Ctrl+O, Enter, Ctrl+X）

# 5. 重启 Docker
sudo systemctl daemon-reload
sudo systemctl restart docker

# 6. 验证配置
docker info | grep "Registry Mirrors"

# 7. 重新构建
cd /opt/colorful-tibet
docker compose build
```

## ⚠️ 注意事项

1. **配置文件格式**：必须是有效的 JSON 格式
2. **多个镜像源**：可以配置多个，Docker 会自动选择最快的
3. **重启服务**：修改配置后必须重启 Docker 服务
4. **网络问题**：如果仍然超时，检查服务器网络连接

## 🚀 配置后继续部署

配置完成后，重新执行：

```bash
cd /opt/colorful-tibet
docker compose build
docker compose up -d
```

## 📚 相关链接

- [Docker 官方文档 - 镜像加速器](https://docs.docker.com/config/daemon/registry-mirrors/)
- [阿里云容器镜像服务](https://cr.console.aliyun.com/)
- [中科大镜像站](https://mirrors.ustc.edu.cn/)

