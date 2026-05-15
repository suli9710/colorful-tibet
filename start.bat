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
set "START_MODE=docker"

if /I "%~1"=="docker" set "START_MODE=docker"
if /I "%~1"=="--docker" set "START_MODE=docker"
if /I "%~1"=="local" set "START_MODE=local"
if /I "%~1"=="native" set "START_MODE=local"
if /I "%~1"=="--local" set "START_MODE=local"
if /I "%~1"=="--native" set "START_MODE=local"

call :load_env_file "%ROOT%.env"
call :load_env_file "%BACKEND_DIR%\.env"

if not defined AI_MODEL set "AI_MODEL=doubao-seed-2-0-pro-260215"
if not defined MYSQL_HOST set "MYSQL_HOST=localhost"
if not defined MYSQL_PORT set "MYSQL_PORT=3306"
if not defined MYSQL_DATABASE set "MYSQL_DATABASE=colorful_tibet"
if not defined MYSQL_USERNAME set "MYSQL_USERNAME=root"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1

echo ========================================
echo   Colorful Tibet - One Click Startup
echo ========================================
echo Mode: %START_MODE%
echo.

if /I "%START_MODE%"=="docker" (
    call :start_docker_stack
    if errorlevel 1 goto :failed
    goto :success
)

net session >nul 2>&1
if errorlevel 1 (
    echo [INFO] Administrator privileges are required to restart MySQL services.
    echo [INFO] Requesting administrator permission...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b 0
)

call :require_command java "Java 17+"
if errorlevel 1 goto :failed

call :require_command mvn "Maven"
if errorlevel 1 goto :failed

call :require_command node "Node.js"
if errorlevel 1 goto :failed

call :require_command npm "npm"
if errorlevel 1 goto :failed

echo [INFO] Checking MySQL...
call :restart_mysql
if errorlevel 1 goto :failed
call :check_mysql_config
if errorlevel 1 goto :failed

echo [INFO] Restarting Redis...
call :stop_redis_if_running
call :start_redis

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

echo [INFO] Restarting backend service...
call :stop_port_process 8080 "backend"
if errorlevel 1 goto :failed
pushd "%BACKEND_DIR%"
start "Colorful Tibet Backend" cmd /k "set AI_MODEL=%AI_MODEL%&& set MYSQL_HOST=%MYSQL_HOST%&& set MYSQL_PORT=%MYSQL_PORT%&& set MYSQL_DATABASE=%MYSQL_DATABASE%&& set MYSQL_USERNAME=%MYSQL_USERNAME%&& set MYSQL_PASSWORD=%MYSQL_PASSWORD%&& set JWT_SECRET=%JWT_SECRET%&& set DOUBAO_API_KEY=%DOUBAO_API_KEY%&& set ARK_API_KEY=%ARK_API_KEY%&& mvn spring-boot:run"
popd
call :wait_for_http "%BACKEND_URL%/api/spots/heatmap?limit=1" 90 "backend"
if errorlevel 1 goto :failed

echo [INFO] Restarting frontend service...
call :stop_port_process 5173 "frontend"
if errorlevel 1 goto :failed
pushd "%FRONTEND_DIR%"
start "Colorful Tibet Frontend" cmd /k "npm run dev -- --host 0.0.0.0 --port 5173"
popd
call :wait_for_http "%FRONTEND_URL%" 60 "frontend"
if errorlevel 1 goto :failed

:success
echo [INFO] Opening browser...
start "" "%FRONTEND_URL%"

echo.
echo ========================================
echo   Startup complete
echo ========================================
echo Frontend: %FRONTEND_URL%
echo Backend:  %BACKEND_URL%
echo.
if /I "%START_MODE%"=="docker" (
    echo Stop services with: "%DOCKER_CLI%" compose -f "%ROOT%docker-compose.yml" down
) else (
    echo Close the Backend/Frontend terminal windows to stop services.
)
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

:load_env_file
set "ENV_FILE=%~1"
if not exist "%ENV_FILE%" exit /b 0
echo [INFO] Loading local environment file.
for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    set "ENV_KEY=%%A"
    set "ENV_VALUE=%%B"
    if not "!ENV_KEY!"=="" if not "!ENV_KEY:~0,1!"=="#" (
        set "!ENV_KEY!=!ENV_VALUE!"
    )
)
exit /b 0

:find_docker
set "DOCKER_CLI="
for /f "delims=" %%D in ('where docker.exe 2^>nul') do (
    if not defined DOCKER_CLI set "DOCKER_CLI=%%D"
)
if not defined DOCKER_CLI if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" set "DOCKER_CLI=%ProgramFiles%\Docker\Docker\resources\bin\docker.exe"
if not defined DOCKER_CLI if exist "%ProgramFiles(x86)%\Docker\Docker\resources\bin\docker.exe" set "DOCKER_CLI=%ProgramFiles(x86)%\Docker\Docker\resources\bin\docker.exe"
if not defined DOCKER_CLI (
    echo [ERROR] Docker CLI was not found. Please install Docker Desktop first.
    exit /b 1
)
"%DOCKER_CLI%" --version
if errorlevel 1 exit /b 1
"%DOCKER_CLI%" compose version
if errorlevel 1 (
    echo [ERROR] Docker Compose plugin is not available.
    exit /b 1
)
exit /b 0

:ensure_docker_engine
set "DOCKER_STATUS_FILE=%TEMP%\colorful-tibet-docker-status.txt"
"%DOCKER_CLI%" desktop status > "%DOCKER_STATUS_FILE%" 2>nul
findstr /I /R /C:"Status .*running" "%DOCKER_STATUS_FILE%" >nul 2>&1
if not errorlevel 1 (
    echo [INFO] Docker Desktop Engine is running.
    exit /b 0
)

echo [INFO] Docker Desktop Engine is not ready. Starting Docker Desktop...
"%DOCKER_CLI%" desktop start >nul 2>&1
if errorlevel 1 (
    if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
        start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
    )
)

for /L %%i in (1,1,90) do (
    "%DOCKER_CLI%" desktop status > "%DOCKER_STATUS_FILE%" 2>nul
    findstr /I /R /C:"Status .*running" "%DOCKER_STATUS_FILE%" >nul 2>&1
    if not errorlevel 1 (
        echo [INFO] Docker Desktop Engine is running.
        exit /b 0
    )
    timeout /t 2 /nobreak >nul
)

echo [ERROR] Docker Desktop is installed, but the Engine is still not running.
echo [ERROR] Open Docker Desktop and finish its setup. If prompted, enable WSL 2 and CPU virtualization.
echo [ERROR] Current Docker Desktop status:
type "%DOCKER_STATUS_FILE%"
exit /b 1

:start_docker_stack
call :find_docker
if errorlevel 1 exit /b 1

if not defined MYSQL_PASSWORD set "MYSQL_PASSWORD=root123456"
if not defined MYSQL_HOST_PORT set "MYSQL_HOST_PORT=3307"
if not defined REDIS_HOST_PORT set "REDIS_HOST_PORT=6380"
if not defined BACKEND_HOST_PORT set "BACKEND_HOST_PORT=8080"
if not defined FRONTEND_HOST_PORT set "FRONTEND_HOST_PORT=80"

set "BACKEND_URL=http://localhost:!BACKEND_HOST_PORT!"
if "!FRONTEND_HOST_PORT!"=="80" (
    set "FRONTEND_URL=http://localhost"
) else (
    set "FRONTEND_URL=http://localhost:!FRONTEND_HOST_PORT!"
)

echo [INFO] Docker CLI: !DOCKER_CLI!
call :ensure_docker_engine
if errorlevel 1 exit /b 1

echo [INFO] Validating Docker Compose configuration...
"!DOCKER_CLI!" compose -f "%ROOT%docker-compose.yml" config --quiet
if errorlevel 1 exit /b 1

echo [INFO] Restarting Docker Compose stack...
"!DOCKER_CLI!" compose -f "%ROOT%docker-compose.yml" down --remove-orphans
if errorlevel 1 exit /b 1
"!DOCKER_CLI!" compose -f "%ROOT%docker-compose.yml" up -d --build
if errorlevel 1 exit /b 1

call :wait_for_http "!BACKEND_URL!/actuator/health/readiness" 360 "docker backend"
if errorlevel 1 (
    "!DOCKER_CLI!" compose -f "%ROOT%docker-compose.yml" ps
    "!DOCKER_CLI!" compose -f "%ROOT%docker-compose.yml" logs --tail=80 backend
    exit /b 1
)
call :wait_for_http "!FRONTEND_URL!" 120 "docker frontend"
if errorlevel 1 (
    "!DOCKER_CLI!" compose -f "%ROOT%docker-compose.yml" ps
    "!DOCKER_CLI!" compose -f "%ROOT%docker-compose.yml" logs --tail=80 frontend
    exit /b 1
)
exit /b 0

:is_port_open
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-NetTCPConnection -State Listen -LocalPort %~1 -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
exit /b %errorlevel%

:wait_for_port
set "WAIT_PORT=%~1"
set "WAIT_SECONDS=%~2"
set "WAIT_NAME=%~3"
echo [INFO] Waiting for %WAIT_NAME% port %WAIT_PORT%...
for /L %%i in (1,1,%WAIT_SECONDS%) do (
    call :is_port_open %WAIT_PORT%
    if not errorlevel 1 (
        echo [INFO] %WAIT_NAME% port %WAIT_PORT% is ready.
        exit /b 0
    )
    timeout /t 1 /nobreak >nul
)
echo [ERROR] %WAIT_NAME% port %WAIT_PORT% did not become ready in %WAIT_SECONDS% seconds.
exit /b 1

:wait_for_port_closed
set "WAIT_PORT=%~1"
set "WAIT_SECONDS=%~2"
set "WAIT_NAME=%~3"
echo [INFO] Waiting for %WAIT_NAME% port %WAIT_PORT% to stop...
for /L %%i in (1,1,%WAIT_SECONDS%) do (
    call :is_port_open %WAIT_PORT%
    if errorlevel 1 (
        echo [INFO] %WAIT_NAME% port %WAIT_PORT% is free.
        exit /b 0
    )
    timeout /t 1 /nobreak >nul
)
echo [ERROR] %WAIT_NAME% port %WAIT_PORT% is still in use after %WAIT_SECONDS% seconds.
exit /b 1

:stop_port_process
set "STOP_PORT=%~1"
set "STOP_NAME=%~2"
call :is_port_open %STOP_PORT%
if errorlevel 1 (
    echo [INFO] %STOP_NAME% is not running on port %STOP_PORT%.
    exit /b 0
)
echo [INFO] %STOP_NAME% is running on port %STOP_PORT%; stopping owning process...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$port=%STOP_PORT%; $owners = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique; foreach ($owner in $owners) { try { $proc = Get-Process -Id $owner -ErrorAction Stop; Write-Host ('[INFO] Stopping process {0} (PID {1}) on port {2}' -f $proc.ProcessName, $owner, $port); Stop-Process -Id $owner -Force -ErrorAction Stop } catch { Write-Host ('[ERROR] Failed to stop PID {0}: {1}' -f $owner, $_.Exception.Message); exit 1 } }"
if errorlevel 1 exit /b 1
call :wait_for_port_closed %STOP_PORT% 20 "%STOP_NAME%"
exit /b %errorlevel%

:check_mysql_config
if not defined MYSQL_USERNAME set "MYSQL_USERNAME=root"
if not defined MYSQL_DATABASE set "MYSQL_DATABASE=colorful_tibet"
if not defined MYSQL_PASSWORD (
    echo [ERROR] MYSQL_PASSWORD is not set.
    echo [ERROR] Backend cannot connect to MySQL as !MYSQL_USERNAME! without a password.
    echo [ERROR] Add MYSQL_PASSWORD=your_mysql_password to %ROOT%.env or %BACKEND_DIR%\.env, then run start.bat again.
    exit /b 1
)
echo [INFO] MySQL config: database !MYSQL_DATABASE!, user !MYSQL_USERNAME!.
exit /b 0

:find_mysql_service
set "MYSQL_SERVICE="
for %%s in (MySQL84 MySQL80 MySQL mysql) do (
    sc query "%%~s" >nul 2>&1
    if not errorlevel 1 if "!MYSQL_SERVICE!"=="" set "MYSQL_SERVICE=%%~s"
)
exit /b 0

:restart_mysql
call :is_port_open 3306
if not errorlevel 1 (
    echo [INFO] MySQL is already listening on port 3306.
    exit /b 0
)

call :find_mysql_service
if "!MYSQL_SERVICE!"=="" (
    echo [ERROR] MySQL port 3306 is closed and no MySQL Windows service was found.
    echo [ERROR] Please install/start MySQL first.
    exit /b 1
)

echo [INFO] Starting MySQL service !MYSQL_SERVICE!...
net start "!MYSQL_SERVICE!" >nul 2>&1
if errorlevel 1 (
    echo [WARN] net start returned an error; checking whether MySQL became reachable...
)
call :wait_for_port 3306 60 "MySQL"
exit /b %errorlevel%

:find_redis
set "REDIS_PATH="
set "REDIS_DIR="
set "REDIS_CLI="
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
if not "!REDIS_DIR!"=="" if exist "!REDIS_DIR!redis-cli.exe" set "REDIS_CLI=!REDIS_DIR!redis-cli.exe"
exit /b 0

:stop_redis_if_running
call :is_port_open 6379
if errorlevel 1 (
    echo [INFO] Redis is not running on port 6379.
    exit /b 0
)
call :find_redis
if not "!REDIS_CLI!"=="" (
    echo [INFO] Redis is running; shutting it down gracefully...
    "!REDIS_CLI!" shutdown >nul 2>&1
    timeout /t 1 /nobreak >nul
)
call :is_port_open 6379
if not errorlevel 1 (
    call :stop_port_process 6379 "Redis"
    if errorlevel 1 exit /b 1
)
exit /b 0

:start_redis
call :find_redis
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
call :wait_for_port 6379 20 "Redis"
if errorlevel 1 (
    echo [WARN] Redis did not become ready. Backend can continue with local cache fallback.
)
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
