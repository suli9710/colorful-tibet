# 快速部署指南（简化版）

这是完整部署教程的简化版本，适合有经验的开发者快速参考。

## 前置条件检查清单

- [ ] 已购买云服务器（推荐：2核4G，Ubuntu 22.04）
- [ ] 已配置安全组（开放22, 80, 443端口）
- [ ] 已购买域名（可选，可用IP访问）
- [ ] 已获取SSH访问权限

## 一键安装环境（推荐）

```bash
# 在服务器上执行
wget https://your-repo-url/server-setup.sh
chmod +x server-setup.sh
sudo bash server-setup.sh
```

或手动安装：

```bash
# 更新系统
apt update && apt upgrade -y

# 安装必要软件
apt install -y openjdk-17-jdk maven mysql-server nodejs nginx git

# 配置MySQL
mysql_secure_installation
```

## 快速部署步骤

### 1. 上传代码

```bash
# 方法1: Git
cd /opt/colorful-tibet
git clone your-repo-url .

# 方法2: SCP（在本地执行）
scp -r backend frontend root@your-server-ip:/opt/colorful-tibet/
```

### 2. 配置数据库

```bash
mysql -u root -p
```

```sql
CREATE DATABASE tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tibet_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. 配置后端

编辑 `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tibet_tourism?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: tibet_user
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

### 4. 编译和启动后端

```bash
cd /opt/colorful-tibet/backend
mvn clean package -DskipTests
java -jar target/tourism-0.0.1-SNAPSHOT.jar
```

或使用systemd服务（推荐）：

```bash
# 创建服务文件
sudo nano /etc/systemd/system/tibet-backend.service
```

```ini
[Unit]
Description=Tibet Backend
After=mysql.service

[Service]
ExecStart=/usr/bin/java -jar /opt/colorful-tibet/backend/target/tourism-0.0.1-SNAPSHOT.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl start tibet-backend
sudo systemctl enable tibet-backend
```

### 5. 构建前端

```bash
cd /opt/colorful-tibet/frontend
npm install
npm run build
```

### 6. 配置Nginx

```bash
sudo nano /etc/nginx/sites-available/colorful-tibet
```

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /opt/colorful-tibet/frontend/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /images {
        proxy_pass http://127.0.0.1:8080;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/colorful-tibet /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 7. 配置SSL（可选）

```bash
sudo certbot --nginx -d your-domain.com
```

## 使用部署脚本

```bash
# 部署全部
./deploy.sh all

# 只部署后端
./deploy.sh backend

# 只部署前端
./deploy.sh frontend
```

## 验证部署

```bash
# 检查服务状态
systemctl status tibet-backend
systemctl status nginx
systemctl status mysql

# 测试API
curl http://localhost:8080/api/spots

# 检查端口
netstat -tlnp | grep -E '8080|80|443'
```

## 常见问题快速解决

**后端无法启动：**
```bash
# 查看日志
journalctl -u tibet-backend -n 50
tail -f /opt/colorful-tibet/logs/backend.log
```

**数据库连接失败：**
```bash
# 测试连接
mysql -u tibet_user -p tibet_tourism
```

**前端404：**
```bash
# 检查Nginx配置
sudo nginx -t
# 检查静态文件
ls -lh /opt/colorful-tibet/frontend/dist/
```

## 下一步

- 详细配置请参考 `DEPLOYMENT_GUIDE.md`
- 配置监控和备份
- 设置自动部署






