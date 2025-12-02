@echo off
chcp 65001 >nul
echo ========================================
echo   前端服务 - 远程模式启动
echo   (连接到远程后端: http://1.15.29.168:6000)
echo ========================================
echo.

cd /d %~dp0

:: 检查 Node.js 是否安装
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Node.js，请先安装 Node.js
    pause
    exit /b 1
)

:: 检查 node_modules 是否存在
if not exist "node_modules" (
    echo [信息] 检测到 node_modules 不存在，正在安装依赖...
    call npm install
)

echo [信息] 正在启动前端服务（远程模式）...
echo [提示] API地址: http://1.15.29.168:6000/api
echo.

:: 设置环境变量并启动
set VITE_API_BASE_URL=http://1.15.29.168:6000/api
start "前端服务 - Vite (远程模式)" cmd /k "npm run dev"

echo.
echo ========================================
echo   启动完成！
echo ========================================
echo 前端服务: 请查看前端服务窗口中的地址
echo 后端地址: http://1.15.29.168:6000/api
echo.
echo 提示: 关闭服务窗口即可停止服务
echo ========================================
pause








