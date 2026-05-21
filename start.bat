@echo off
chcp 65001 >nul
title Colorful Tibet - Start
pushd "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\start.ps1"
set "START_EXIT_CODE=%ERRORLEVEL%"
popd
if not "%START_EXIT_CODE%"=="0" (
    echo.
    echo Startup script failed. Please check the message above.
    pause
)
exit /b %START_EXIT_CODE%
