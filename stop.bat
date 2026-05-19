@echo off
chcp 65001 >/dev/null
title Colorful Tibet - Stop Services

echo ============================================
echo   Colorful Tibet - Stop All Services
echo ============================================
echo.

set PROJECT_DIR=/mnt/c/Users/Suli/Desktop/colorful-tibet

echo Stopping Docker Compose services...
wsl -u root -d Ubuntu -- docker compose -f %PROJECT_DIR%/docker-compose.yml down

echo.
echo All services stopped.
pause
