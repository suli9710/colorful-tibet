# 修复 MySQL Socket 目录问题

## 🔍 问题分析

错误信息：
- `Directory '/var/run/mysqld' for UNIX socket file don't exists.`
- `ERROR 2002 (HY000): Can't connect to local MySQL server through socket`

这说明：
- MySQL socket 目录不存在
- MySQL 服务没有正常运行

## ✅ 解决方案

### 步骤1：创建 socket 目录

```bash
# 创建目录
sudo mkdir -p /var/run/mysqld

# 设置权限
sudo chown mysql:mysql /var/run/mysqld
sudo chmod 755 /var/run/mysqld
```

### 步骤2：停止所有 MySQL 进程

```bash
# 停止所有MySQL相关进程
sudo pkill mysqld
sudo pkill mysqld_safe

# 确保MySQL服务已停止
sudo systemctl stop mysql
```

### 步骤3：正常启动 MySQL

```bash
# 启动MySQL服务
sudo systemctl start mysql

# 检查状态
sudo systemctl status mysql

# 等待几秒让MySQL完全启动
sleep 5
```

### 步骤4：测试连接

```bash
# 尝试使用sudo mysql登录
sudo mysql
```

如果还是不行，继续下一步。

### 步骤5：重置 root 密码（如果需要）

```bash
# 停止MySQL
sudo systemctl stop mysql

# 创建socket目录（如果还没有）
sudo mkdir -p /var/run/mysqld
sudo chown mysql:mysql /var/run/mysqld

# 以安全模式启动
sudo mysqld_safe --skip-grant-tables --skip-networking &

# 等待启动
sleep 5

# 连接MySQL
mysql -u root
```

在 MySQL 中执行：

```sql
USE mysql;
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=';
FLUSH PRIVILEGES;
EXIT;
```

然后：

```bash
# 停止安全模式
sudo pkill mysqld
sudo pkill mysqld_safe

# 正常启动
sudo systemctl start mysql

# 测试登录
mysql -u root -p
# 输入密码：mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=
```

## 🎯 快速修复命令（一键执行）

复制以下命令一次性执行：

```bash
# 创建socket目录
sudo mkdir -p /var/run/mysqld
sudo chown mysql:mysql /var/run/mysqld
sudo chmod 755 /var/run/mysqld

# 停止所有MySQL进程
sudo pkill mysqld 2>/dev/null
sudo pkill mysqld_safe 2>/dev/null
sudo systemctl stop mysql 2>/dev/null

# 正常启动MySQL
sudo systemctl start mysql
sleep 5

# 检查状态
sudo systemctl status mysql

# 测试连接
sudo mysql
```

## 🔧 如果还是不行：完全重新安装

如果以上方法都不行，可以完全重新安装MySQL：

```bash
# 1. 完全卸载
sudo systemctl stop mysql
sudo apt remove --purge mysql-server mysql-client mysql-common mysql-server-core-* mysql-client-core-* -y
sudo apt autoremove -y
sudo apt autoclean
sudo rm -rf /var/lib/mysql
sudo rm -rf /etc/mysql
sudo rm -rf /var/run/mysqld

# 2. 重新安装
sudo apt update
sudo apt install -y mysql-server

# 3. 启动服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 4. 现在应该可以使用 sudo mysql 了
sudo mysql
```

## ✅ 验证

修复后，测试：

```bash
# 检查服务状态
sudo systemctl status mysql
# 应该显示：active (running)

# 测试连接
sudo mysql
# 应该能成功进入MySQL命令行
```

## 📝 创建数据库和用户

连接成功后，执行：

```sql
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

**建议**：先执行"快速修复命令"，如果不行再考虑重新安装。


