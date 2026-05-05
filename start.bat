@echo off
setlocal EnableExtensions

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
