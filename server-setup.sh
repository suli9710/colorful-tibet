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

# 等待MySQL安装完成并启动服务
sleep 5

# 尝试启动MySQL服务（兼容不同的服务名）
if systemctl list-units --type=service | grep -q "mysql.service"; then
    systemctl start mysql
    systemctl enable mysql
elif systemctl list-units --type=service | grep -q "mysqld.service"; then
    systemctl start mysqld
    systemctl enable mysqld
else
    # 如果服务不存在，尝试直接启动mysqld
    service mysql start || service mysqld start || true
fi

# 等待MySQL完全启动
sleep 5

# 检查MySQL是否运行
if ! pgrep -x mysqld > /dev/null; then
    log_warn "MySQL服务未运行，尝试手动启动..."
    mysqld_safe --user=mysql &
    sleep 5
fi

# 生成随机密码
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32)
log_warn "MySQL root密码: $MYSQL_ROOT_PASSWORD"
log_warn "请保存此密码！"

# 安全配置MySQL（使用sudo mysql如果直接mysql失败）
if command -v mysql &> /dev/null; then
    # 尝试使用sudo mysql（Ubuntu 22.04默认配置）
    sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';" 2>/dev/null || \
    mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';" 2>/dev/null || \
    mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';"
    
    sudo mysql -e "DELETE FROM mysql.user WHERE User='';" 2>/dev/null || \
    mysql -u root -e "DELETE FROM mysql.user WHERE User='';" 2>/dev/null || \
    mysql -e "DELETE FROM mysql.user WHERE User='';"
    
    sudo mysql -e "DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');" 2>/dev/null || \
    mysql -u root -e "DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');" 2>/dev/null || \
    mysql -e "DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');"
    
    sudo mysql -e "DROP DATABASE IF EXISTS test;" 2>/dev/null || \
    mysql -u root -e "DROP DATABASE IF EXISTS test;" 2>/dev/null || \
    mysql -e "DROP DATABASE IF EXISTS test;"
    
    sudo mysql -e "DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';" 2>/dev/null || \
    mysql -u root -e "DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';" 2>/dev/null || \
    mysql -e "DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';"
    
    sudo mysql -e "FLUSH PRIVILEGES;" 2>/dev/null || \
    mysql -u root -e "FLUSH PRIVILEGES;" 2>/dev/null || \
    mysql -e "FLUSH PRIVILEGES;"
else
    log_error "MySQL未正确安装"
    exit 1
fi

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

