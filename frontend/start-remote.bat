@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion

if "%REMOTE_BACKEND%"=="" (
    if not "%~1"=="" set "REMOTE_BACKEND=%~1"
)

if "%REMOTE_BACKEND%"=="" (
    echo [ERROR] REMOTE_BACKEND is required.
    echo Set REMOTE_BACKEND to an HTTPS backend, for example:
    echo   set REMOTE_BACKEND=https://api.example.com
    echo Or use a local secure tunnel and point to localhost HTTP:
    echo   set REMOTE_BACKEND=http://localhost:6000
    pause
    exit /b 1
)

if /I "!REMOTE_BACKEND:~0,7!"=="http://" (
    set "REMOTE_BACKEND_LOCAL_HTTP="
    if /I "!REMOTE_BACKEND:~0,12!"=="http://[::1]" set "REMOTE_BACKEND_LOCAL_HTTP=1"
    if not defined REMOTE_BACKEND_LOCAL_HTTP (
        set "REMOTE_BACKEND_AFTER_SCHEME=!REMOTE_BACKEND:~7!"
        for /f "tokens=1 delims=/:" %%H in ("!REMOTE_BACKEND_AFTER_SCHEME!") do set "REMOTE_BACKEND_HTTP_HOST=%%H"
        if /I "!REMOTE_BACKEND_HTTP_HOST!"=="localhost" set "REMOTE_BACKEND_LOCAL_HTTP=1"
        if /I "!REMOTE_BACKEND_HTTP_HOST!"=="127.0.0.1" set "REMOTE_BACKEND_LOCAL_HTTP=1"
    )
    if not defined REMOTE_BACKEND_LOCAL_HTTP (
        echo [ERROR] Plain HTTP remote backends are refused: !REMOTE_BACKEND!
        echo Use HTTPS, or expose the backend through a localhost tunnel first.
        pause
        exit /b 1
    )
)

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
