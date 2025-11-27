# 腾讯云服务器一键安装环境指南

本指南专门针对腾讯云服务器，提供详细的一键安装环境步骤。

## 📋 前置准备

### 1. 购买腾讯云服务器

1. 访问 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 进入 **云服务器 CVM** → **实例**
3. 点击 **新建** 创建实例

**推荐配置：**
- **地域**：选择离您最近的地域（如：北京、上海、广州）
- **机型**：标准型S5
- **CPU**：2核
- **内存**：4GB
- **系统盘**：50GB SSD云硬盘
- **镜像**：Ubuntu 22.04 LTS（推荐）或 Ubuntu 20.04 LTS
- **网络**：默认VPC
- **公网IP**：分配公网IP
- **带宽**：按量计费，3Mbps起

### 2. 配置安全组（重要！）

**必须开放以下端口：**

1. 在实例列表，点击实例ID进入详情
2. 点击 **安全组** → **修改规则**
3. 添加入站规则：

| 类型 | 协议 | 端口 | 来源 | 说明 |
|------|------|------|------|------|
| SSH | TCP | 22 | 0.0.0.0/0 | 远程连接（建议限制为您的IP） |
| HTTP | TCP | 80 | 0.0.0.0/0 | Web访问 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS访问 |
| 自定义 | TCP | 8080 | 127.0.0.1 | 后端API（仅本地） |

**注意**：8080端口只允许本地访问，通过Nginx反向代理。

### 3. 获取服务器信息

在实例列表中，记录：
- **公网IP**：用于SSH连接
- **用户名**：Ubuntu系统默认为 `ubuntu`（不是root）
- **密码**：在创建实例时设置的密码，或使用密钥

## 🚀 方法一：直接在线执行（推荐）

### 步骤1：连接服务器

**使用腾讯云Web Shell（最简单）：**

1. 在实例列表中，点击 **登录**
2. 选择 **标准登录方式**
3. 输入用户名：`ubuntu`
4. 输入密码
5. 点击 **登录**

**使用SSH客户端（Windows）：**

```powershell
# 在PowerShell或Windows Terminal中执行
ssh ubuntu@your-server-ip
```

**使用PuTTY：**
1. 打开PuTTY
2. Host Name: `your-server-ip`
3. Port: 22
4. Connection type: SSH
5. 点击 Open
6. 输入用户名：`ubuntu`
7. 输入密码

### 步骤2：切换到root用户

```bash
# 切换到root用户
sudo su -

# 或者使用sudo执行命令
```

### 步骤3：下载并执行安装脚本

**方法A：从GitHub/Gitee下载（如果有仓库）**

```bash
# 切换到root
sudo su -

# 下载脚本
wget https://raw.githubusercontent.com/your-repo/colorful-tibet/main/server-setup.sh

# 或者使用curl
curl -O https://raw.githubusercontent.com/your-repo/colorful-tibet/main/server-setup.sh

# 赋予执行权限
chmod +x server-setup.sh

# 执行安装
bash server-setup.sh
```

**方法B：直接在线创建脚本（推荐）**

```bash
# 切换到root
sudo su -

# 创建脚本文件
cat > /tmp/server-setup.sh << 'SCRIPT_EOF'
#!/bin/bash

# 服务器环境一键安装脚本
# 适用于 Ubuntu 22.04 LTS
# 使用方法: sudo bash server-setup.sh

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then 
    log_error "请使用root权限运行此脚本"
    exit 1
fi

log_info "========================================="
log_info "  服务器环境一键安装脚本"
log_info "========================================="

# 1. 更新系统
log_step "1. 更新系统包..."
apt update && apt upgrade -y

# 2. 安装基础工具
log_step "2. 安装基础工具..."
apt install -y curl wget git vim nano htop ufw

# 3. 安装Java 17
log_step "3. 安装Java 17..."
apt install -y openjdk-17-jdk
java -version
log_info "Java安装完成"

# 4. 安装Maven
log_step "4. 安装Maven..."
apt install -y maven
mvn -version
log_info "Maven安装完成"

# 5. 安装MySQL
log_step "5. 安装MySQL..."
apt install -y mysql-server
systemctl start mysql
systemctl enable mysql

# 生成随机密码
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32)
log_warn "MySQL root密码: $MYSQL_ROOT_PASSWORD"
log_warn "请保存此密码！"

# 安全配置MySQL
mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';"
mysql -e "DELETE FROM mysql.user WHERE User='';"
mysql -e "DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');"
mysql -e "DROP DATABASE IF EXISTS test;"
mysql -e "DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';"
mysql -e "FLUSH PRIVILEGES;"

log_info "MySQL安装完成"

# 6. 安装Node.js 18
log_step "6. 安装Node.js 18..."
curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
apt install -y nodejs
node -v
npm -v
log_info "Node.js安装完成"

# 7. 安装Nginx
log_step "7. 安装Nginx..."
apt install -y nginx
systemctl start nginx
systemctl enable nginx
log_info "Nginx安装完成"

# 8. 安装PM2（可选）
log_step "8. 安装PM2..."
npm install -g pm2
log_info "PM2安装完成"

# 9. 安装Certbot（SSL证书）
log_step "9. 安装Certbot..."
apt install -y certbot python3-certbot-nginx
log_info "Certbot安装完成"

# 10. 配置防火墙
log_step "10. 配置防火墙..."
ufw --force enable
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
log_info "防火墙配置完成"

# 11. 创建项目目录
log_step "11. 创建项目目录..."
mkdir -p /opt/colorful-tibet/{logs,backups,uploads}
log_info "项目目录创建完成"

# 12. 创建MySQL数据库和用户
log_step "12. 创建数据库..."
read -p "请输入数据库用户密码（留空将自动生成）: " DB_PASSWORD
if [ -z "$DB_PASSWORD" ]; then
    DB_PASSWORD=$(openssl rand -base64 16)
    log_warn "数据库用户密码: $DB_PASSWORD"
fi

mysql -u root -p${MYSQL_ROOT_PASSWORD} <<EOF
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EOF

log_info "数据库创建完成"

# 13. 保存配置信息
log_step "13. 保存配置信息..."
cat > /opt/colorful-tibet/server-info.txt <<EOF
=========================================
服务器环境安装完成
=========================================

MySQL root密码: ${MYSQL_ROOT_PASSWORD}
数据库用户: tibet_user
数据库密码: ${DB_PASSWORD}
数据库名: tibet_tourism

请妥善保管以上信息！

安装的软件版本:
- Java: $(java -version 2>&1 | head -n 1)
- Maven: $(mvn -version | head -n 1)
- Node.js: $(node -v)
- npm: $(npm -v)
- MySQL: $(mysql --version)
- Nginx: $(nginx -v 2>&1)

下一步:
1. 上传项目代码到 /opt/colorful-tibet/
2. 配置 application.yml
3. 编译并启动后端服务
4. 构建前端并配置Nginx
5. 配置域名和SSL证书

详细步骤请参考: DEPLOYMENT_GUIDE.md
=========================================
EOF

log_info "配置信息已保存到: /opt/colorful-tibet/server-info.txt"

log_info "========================================="
log_info "  安装完成！"
log_info "========================================="
log_warn "请查看 /opt/colorful-tibet/server-info.txt 获取重要信息"
log_info "下一步请参考 DEPLOYMENT_GUIDE.md 进行项目部署"
SCRIPT_EOF

# 赋予执行权限
chmod +x /tmp/server-setup.sh

# 执行安装
bash /tmp/server-setup.sh
```

**执行后：**
- 脚本会自动安装所有必要软件
- 安装过程约10-20分钟
- 安装完成后会显示数据库密码等信息

## 📤 方法二：上传脚本文件

### 步骤1：在本地准备脚本

确保 `server-setup.sh` 文件在本地。

### 步骤2：上传到服务器

**使用WinSCP（Windows推荐）：**

1. 下载并安装 [WinSCP](https://winscp.net/)
2. 新建会话：
   - 协议：SFTP
   - 主机名：您的服务器IP
   - 用户名：`ubuntu`
   - 密码：您的服务器密码
3. 连接后，将 `server-setup.sh` 拖拽到服务器 `/tmp/` 目录

**使用SCP命令（PowerShell）：**

```powershell
# 在PowerShell中执行
scp C:\Users\Suli\Desktop\colorful-tibet\server-setup.sh ubuntu@your-server-ip:/tmp/
```

**使用VS Code Remote-SSH：**

1. 安装 Remote-SSH 扩展
2. 连接到服务器
3. 在服务器上创建文件，粘贴脚本内容

### 步骤3：在服务器上执行

```bash
# SSH连接到服务器
ssh ubuntu@your-server-ip

# 切换到root
sudo su -

# 移动到脚本位置
cd /tmp

# 赋予执行权限
chmod +x server-setup.sh

# 执行安装
bash server-setup.sh
```

## ⚙️ 腾讯云特有配置

### 1. 腾讯云安全组 vs UFW防火墙

腾讯云有两层防火墙：
- **安全组**：在云控制台配置（必须配置）
- **UFW**：系统级防火墙（脚本会自动配置）

**建议**：
- 安全组：开放22, 80, 443端口
- UFW：脚本会自动配置，无需手动操作

### 2. 腾讯云内网访问

如果使用腾讯云其他服务（如云数据库），注意：
- 使用内网IP访问（更快、免费）
- 安全组需要允许内网访问

### 3. 腾讯云域名解析

如果使用腾讯云域名：
1. 进入 **云解析DNS**
2. 添加A记录指向服务器IP
3. 等待DNS生效（通常几分钟）

## 📝 安装后验证

### 检查安装的软件

```bash
# 检查Java
java -version
# 应该显示：openjdk version "17.0.x"

# 检查Maven
mvn -version
# 应该显示Maven版本信息

# 检查Node.js
node -v
npm -v
# 应该显示：v18.x.x 和版本号

# 检查MySQL
mysql --version
systemctl status mysql
# 应该显示运行中

# 检查Nginx
nginx -v
systemctl status nginx
# 应该显示运行中

# 检查防火墙
ufw status
# 应该显示：22, 80, 443端口已开放
```

### 查看安装信息

```bash
# 查看保存的配置信息
cat /opt/colorful-tibet/server-info.txt
```

**重要**：请保存显示的MySQL密码！

## 🔧 常见问题

### 1. 无法连接服务器

**检查项：**
- 安全组是否开放22端口
- 服务器是否运行中
- IP地址是否正确
- 用户名是否正确（Ubuntu系统是 `ubuntu`，不是 `root`）

### 2. 安装过程中断

```bash
# 重新执行脚本即可，已安装的软件不会重复安装
bash /tmp/server-setup.sh
```

### 3. MySQL密码忘记

```bash
# 查看保存的密码
cat /opt/colorful-tibet/server-info.txt

# 如果文件不存在，需要重置MySQL root密码
sudo mysql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'new_password';
FLUSH PRIVILEGES;
EXIT;
```

### 4. 端口被占用

```bash
# 检查端口占用
netstat -tlnp | grep -E '8080|80|443|3306'

# 如果80端口被占用，可能是Nginx已启动
# 这是正常的，可以继续
```

### 5. 网络连接慢

**使用国内镜像源（可选）：**

```bash
# 备份原配置
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak

# 使用阿里云镜像（Ubuntu 22.04）
sudo sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list
sudo sed -i 's/security.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list

# 更新
sudo apt update
```

## 🎯 下一步

安装完成后，按照以下步骤继续：

1. **查看安装信息**
   ```bash
   cat /opt/colorful-tibet/server-info.txt
   ```

2. **上传项目代码**
   - 参考 `DEPLOYMENT_GUIDE.md` 第4章

3. **配置项目**
   - 参考 `DEPLOYMENT_GUIDE.md` 第3-4章

4. **部署项目**
   - 参考 `DEPLOYMENT_GUIDE.md` 第4-5章

## 💡 快速命令参考

```bash
# 连接服务器
ssh ubuntu@your-server-ip

# 切换到root
sudo su -

# 查看服务状态
systemctl status mysql
systemctl status nginx

# 查看日志
tail -f /opt/colorful-tibet/logs/backend.log

# 重启服务
systemctl restart mysql
systemctl restart nginx
```

## 📞 获取帮助

- **腾讯云文档**：https://cloud.tencent.com/document/product/213
- **完整部署教程**：`DEPLOYMENT_GUIDE.md`
- **快速部署指南**：`DEPLOYMENT_QUICK_START.md`

---

**祝安装顺利！** 🎉

安装完成后，请妥善保存 `/opt/colorful-tibet/server-info.txt` 中的密码信息。






