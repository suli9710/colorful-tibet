# 西藏旅游网站完整部署教程

本教程将指导您从零开始，将整个项目部署到自己的服务器上。

## 📋 目录

1. [服务器选择](#1-服务器选择)
2. [服务器环境准备](#2-服务器环境准备)
3. [数据库配置](#3-数据库配置)
4. [后端部署](#4-后端部署)
5. [前端部署](#5-前端部署)
6. [Nginx配置](#6-nginx配置)
7. [域名和SSL配置](#7-域名和ssl配置)
8. [监控和维护](#8-监控和维护)
9. [常见问题](#9-常见问题)

---

## 1. 服务器选择

### 1.1 服务器配置推荐

根据项目需求，推荐以下配置：

**最低配置（适合测试/小规模使用）：**
- CPU: 2核
- 内存: 4GB
- 硬盘: 40GB SSD
- 带宽: 3Mbps
- 操作系统: Ubuntu 22.04 LTS 或 CentOS 7/8

**推荐配置（适合生产环境）：**
- CPU: 4核
- 内存: 8GB
- 硬盘: 80GB SSD
- 带宽: 5Mbps
- 操作系统: Ubuntu 22.04 LTS

**高性能配置（适合高并发）：**
- CPU: 8核
- 内存: 16GB
- 硬盘: 160GB SSD
- 带宽: 10Mbps
- 操作系统: Ubuntu 22.04 LTS

### 1.2 云服务器提供商推荐

#### 国内服务器（推荐）

1. **阿里云 ECS**
   - 优点：稳定可靠，国内访问速度快
   - 价格：约 200-500元/月（按配置）
   - 链接：https://www.aliyun.com/product/ecs

2. **腾讯云 CVM**
   - 优点：性价比高，新用户优惠多
   - 价格：约 180-450元/月（按配置）
   - 链接：https://cloud.tencent.com/product/cvm

3. **华为云 ECS**
   - 优点：企业级服务，安全性高
   - 价格：约 200-500元/月（按配置）
   - 链接：https://www.huaweicloud.com/product/ecs.html

#### 国外服务器（可选）

1. **AWS EC2**
   - 优点：全球覆盖，功能强大
   - 价格：按需付费

2. **DigitalOcean**
   - 优点：简单易用，价格透明
   - 价格：约 $24-48/月

### 1.3 购买服务器步骤

以阿里云为例：

1. 注册/登录阿里云账号
2. 进入 ECS 控制台
3. 点击"创建实例"
4. 选择配置：
   - 地域：选择离用户最近的地域
   - 实例规格：选择推荐配置
   - 镜像：Ubuntu 22.04 LTS
   - 存储：SSD云盘，40GB+
   - 网络：专有网络VPC
   - 安全组：开放 22(SSH)、80(HTTP)、443(HTTPS)、8080(后端) 端口
5. 设置root密码或SSH密钥
6. 完成购买

### 1.4 安全组配置

在云服务器控制台配置安全组规则，开放以下端口：

| 端口 | 协议 | 说明 | 来源 |
|------|------|------|------|
| 22 | TCP | SSH远程连接 | 0.0.0.0/0（建议限制为您的IP） |
| 80 | TCP | HTTP | 0.0.0.0/0 |
| 443 | TCP | HTTPS | 0.0.0.0/0 |
| 8080 | TCP | 后端API（仅内网） | 127.0.0.1 |

**注意**：8080端口只允许本地访问，通过Nginx反向代理暴露。

---

## 2. 服务器环境准备

### 2.1 连接到服务器

使用SSH连接到服务器：

```bash
ssh root@your-server-ip
```

如果是Windows系统，可以使用：
- **PuTTY**：https://www.putty.org/
- **Xshell**：https://www.netsarang.com/zh/xshell/
- **Windows Terminal**（Windows 10/11自带）

### 2.2 更新系统

```bash
# Ubuntu/Debian
apt update && apt upgrade -y

# CentOS/RHEL
yum update -y
```

### 2.3 安装Java 17

项目需要Java 17，安装步骤：

```bash
# Ubuntu/Debian
apt install -y openjdk-17-jdk

# CentOS/RHEL
yum install -y java-17-openjdk java-17-openjdk-devel

# 验证安装
java -version
```

应该看到类似输出：
```
openjdk version "17.0.x"
```

### 2.4 安装Maven

```bash
# Ubuntu/Debian
apt install -y maven

# CentOS/RHEL
yum install -y maven

# 验证安装
mvn -version
```

### 2.5 安装MySQL 8.0

```bash
# Ubuntu/Debian
apt install -y mysql-server

# CentOS/RHEL
yum install -y mysql-server

# 启动MySQL服务
systemctl start mysql
systemctl enable mysql

# 安全配置（设置root密码）
mysql_secure_installation
```

**安全配置选项：**
- 设置root密码：输入强密码
- 移除匿名用户：Y
- 禁止root远程登录：Y（可选，建议）
- 移除test数据库：Y
- 重新加载权限表：Y

### 2.6 安装Node.js和npm

前端需要Node.js 18+：

```bash
# 使用NodeSource安装Node.js 18
curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
apt install -y nodejs

# 验证安装
node -v
npm -v
```

### 2.7 安装Nginx

```bash
# Ubuntu/Debian
apt install -y nginx

# CentOS/RHEL
yum install -y nginx

# 启动Nginx
systemctl start nginx
systemctl enable nginx

# 验证安装
nginx -v
```

### 2.8 安装Git

```bash
# Ubuntu/Debian
apt install -y git

# CentOS/RHEL
yum install -y git

# 验证安装
git --version
```

### 2.9 创建项目目录

```bash
# 创建项目目录
mkdir -p /opt/colorful-tibet
cd /opt/colorful-tibet

# 创建必要的子目录
mkdir -p logs
mkdir -p uploads
```

### 2.10 安装PM2（可选，用于进程管理）

```bash
npm install -g pm2
```

---

## 3. 数据库配置

### 3.1 创建数据库和用户

```bash
# 登录MySQL
mysql -u root -p

# 在MySQL中执行以下命令
CREATE DATABASE tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tibet_user'@'localhost' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**重要**：请将 `your_strong_password` 替换为强密码。

### 3.2 配置MySQL远程访问（可选）

如果需要从其他服务器访问数据库：

```bash
# 编辑MySQL配置文件
nano /etc/mysql/mysql.conf.d/mysqld.cnf  # Ubuntu
# 或
nano /etc/my.cnf  # CentOS

# 找到 bind-address 行，修改为：
bind-address = 0.0.0.0

# 重启MySQL
systemctl restart mysql

# 创建远程用户（在MySQL中执行）
CREATE USER 'tibet_user'@'%' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'%';
FLUSH PRIVILEGES;
```

---

## 4. 后端部署

### 4.1 上传项目代码

**方法1：使用Git（推荐）**

```bash
cd /opt/colorful-tibet
git clone https://your-git-repo-url.git .
# 或
git clone https://your-git-repo-url.git backend
```

**方法2：使用SCP上传**

在本地电脑执行：

```bash
# Windows PowerShell
scp -r C:\Users\Suli\Desktop\colorful-tibet\backend root@your-server-ip:/opt/colorful-tibet/

# Linux/Mac
scp -r ~/Desktop/colorful-tibet/backend root@your-server-ip:/opt/colorful-tibet/
```

**方法3：使用FTP工具**

- FileZilla：https://filezilla-project.org/
- WinSCP：https://winscp.net/

### 4.2 配置后端应用

```bash
cd /opt/colorful-tibet/backend
```

编辑 `src/main/resources/application.yml`：

```bash
nano src/main/resources/application.yml
```

修改配置：

```yaml
server:
  port: 8080

spring:
  application:
    name: colorful-tibet-tourism
  mvc:
    async:
      request-timeout: 120000
  datasource:
    url: jdbc:mysql://localhost:3306/tibet_tourism?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: tibet_user
    password: your_strong_password  # 替换为实际密码
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: update  # 生产环境使用 update，不要用 create-drop
    show-sql: false  # 生产环境关闭SQL日志
    properties:
      hibernate:
        format_sql: false
  h2:
    console:
      enabled: false  # 生产环境关闭H2控制台

# 豆包API配置（请替换为您的实际配置）
doubao:
  api:
    url: https://ark.cn-beijing.volces.com/api/v3/chat/completions
    key: your-doubao-api-key  # 替换为实际API Key
    model: your-model-name  # 替换为实际模型名称

# 价格定时更新配置
price:
  update:
    enabled: true
    force: false
    cron: "0 0 2 * * ?"
    weekly:
      cron: "0 0 3 ? * MON"
    hourly:
      cron: "0 0 * * * ?"
```

### 4.3 编译打包后端

```bash
cd /opt/colorful-tibet/backend

# 清理并打包
mvn clean package -DskipTests

# 打包完成后，JAR文件在 target/ 目录
ls -lh target/*.jar
```

### 4.4 创建启动脚本

```bash
nano /opt/colorful-tibet/backend/start.sh
```

添加以下内容：

```bash
#!/bin/bash

# 项目目录
PROJECT_DIR="/opt/colorful-tibet/backend"
JAR_FILE="tourism-0.0.1-SNAPSHOT.jar"
LOG_FILE="/opt/colorful-tibet/logs/backend.log"
PID_FILE="/opt/colorful-tibet/logs/backend.pid"

cd $PROJECT_DIR

# 检查是否已运行
if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    if ps -p $PID > /dev/null 2>&1; then
        echo "后端服务已在运行 (PID: $PID)"
        exit 1
    fi
fi

# 启动服务
nohup java -jar \
    -Xms512m \
    -Xmx2048m \
    -Dspring.profiles.active=prod \
    target/$JAR_FILE \
    > $LOG_FILE 2>&1 &

# 保存PID
echo $! > $PID_FILE

echo "后端服务已启动 (PID: $(cat $PID_FILE))"
echo "日志文件: $LOG_FILE"
```

```bash
# 赋予执行权限
chmod +x /opt/colorful-tibet/backend/start.sh
```

### 4.5 创建停止脚本

```bash
nano /opt/colorful-tibet/backend/stop.sh
```

添加以下内容：

```bash
#!/bin/bash

PID_FILE="/opt/colorful-tibet/logs/backend.pid"

if [ ! -f $PID_FILE ]; then
    echo "后端服务未运行"
    exit 1
fi

PID=$(cat $PID_FILE)

if ! ps -p $PID > /dev/null 2>&1; then
    echo "后端服务未运行"
    rm -f $PID_FILE
    exit 1
fi

kill $PID
rm -f $PID_FILE

echo "后端服务已停止"
```

```bash
chmod +x /opt/colorful-tibet/backend/stop.sh
```

### 4.6 创建systemd服务（推荐）

```bash
nano /etc/systemd/system/tibet-backend.service
```

添加以下内容：

```ini
[Unit]
Description=Colorful Tibet Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/colorful-tibet/backend
ExecStart=/usr/bin/java -jar -Xms512m -Xmx2048m -Dspring.profiles.active=prod /opt/colorful-tibet/backend/target/tourism-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:/opt/colorful-tibet/logs/backend.log
StandardError=append:/opt/colorful-tibet/logs/backend.log

[Install]
WantedBy=multi-user.target
```

```bash
# 重新加载systemd配置
systemctl daemon-reload

# 启动服务
systemctl start tibet-backend

# 设置开机自启
systemctl enable tibet-backend

# 查看状态
systemctl status tibet-backend

# 查看日志
journalctl -u tibet-backend -f
```

### 4.7 验证后端运行

```bash
# 检查端口是否监听
netstat -tlnp | grep 8080

# 或使用
ss -tlnp | grep 8080

# 测试API
curl http://localhost:8080/api/spots
```

---

## 5. 前端部署

### 5.1 上传前端代码

```bash
# 如果使用Git
cd /opt/colorful-tibet
git clone https://your-git-repo-url.git frontend

# 或使用SCP上传
# 在本地执行：scp -r frontend root@your-server-ip:/opt/colorful-tibet/
```

### 5.2 安装依赖

```bash
cd /opt/colorful-tibet/frontend
npm install --production
```

### 5.3 配置前端API地址

编辑 `src/api/index.ts`：

```bash
nano src/api/index.ts
```

修改baseURL（如果需要）：

```typescript
const api = axios.create({
    baseURL: '/api',  // 使用相对路径，由Nginx代理
    timeout: 120000,
    headers: {
        'Content-Type': 'application/json'
    }
})
```

### 5.4 配置高德地图API

编辑 `index.html`：

```bash
nano index.html
```

确保配置了高德地图的API Key和安全密钥（参考 `高德地图配置说明.md`）。

### 5.5 构建前端

```bash
cd /opt/colorful-tibet/frontend

# 构建生产版本
npm run build

# 构建完成后，dist目录包含所有静态文件
ls -lh dist/
```

### 5.6 配置Nginx（见下一节）

前端静态文件将通过Nginx提供服务。

---

## 6. Nginx配置

### 6.1 创建Nginx配置文件

```bash
nano /etc/nginx/sites-available/colorful-tibet
```

添加以下配置：

```nginx
# 上游后端服务器
upstream backend {
    server 127.0.0.1:8080;
    keepalive 64;
}

# HTTP服务器（重定向到HTTPS）
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    
    # 重定向到HTTPS
    return 301 https://$server_name$request_uri;
}

# HTTPS服务器
server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;
    
    # SSL证书配置（见下一节）
    ssl_certificate /etc/nginx/ssl/your-domain.com.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;
    
    # SSL优化配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # 日志配置
    access_log /var/log/nginx/colorful-tibet-access.log;
    error_log /var/log/nginx/colorful-tibet-error.log;
    
    # 前端静态文件
    root /opt/colorful-tibet/frontend/dist;
    index index.html;
    
    # 前端路由（Vue Router）
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # 后端API代理
    location /api {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # 超时设置（适应AI生成时间）
        proxy_connect_timeout 120s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }
    
    # 图片资源代理
    location /images {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # 图片缓存
        expires 30d;
        add_header Cache-Control "public";
    }
    
    # 文件上传大小限制
    client_max_body_size 20M;
    
    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/json application/javascript;
}
```

**注意**：将 `your-domain.com` 替换为您的实际域名。

### 6.2 启用配置

```bash
# 创建符号链接
ln -s /etc/nginx/sites-available/colorful-tibet /etc/nginx/sites-enabled/

# 测试配置
nginx -t

# 如果测试通过，重载Nginx
systemctl reload nginx
```

### 6.3 如果没有域名（仅IP访问）

如果暂时没有域名，可以使用IP访问，修改配置：

```nginx
server {
    listen 80;
    server_name _;  # 匹配所有域名
    
    root /opt/colorful-tibet/frontend/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 120s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }
    
    location /images {
        proxy_pass http://backend;
        proxy_set_header Host $host;
    }
    
    client_max_body_size 20M;
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/json application/javascript;
}
```

---

## 7. 域名和SSL配置

### 7.1 购买域名

推荐域名注册商：
- 阿里云：https://wanwang.aliyun.com/
- 腾讯云：https://dnspod.cloud.tencent.com/
- GoDaddy：https://www.godaddy.com/

### 7.2 配置DNS解析

在域名管理后台添加A记录：

| 类型 | 主机记录 | 记录值 | TTL |
|------|----------|--------|-----|
| A | @ | 您的服务器IP | 600 |
| A | www | 您的服务器IP | 600 |

### 7.3 安装SSL证书（Let's Encrypt免费证书）

```bash
# 安装Certbot
apt install -y certbot python3-certbot-nginx

# 或 CentOS
yum install -y certbot python3-certbot-nginx

# 获取证书
certbot --nginx -d your-domain.com -d www.your-domain.com

# 按照提示操作：
# 1. 输入邮箱
# 2. 同意服务条款
# 3. 选择是否分享邮箱（可选）
# 4. Certbot会自动配置Nginx
```

### 7.4 自动续期证书

Let's Encrypt证书有效期90天，需要自动续期：

```bash
# 测试续期
certbot renew --dry-run

# 添加定时任务（自动续期）
crontab -e

# 添加以下行（每月1号凌晨3点检查续期）
0 3 1 * * certbot renew --quiet && systemctl reload nginx
```

### 7.5 使用自签名证书（仅测试用）

如果只是测试，可以使用自签名证书：

```bash
# 创建SSL目录
mkdir -p /etc/nginx/ssl

# 生成自签名证书
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/ssl/your-domain.com.key \
    -out /etc/nginx/ssl/your-domain.com.crt

# 按照提示输入信息
```

**注意**：自签名证书浏览器会显示警告，仅用于测试。

---

## 8. 监控和维护

### 8.1 日志管理

**后端日志：**
```bash
# 查看实时日志
tail -f /opt/colorful-tibet/logs/backend.log

# 或使用systemd
journalctl -u tibet-backend -f
```

**Nginx日志：**
```bash
# 访问日志
tail -f /var/log/nginx/colorful-tibet-access.log

# 错误日志
tail -f /var/log/nginx/colorful-tibet-error.log
```

### 8.2 日志轮转

创建日志轮转配置：

```bash
nano /etc/logrotate.d/colorful-tibet
```

添加：

```
/opt/colorful-tibet/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    notifempty
    missingok
    create 0644 root root
}
```

### 8.3 监控服务状态

```bash
# 检查后端服务
systemctl status tibet-backend

# 检查Nginx
systemctl status nginx

# 检查MySQL
systemctl status mysql

# 检查端口
netstat -tlnp | grep -E '8080|80|443|3306'
```

### 8.4 性能监控

安装htop：

```bash
apt install -y htop
htop
```

### 8.5 备份数据库

创建备份脚本：

```bash
nano /opt/colorful-tibet/backup-db.sh
```

添加：

```bash
#!/bin/bash

BACKUP_DIR="/opt/colorful-tibet/backups"
DB_NAME="tibet_tourism"
DB_USER="tibet_user"
DB_PASS="your_strong_password"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

mysqldump -u$DB_USER -p$DB_PASS $DB_NAME > $BACKUP_DIR/db_backup_$DATE.sql

# 压缩备份
gzip $BACKUP_DIR/db_backup_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "数据库备份完成: db_backup_$DATE.sql.gz"
```

```bash
chmod +x /opt/colorful-tibet/backup-db.sh

# 添加到定时任务（每天凌晨2点备份）
crontab -e
# 添加：0 2 * * * /opt/colorful-tibet/backup-db.sh
```

### 8.6 更新部署

**更新后端：**

```bash
cd /opt/colorful-tibet/backend

# 拉取最新代码
git pull

# 重新编译
mvn clean package -DskipTests

# 重启服务
systemctl restart tibet-backend
```

**更新前端：**

```bash
cd /opt/colorful-tibet/frontend

# 拉取最新代码
git pull

# 重新构建
npm run build

# 重载Nginx（无需重启）
systemctl reload nginx
```

---

## 9. 常见问题

### 9.1 后端无法启动

**检查Java版本：**
```bash
java -version  # 应该是17
```

**检查端口占用：**
```bash
netstat -tlnp | grep 8080
lsof -i :8080
```

**查看日志：**
```bash
tail -100 /opt/colorful-tibet/logs/backend.log
journalctl -u tibet-backend -n 100
```

### 9.2 数据库连接失败

**检查MySQL服务：**
```bash
systemctl status mysql
```

**测试连接：**
```bash
mysql -u tibet_user -p tibet_tourism
```

**检查配置文件：**
```bash
cat /opt/colorful-tibet/backend/src/main/resources/application.yml | grep datasource
```

### 9.3 前端404错误

**检查Nginx配置：**
```bash
nginx -t
```

**检查静态文件：**
```bash
ls -lh /opt/colorful-tibet/frontend/dist/
```

**检查Nginx错误日志：**
```bash
tail -f /var/log/nginx/colorful-tibet-error.log
```

### 9.4 API请求超时

**增加Nginx超时时间：**
在Nginx配置中已经设置了120秒超时，如果还不够，可以增加：

```nginx
proxy_connect_timeout 300s;
proxy_send_timeout 300s;
proxy_read_timeout 300s;
```

### 9.5 内存不足

**检查内存使用：**
```bash
free -h
```

**增加交换空间：**
```bash
# 创建2GB交换文件
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile

# 永久启用
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

### 9.6 权限问题

**检查文件权限：**
```bash
# 确保日志目录可写
chmod 755 /opt/colorful-tibet/logs
chown -R root:root /opt/colorful-tibet
```

---

## 10. 安全建议

### 10.1 防火墙配置

```bash
# Ubuntu使用ufw
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable

# CentOS使用firewalld
firewall-cmd --permanent --add-service=ssh
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload
```

### 10.2 禁用root SSH登录（推荐）

```bash
# 创建新用户
adduser deploy
usermod -aG sudo deploy

# 配置SSH密钥
su - deploy
mkdir -p ~/.ssh
chmod 700 ~/.ssh
# 将您的公钥添加到 authorized_keys

# 编辑SSH配置
nano /etc/ssh/sshd_config
# 设置：
# PermitRootLogin no
# PasswordAuthentication no

# 重启SSH
systemctl restart sshd
```

### 10.3 定期更新系统

```bash
# 设置自动更新（Ubuntu）
apt install -y unattended-upgrades
dpkg-reconfigure -plow unattended-upgrades
```

### 10.4 数据库安全

- 使用强密码
- 限制数据库用户权限
- 定期备份
- 不要将数据库暴露到公网

### 10.5 应用安全

- 定期更新依赖包
- 使用HTTPS
- 配置CORS策略
- 验证用户输入
- 使用JWT令牌过期时间

---

## 11. 部署检查清单

部署完成后，请检查以下项目：

- [ ] 服务器可以SSH连接
- [ ] Java 17已安装
- [ ] MySQL已安装并运行
- [ ] 数据库已创建
- [ ] 后端服务已启动（端口8080）
- [ ] 前端已构建（dist目录存在）
- [ ] Nginx已配置并运行
- [ ] 域名DNS已解析
- [ ] SSL证书已安装（如果使用HTTPS）
- [ ] 可以通过域名/IP访问网站
- [ ] API接口正常响应
- [ ] 数据库连接正常
- [ ] 日志文件正常生成
- [ ] 定时任务已配置
- [ ] 备份脚本已配置
- [ ] 防火墙已配置
- [ ] 安全组规则已配置

---

## 12. 快速部署脚本

为了方便部署，可以创建一个自动化脚本：

```bash
nano /opt/colorful-tibet/deploy.sh
```

```bash
#!/bin/bash

set -e

echo "开始部署..."

# 1. 更新代码
cd /opt/colorful-tibet/backend
git pull
cd /opt/colorful-tibet/frontend
git pull

# 2. 编译后端
echo "编译后端..."
cd /opt/colorful-tibet/backend
mvn clean package -DskipTests

# 3. 构建前端
echo "构建前端..."
cd /opt/colorful-tibet/frontend
npm install
npm run build

# 4. 重启后端
echo "重启后端服务..."
systemctl restart tibet-backend

# 5. 重载Nginx
echo "重载Nginx..."
systemctl reload nginx

echo "部署完成！"
```

```bash
chmod +x /opt/colorful-tibet/deploy.sh
```

使用：
```bash
/opt/colorful-tibet/deploy.sh
```

---

## 总结

完成以上步骤后，您的项目应该已经成功部署到服务器上。如果遇到问题，请参考"常见问题"部分，或查看相关日志文件。

**重要提示：**
1. 定期备份数据库
2. 监控服务器资源使用情况
3. 及时更新系统和依赖包
4. 保护好API密钥和数据库密码
5. 使用HTTPS保护数据传输

祝部署顺利！🎉






