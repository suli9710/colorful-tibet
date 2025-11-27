# 快速修复 MySQL 服务问题

## 问题：Unit mysql.service could not be found

### 立即解决方案（在服务器上执行）

```bash
# 1. 检查MySQL是否已安装
dpkg -l | grep mysql-server

# 2. 如果已安装，尝试启动（多种方式）
sudo service mysql start
# 或
sudo systemctl start mysql
# 或
sudo systemctl start mysqld

# 3. 检查MySQL是否运行
ps aux | grep mysql

# 4. 如果MySQL正在运行，继续执行脚本的后续步骤
# 或者手动配置MySQL
```

### 如果MySQL未安装或安装失败

```bash
# 完全重新安装MySQL
sudo apt remove --purge mysql-server mysql-client mysql-common
sudo apt autoremove
sudo apt update
sudo apt install -y mysql-server

# 等待几秒让MySQL初始化
sleep 5

# 启动MySQL
sudo service mysql start
sudo systemctl enable mysql

# 验证
sudo systemctl status mysql
```

### 手动配置MySQL（如果脚本的自动配置失败）

```bash
# 1. 确保MySQL运行
sudo systemctl start mysql

# 2. 使用sudo mysql登录（Ubuntu 22.04默认允许）
sudo mysql

# 3. 在MySQL命令行中执行（替换your_password为实际密码）
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'your_password';
FLUSH PRIVILEGES;
EXIT;

# 4. 创建项目数据库和用户
sudo mysql -u root -p
# 输入刚才设置的密码

CREATE DATABASE tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tibet_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 使用修复后的脚本

我已经更新了所有脚本，现在它们会自动处理MySQL服务启动问题。

**选项1：继续执行当前脚本**
```bash
# 如果脚本在MySQL部分失败，先手动启动MySQL
sudo service mysql start

# 然后继续执行脚本（跳过已安装的部分）
# 或者重新执行整个脚本
bash /tmp/server-setup.sh
```

**选项2：使用最新版本的脚本**
```bash
# 我已经修复了 server-setup.sh 和 腾讯云一键安装命令.txt
# 重新下载或复制最新版本的脚本执行
```

## 验证MySQL是否正常工作

```bash
# 检查服务状态
sudo systemctl status mysql

# 检查进程
ps aux | grep mysql

# 检查端口
sudo netstat -tlnp | grep 3306

# 测试连接
sudo mysql -u root -p
# 如果成功进入MySQL命令行，说明正常
```

## 常见问题

**Q: MySQL启动后立即停止？**
```bash
# 查看错误日志
sudo tail -50 /var/log/mysql/error.log
sudo journalctl -u mysql -n 50
```

**Q: 权限问题？**
```bash
sudo chown -R mysql:mysql /var/lib/mysql
sudo chmod -R 755 /var/lib/mysql
sudo systemctl restart mysql
```

**Q: 端口被占用？**
```bash
sudo lsof -i :3306
# 停止占用进程或修改MySQL配置
```

## 下一步

MySQL修复后，继续执行安装脚本的后续步骤，或参考 `DEPLOYMENT_GUIDE.md` 继续部署项目。

---

**提示**：我已经更新了所有脚本，新版本会自动处理这些问题。如果使用最新版本的脚本，应该不会再遇到这个问题。






