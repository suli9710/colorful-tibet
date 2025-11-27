@echo off
chcp 65001 >nul
echo ========================================
echo   彩色西藏旅游网站 - 一键启动脚本
echo ========================================
echo.

:: 检查 Java 是否安装
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Java，请先安装 Java 17 或更高版本
    pause
    exit /b 1
)

:: 检查 Maven 是否安装
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Maven，请先安装 Maven
    pause
    exit /b 1
)

:: 检查 Node.js 是否安装
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Node.js，请先安装 Node.js
    pause
    exit /b 1
)

echo [信息] 正在启动后端服务...
cd backend
start "后端服务 - Spring Boot" cmd /k "mvn spring-boot:run"
cd ..

echo [信息] 等待后端服务启动...
timeout /t 5 /nobreak >nul

echo [信息] 正在启动前端服务...
cd frontend

:: 检查 node_modules 是否存在
if not exist "node_modules" (
    echo [信息] 检测到 node_modules 不存在，正在安装依赖...
    call npm install
)

start "前端服务 - Vite" cmd /k "npm run dev"
cd ..

echo.
echo ========================================
echo   启动完成！
echo ========================================
echo 后端服务: http://localhost:8080
echo 前端服务: 请查看前端服务窗口中的地址
echo.
echo 提示: 关闭服务窗口即可停止对应的服务
echo ========================================
pause

