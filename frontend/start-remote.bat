@echo off
chcp 65001 >nul
setlocal

set "REMOTE_BACKEND=http://1.15.29.168:6000"

echo ========================================
echo   Frontend dev server - remote backend
echo   Remote backend: %REMOTE_BACKEND%
echo ========================================
echo.

cd /d "%~dp0"

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js was not found. Install Node.js first.
    pause
    exit /b 1
)

if not exist "node_modules" (
    echo [INFO] node_modules not found. Installing dependencies...
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed.
        pause
        exit /b 1
    )
)

echo [INFO] Starting Vite with a same-origin /api browser base.
echo [INFO] Vite proxy target: %REMOTE_BACKEND%
echo.

set "VITE_API_BASE_URL=/api"
set "VITE_DEV_PROXY_TARGET=%REMOTE_BACKEND%"
start "Frontend - Vite remote backend" cmd /k "npm run dev"

echo.
echo ========================================
echo   Started
echo ========================================
echo Frontend URL: see the Vite window.
echo Browser API base: /api
echo Vite proxy target: %REMOTE_BACKEND%
echo.
echo Close the Vite command window to stop the service.
echo ========================================
pause
