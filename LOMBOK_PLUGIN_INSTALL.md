# Lombok 插件安装指南

## IntelliJ IDEA 安装 Lombok 插件

### 方法一：通过插件市场安装（推荐）

1. **打开设置**
   - Windows/Linux: `File` → `Settings`
   - macOS: `IntelliJ IDEA` → `Preferences`

2. **进入插件管理**
   - 左侧菜单选择 `Plugins`

3. **搜索并安装**
   - 在搜索框输入 `Lombok`
   - 找到 `Lombok` 插件（作者：Michail Plushnikov）
   - 点击 `Install` 按钮
   - 等待安装完成

4. **重启 IDE**
   - 安装完成后会提示重启，点击 `Restart IDE`

5. **启用注解处理器**
   - 重启后，再次打开设置
   - 导航到：`Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
   - 勾选 `Enable annotation processing`
   - 点击 `Apply` 和 `OK`

### 方法二：手动安装

如果无法通过插件市场安装：

1. 访问 [Lombok 插件发布页面](https://github.com/mplushnikov/lombok-intellij-plugin/releases)
2. 下载与你的 IntelliJ IDEA 版本兼容的 `.zip` 文件
3. 在 IDEA 中：`File` → `Settings` → `Plugins` → 点击齿轮图标 → `Install Plugin from Disk...`
4. 选择下载的 `.zip` 文件
5. 重启 IDE 并启用注解处理器（参考方法一的第5步）

## Eclipse 安装 Lombok 插件

1. **下载 Lombok**
   - 访问 [Lombok 官网](https://projectlombok.org/download)
   - 下载 `lombok.jar` 文件

2. **运行安装程序**
   - 双击下载的 `lombok.jar` 文件
   - 选择你的 Eclipse 安装目录
   - 点击 `Install/Update` 按钮

3. **重启 Eclipse**
   - 安装完成后重启 Eclipse

4. **验证安装**
   - 在 Eclipse 中：`Help` → `About Eclipse IDE`
   - 应该能看到 Lombok 相关信息

## Visual Studio Code 安装 Lombok 插件（推荐）

### 步骤 1：安装必要的扩展

1. **安装 Java Extension Pack**
   - 按 `Ctrl+Shift+X` 打开扩展市场
   - 搜索 `Extension Pack for Java`（Microsoft 官方）
   - 点击 `Install` 安装（包含多个 Java 开发必需扩展）

2. **安装 Lombok 支持扩展**
   - 在扩展市场中搜索 `Lombok Annotations Support for VS Code`
   - 或搜索 `Lombok`，找到相关扩展
   - 点击 `Install` 安装

### 步骤 2：配置 VS Code 设置

项目已自动配置了 `.vscode/settings.json`，包含以下设置：
- `java.jdt.ls.lombokSupport.enabled: true` - 启用 Lombok 支持
- `java.configuration.updateBuildConfiguration: automatic` - 自动更新构建配置

### 步骤 3：重新加载窗口

1. 按 `Ctrl+Shift+P` 打开命令面板
2. 输入 `Java: Clean Java Language Server Workspace`
3. 选择该命令并确认清理
4. 重新加载窗口：`Ctrl+Shift+P` → `Developer: Reload Window`

### 步骤 4：验证安装

打开任意使用 Lombok 注解的实体类（如 `User.java`），应该：
- 没有红色错误提示
- 可以正常使用 `@Getter`、`@Setter` 等注解
- 代码补全能识别生成的 getter/setter 方法

## 验证安装

安装完成后，打开项目中的实体类（如 `User.java` 或 `Booking.java`），如果看到：
- 没有红色错误提示
- 可以正常使用 `@Getter`、`@Setter` 等注解
- IDE 能够识别生成的 getter/setter 方法

说明安装成功！

## 常见问题

### 问题1：安装后仍然报错
**解决方案：**
- 确保已启用注解处理器（IntelliJ IDEA）
- 尝试 `File` → `Invalidate Caches / Restart` → `Invalidate and Restart`
- 重新构建项目：`Build` → `Rebuild Project`

### 问题2：Maven 编译失败
**解决方案：**
- 确保 `pom.xml` 中已添加 Lombok 依赖
- 运行 `mvn clean install` 重新编译
- 检查 Java 版本是否兼容（需要 Java 17+）

### 问题3：插件版本不兼容
**解决方案：**
- 更新 IntelliJ IDEA 到最新版本
- 或下载与当前 IDEA 版本兼容的 Lombok 插件版本

