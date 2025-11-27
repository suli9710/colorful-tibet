# 修复 MySQL 配置问题

## 🔍 问题分析

看到错误：`ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: NO)`

这说明：
- ✅ MySQL 已安装并启动
- ✅ Root 密码已生成：`mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=`
- ❌ 但配置 MySQL 时出现权限问题

## ✅ 解决方案

### 方法1：手动配置MySQL（推荐）

在服务器终端执行以下命令：

```bash
# 1. 使用sudo mysql登录（Ubuntu 22.04默认允许）
sudo mysql

# 2. 在MySQL命令行中执行（替换YOUR_PASSWORD为上面显示的密码）
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=';
FLUSH PRIVILEGES;
EXIT;

# 3. 创建数据库和用户
mysql -u root -p
# 输入密码：mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=

# 4. 在MySQL中执行
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'your_db_password';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 方法2：继续执行脚本的后续步骤

如果MySQL配置失败，但其他软件已安装，可以：

1. **检查已安装的软件**
   ```bash
   java -version
   mvn -version
   node -v
   npm -v
   nginx -v
   ```

2. **手动完成MySQL配置**（使用方法1）

3. **继续执行脚本的其他部分**，或手动完成剩余配置

### 方法3：重新运行MySQL配置部分

```bash
# 设置MySQL root密码
MYSQL_ROOT_PASSWORD='mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs='

# 使用sudo mysql配置
sudo mysql <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';
FLUSH PRIVILEGES;
EOF

# 创建数据库
mysql -u root -p${MYSQL_ROOT_PASSWORD} <<EOF
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EOF
```

## 📝 重要信息

**请保存以下信息：**
- MySQL root密码：`mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=`
- 数据库名：`tibet_tourism`
- 数据库用户：`tibet_user`
- 数据库密码：需要您设置（或使用自动生成的）

## 🎯 推荐操作

1. **先使用方法1手动配置MySQL**（最简单直接）
2. **然后检查其他软件是否已安装**
3. **如果其他软件都已安装，可以继续部署项目**

## 检查安装状态

执行以下命令检查：

```bash
# 检查Java
java -version

# 检查Maven
mvn -version

# 检查Node.js
node -v
npm -v

# 检查MySQL
mysql --version
systemctl status mysql

# 检查Nginx
nginx -v
systemctl status nginx
```

如果这些都已安装，MySQL配置完成后就可以继续部署项目了！


