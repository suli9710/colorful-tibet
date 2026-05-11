@echo off
setlocal EnableExtensions EnableDelayedExpansion

chcp 65001 >nul

echo ========================================
echo   Colorful Tibet - Startup Script
echo ========================================
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java was not found. Please install Java 17 or later.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven was not found. Please install Maven.
    pause
    exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js was not found. Please install Node.js.
    pause
    exit /b 1
)

echo [INFO] Checking Redis...
set "REDIS_STARTED=0"
netstat -ano 2>nul | findstr ":6379 " >nul
if not errorlevel 1 (
    echo [INFO] Redis is already running on port 6379.
    set "REDIS_STARTED=1"
)

if %REDIS_STARTED%==0 (
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
    if not "!REDIS_PATH!"=="" (
        echo [INFO] Starting Redis from !REDIS_PATH!...
        if exist "!REDIS_DIR!redis.windows.conf" (
            start "" /D "!REDIS_DIR!" "!REDIS_PATH!" "!REDIS_DIR!redis.windows.conf"
        ) else (
            start "" /D "!REDIS_DIR!" "!REDIS_PATH!"
        )
        timeout /t 1 /nobreak >nul
    ) else (
        echo [WARN] redis-server.exe not found in common locations.
        echo [WARN] Please start Redis manually or install it from https://github.com/tporadowski/redis/releases
    )
)

set "REDIS_CLI="
set "REDIS_PING="
for %%p in (
    "%USERPROFILE%\Redis\redis-cli.exe"
    "C:\Program Files\Redis\redis-cli.exe"
    "C:\Program Files (x86)\Redis\redis-cli.exe"
    "redis-cli.exe"
) do (
    if exist %%p if "!REDIS_CLI!"=="" set "REDIS_CLI=%%~p"
)
if not "!REDIS_CLI!"=="" (
    for /f "delims=" %%r in ('"!REDIS_CLI!" -h 127.0.0.1 -p 6379 ping 2^>nul') do set "REDIS_PING=%%r"
    if /I "!REDIS_PING!"=="PONG" (
        echo [INFO] Redis ping OK.
    ) else (
        echo [WARN] Redis did not respond to ping. Backend will continue with local cache fallback.
    )
) else (
    echo [WARN] redis-cli.exe not found; skipping Redis ping.
)

setlocal DisableDelayedExpansion

echo [INFO] Starting backend service...
pushd "%~dp0backend"
set "AI_MODEL=doubao-seed-2-0-pro-260215"
if not defined ARK_API_KEY (
    echo [WARN] ARK_API_KEY is not set. AI route generation will not work.
    echo [WARN] Please set ARK_API_KEY environment variable before starting.
)
start "" cmd /k "set AI_MODEL=%AI_MODEL%&& set ARK_API_KEY=%ARK_API_KEY%&& mvn spring-boot:run"
popd

echo [INFO] Waiting for backend to start...
timeout /t 5 /nobreak >nul

echo [INFO] Starting frontend service...
pushd "%~dp0frontend"

if not exist "node_modules" (
    echo [INFO] node_modules not found, installing dependencies...
    call npm install
    if errorlevel 1 (
        echo [ERROR] Frontend dependency installation failed.
        popd
        pause
        exit /b 1
    )
)

start "" cmd /k "npm run dev"
popd

echo.
echo ========================================
echo   Startup complete!
echo ========================================
echo Backend: http://localhost:8080
echo Frontend: check the frontend terminal window for its URL
echo.
echo Tip: close the service windows to stop them
echo ========================================
pause
