@echo off
setlocal EnableExtensions EnableDelayedExpansion

chcp 65001 >nul
title Colorful Tibet - One Click Startup

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "LOG_DIR=%ROOT%logs"
set "BACKEND_URL=http://localhost:8080"
set "FRONTEND_URL=http://localhost:5173"
set "AI_MODEL=doubao-seed-2-0-pro-260215"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1

echo ========================================
echo   Colorful Tibet - One Click Startup
echo ========================================
echo.

call :require_command java "Java 17+"
if errorlevel 1 goto :failed

call :require_command mvn "Maven"
if errorlevel 1 goto :failed

call :require_command node "Node.js"
if errorlevel 1 goto :failed

call :require_command npm "npm"
if errorlevel 1 goto :failed

echo [INFO] Checking MySQL on port 3306...
call :is_port_open 3306
if errorlevel 1 (
    call :try_start_windows_service MySQL80
    call :try_start_windows_service MySQL
    timeout /t 2 /nobreak >nul
)
call :is_port_open 3306
if errorlevel 1 (
    echo [ERROR] MySQL does not appear to be listening on port 3306.
    echo [ERROR] Please start MySQL, create database colorful_tibet, and set MYSQL_PASSWORD.
    goto :failed
) else (
    echo [INFO] MySQL is listening on port 3306.
)

echo [INFO] Checking Redis on port 6379...
call :is_port_open 6379
if errorlevel 1 (
    call :start_redis
) else (
    echo [INFO] Redis is already running.
)

echo [INFO] Preparing frontend dependencies...
pushd "%FRONTEND_DIR%"
if not exist "node_modules" (
    echo [INFO] node_modules not found, running npm install...
    call npm install
    if errorlevel 1 (
        popd
        echo [ERROR] Frontend dependency installation failed.
        goto :failed
    )
)
popd

echo [INFO] Checking backend on port 8080...
call :is_port_open 8080
if errorlevel 1 (
    echo [INFO] Starting backend service...
    pushd "%BACKEND_DIR%"
    start "Colorful Tibet Backend" cmd /k "set AI_MODEL=%AI_MODEL%&& mvn spring-boot:run"
    popd
    call :wait_for_http "%BACKEND_URL%/api/spots/heatmap?limit=1" 90 "backend"
    if errorlevel 1 goto :failed
) else (
    echo [INFO] Backend port 8080 is already in use. Reusing the running backend.
)

echo [INFO] Checking frontend on port 5173...
call :is_port_open 5173
if errorlevel 1 (
    echo [INFO] Starting frontend service...
    pushd "%FRONTEND_DIR%"
    start "Colorful Tibet Frontend" cmd /k "npm run dev -- --host 0.0.0.0 --port 5173"
    popd
    call :wait_for_http "%FRONTEND_URL%" 60 "frontend"
    if errorlevel 1 goto :failed
) else (
    echo [INFO] Frontend port 5173 is already in use. Reusing the running frontend.
)

echo [INFO] Opening browser...
start "" "%FRONTEND_URL%"

echo.
echo ========================================
echo   Startup complete
echo ========================================
echo Frontend: %FRONTEND_URL%
echo Backend:  %BACKEND_URL%
echo.
echo Close the Backend/Frontend terminal windows to stop services.
echo ========================================
pause
exit /b 0

:require_command
where %~1 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] %~2 was not found. Please install it first.
    exit /b 1
)
echo [INFO] %~2 found.
exit /b 0

:is_port_open
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-NetTCPConnection -State Listen -LocalPort %~1 -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
exit /b %errorlevel%

:try_start_windows_service
sc query "%~1" >nul 2>&1
if errorlevel 1 exit /b 0
echo [INFO] Trying to start Windows service %~1...
net start "%~1" >nul 2>&1
exit /b 0

:start_redis
set "REDIS_PATH="
set "REDIS_DIR="
for %%p in (
    "%USERPROFILE%\Redis\redis-server.exe"
    "C:\Program Files\Redis\redis-server.exe"
    "C:\Program Files (x86)\Redis\redis-server.exe"
    "redis-server.exe"
) do (
    if exist %%p if "!REDIS_PATH!"=="" (
        set "REDIS_PATH=%%~p"
        set "REDIS_DIR=%%~dpp"
    )
)

if "!REDIS_PATH!"=="" (
    echo [WARN] redis-server.exe not found. Backend can continue with local cache fallback.
    exit /b 0
)

echo [INFO] Starting Redis from !REDIS_PATH!...
if exist "!REDIS_DIR!redis.windows.conf" (
    start "Colorful Tibet Redis" /D "!REDIS_DIR!" "!REDIS_PATH!" "!REDIS_DIR!redis.windows.conf"
) else (
    start "Colorful Tibet Redis" /D "!REDIS_DIR!" "!REDIS_PATH!"
)
timeout /t 1 /nobreak >nul
exit /b 0

:wait_for_http
set "WAIT_URL=%~1"
set "WAIT_SECONDS=%~2"
set "WAIT_NAME=%~3"
echo [INFO] Waiting for %WAIT_NAME% to be ready...
for /L %%i in (1,1,%WAIT_SECONDS%) do (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 '%WAIT_URL%'; if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { exit 0 } } catch { exit 1 }" >nul 2>&1
    if not errorlevel 1 (
        echo [INFO] %WAIT_NAME% is ready.
        exit /b 0
    )
    timeout /t 1 /nobreak >nul
)
echo [ERROR] %WAIT_NAME% did not become ready in %WAIT_SECONDS% seconds.
exit /b 1

:failed
echo.
echo ========================================
echo   Startup failed
echo ========================================
echo Please check the terminal windows above for the detailed error.
echo ========================================
pause
exit /b 1
