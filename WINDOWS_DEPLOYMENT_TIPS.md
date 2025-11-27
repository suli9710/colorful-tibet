# Windows用户部署指南

本文档为Windows用户提供额外的部署提示和工具推荐。

## 本地准备

### 1. SSH客户端工具

**推荐工具：**

1. **PuTTY**（最常用）
   - 下载：https://www.putty.org/
   - 免费、轻量、功能强大

2. **Xshell**（功能丰富）
   - 下载：https://www.netsarang.com/zh/xshell/
   - 个人版免费，界面友好

3. **Windows Terminal**（Windows 10/11自带）
   - 已集成SSH，无需安装
   - 使用：`ssh root@your-server-ip`

4. **MobaXterm**（全功能）
   - 下载：https://mobaxterm.mobatek.net/
   - 集成SSH、SFTP、X11等功能

### 2. 文件传输工具

**推荐工具：**

1. **WinSCP**（推荐）
   - 下载：https://winscp.net/
   - 图形化界面，支持SFTP/SCP
   - 可以同步本地和服务器文件

2. **FileZilla**
   - 下载：https://filezilla-project.org/
   - 免费开源，支持FTP/SFTP

3. **使用SCP命令（PowerShell）**
   ```powershell
   # 上传文件
   scp -r C:\Users\Suli\Desktop\colorful-tibet\backend root@your-server-ip:/opt/colorful-tibet/
   
   # 下载文件
   scp root@your-server-ip:/opt/colorful-tibet/logs/backend.log C:\Users\Suli\Desktop\
   ```

### 3. 代码编辑器

- **VS Code**：推荐，支持远程SSH扩展
- **Notepad++**：轻量级编辑器
- **Sublime Text**：功能强大

## 在Windows上准备部署文件

### 1. 修改文件权限（Linux脚本）

Windows上无法直接执行Linux脚本，需要：

**方法1：在服务器上创建脚本**
- 使用WinSCP上传脚本内容
- 在服务器上创建文件并设置权限

**方法2：使用Git Bash**
- 安装Git for Windows（包含Git Bash）
- 在Git Bash中可以执行部分Linux命令

### 2. 准备配置文件

在本地修改好配置文件后上传：

```powershell
# 1. 修改 application.yml（本地）
# 2. 使用WinSCP上传到服务器
# 3. 在服务器上验证配置
```

## 部署流程（Windows用户版）

### 步骤1：连接服务器

**使用PuTTY：**
1. 打开PuTTY
2. 输入服务器IP
3. 端口：22
4. 连接类型：SSH
5. 点击"Open"
6. 输入用户名：root
7. 输入密码

**使用Windows Terminal：**
```powershell
ssh root@your-server-ip
```

### 步骤2：上传项目文件

**使用WinSCP：**
1. 打开WinSCP
2. 新建会话：
   - 协议：SFTP
   - 主机名：your-server-ip
   - 用户名：root
   - 密码：your-password
3. 连接后，左侧是本地文件，右侧是服务器文件
4. 拖拽上传整个项目文件夹

**使用SCP命令：**
```powershell
# 在PowerShell中执行
scp -r C:\Users\Suli\Desktop\colorful-tibet root@your-server-ip:/opt/
```

### 步骤3：在服务器上执行命令

通过SSH连接后，按照 `DEPLOYMENT_GUIDE.md` 中的步骤执行命令。

### 步骤4：监控和日志查看

**使用WinSCP查看日志：**
1. 连接到服务器
2. 导航到 `/opt/colorful-tibet/logs/`
3. 右键点击日志文件 → 编辑（使用内置编辑器）

**使用PuTTY查看实时日志：**
```bash
# 在SSH会话中执行
tail -f /opt/colorful-tibet/logs/backend.log
```

## VS Code远程开发（推荐）

### 安装Remote-SSH扩展

1. 打开VS Code
2. 扩展市场搜索"Remote - SSH"
3. 安装Microsoft的Remote - SSH扩展

### 连接到服务器

1. 点击左下角绿色图标
2. 选择"Connect to Host"
3. 输入：`root@your-server-ip`
4. 输入密码
5. 在服务器上直接编辑文件

### 优势

- 直接在服务器上编辑代码
- 集成终端，方便执行命令
- 支持文件浏览器
- 可以安装扩展（如Java、Maven支持）

## 常见问题

### 1. 中文乱码

**PuTTY设置：**
- Window → Translation → Character set: UTF-8

**WinSCP设置：**
- 选项 → 首选项 → 编辑器 → 默认编码：UTF-8

### 2. 文件权限问题

在WinSCP中：
- 右键文件 → 属性 → 修改权限
- 或使用SSH终端执行 `chmod` 命令

### 3. 行尾符问题（CRLF vs LF）

Windows使用CRLF，Linux使用LF。可能导致脚本无法执行。

**解决方法：**
```bash
# 在服务器上转换
dos2unix script.sh
# 或
sed -i 's/\r$//' script.sh
```

**在VS Code中：**
- 右下角点击"CRLF" → 选择"LF"

### 4. 防火墙阻止连接

**检查Windows防火墙：**
- 控制面板 → Windows Defender防火墙
- 允许应用通过防火墙

**检查服务器防火墙：**
```bash
# 在服务器上
ufw status
ufw allow 22/tcp
```

## 推荐工作流程

1. **本地开发**：在Windows上使用VS Code开发
2. **代码管理**：使用Git管理代码
3. **部署**：
   - 推送到Git仓库
   - 在服务器上 `git pull`
   - 执行部署脚本
4. **监控**：使用VS Code Remote-SSH或WinSCP查看日志

## 快速命令参考

```powershell
# PowerShell中执行

# 1. 连接SSH
ssh root@your-server-ip

# 2. 上传文件
scp -r local_folder root@your-server-ip:/remote/path/

# 3. 下载文件
scp root@your-server-ip:/remote/file local_path

# 4. 执行远程命令
ssh root@your-server-ip "command"
```

## 工具下载链接汇总

- **PuTTY**: https://www.putty.org/
- **WinSCP**: https://winscp.net/
- **Xshell**: https://www.netsarang.com/zh/xshell/
- **FileZilla**: https://filezilla-project.org/
- **VS Code**: https://code.visualstudio.com/
- **Git for Windows**: https://git-scm.com/download/win

## 下一步

完成本地准备后，按照 `DEPLOYMENT_GUIDE.md` 进行服务器部署。






