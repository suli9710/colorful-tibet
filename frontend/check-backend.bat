@echo off
chcp 65001 >nul
echo ========================================
echo   后端连接诊断工具
echo ========================================
echo.

echo [1/3] 检查本地后端 (http://localhost:8080)...
curl -s -o nul -w "状态码: %%{http_code}\n" http://localhost:8080/api/spots 2>nul
if %errorlevel% equ 0 (
    echo ✓ 本地后端连接成功！
) else (
    echo ✗ 本地后端连接失败
    echo   请确保后端正在运行: start.bat 或 cd backend ^&^& mvn spring-boot:run
)
echo.

echo [2/3] 检查远程后端 (http://1.15.29.168:6000)...
curl -s -o nul -w "状态码: %%{http_code}\n" http://1.15.29.168:6000/api/spots 2>nul
if %errorlevel% equ 0 (
    echo ✓ 远程后端连接成功！
) else (
    echo ✗ 远程后端连接失败
    echo   请检查 FRP 隧道是否正常运行
)
echo.

echo [3/3] 检查端口占用情况...
netstat -ano | findstr ":8080" >nul
if %errorlevel% equ 0 (
    echo ✓ 端口 8080 已被占用（可能是后端服务）
    netstat -ano | findstr ":8080"
) else (
    echo ✗ 端口 8080 未被占用（后端可能未运行）
)
echo.

echo ========================================
echo   诊断完成
echo ========================================
echo.
echo 提示：
echo - 如果本地后端未运行，请使用 start.bat 启动
echo - 如果使用远程后端，请使用 frontend\start-remote.bat 启动前端
echo - 如果使用本地后端，请使用 start.bat 或直接 npm run dev
echo.
pause



