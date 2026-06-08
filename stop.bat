@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title Colorful Tibet - Stop Services

set "PREFERRED_WSL_DISTRO=Ubuntu"
set "DRY_RUN=0"

:parse_args
if "%~1"=="" goto after_args
if /I "%~1"=="--dry-run" (
    set "DRY_RUN=1"
    shift
    goto parse_args
)
if /I "%~1"=="--help" (
    set "USAGE_EXIT_CODE=0"
    goto usage
)
if /I "%~1"=="/?" (
    set "USAGE_EXIT_CODE=0"
    goto usage
)
echo Unknown argument: %~1
echo.
set "USAGE_EXIT_CODE=1"
goto usage

:after_args
echo ============================================
echo   Colorful Tibet - Stop All Services
echo ============================================
echo.

for %%I in ("%~dp0.") do set "PROJECT_DIR=%%~fI"
if not exist "%PROJECT_DIR%\docker-compose.yml" (
    echo Error: docker-compose.yml was not found under "%PROJECT_DIR%".
    call :wait_before_exit
    exit /b 1
)

call :select_wsl_distro || (
    call :wait_before_exit
    exit /b 1
)

call :convert_project_path || (
    call :wait_before_exit
    exit /b 1
)

echo Project directory: %PROJECT_DIR%
echo WSL distribution: %WSL_DISTRO%
echo WSL project path: %WSL_PROJECT_DIR%
echo.

if "%DRY_RUN%"=="1" (
    echo Dry run only. Command that would be executed:
    echo wsl.exe -d "%WSL_DISTRO%" --user root -- bash -lc "cd '%WSL_PROJECT_DIR%' && docker compose -f docker-compose.yml down"
    call :wait_before_exit
    exit /b 0
)

echo Stopping Docker Compose services...
wsl.exe -d "%WSL_DISTRO%" --user root -- bash -lc "cd '%WSL_PROJECT_DIR%' && docker compose -f docker-compose.yml down"
if not "!ERRORLEVEL!"=="0" (
    echo.
    echo Error: docker compose down failed.
    call :wait_before_exit
    exit /b 1
)

echo.
echo All services stopped.
call :wait_before_exit
exit /b 0

:usage
if not defined USAGE_EXIT_CODE set "USAGE_EXIT_CODE=1"
echo Usage: stop.bat [--dry-run]
echo.
echo Stops the local Docker Compose stack from this script's project directory.
echo Set COLORFUL_TIBET_WSL_DISTRO to choose a specific WSL distribution.
echo Set COLORFUL_TIBET_NO_PAUSE=1 to skip the final pause.
exit /b %USAGE_EXIT_CODE%

:select_wsl_distro
where wsl.exe >nul 2>nul
if errorlevel 1 (
    echo Error: wsl.exe was not found. Please install WSL2 first.
    exit /b 1
)

set "WSL_DISTRO="
set "PS_SELECT_WSL=$Preferred='%PREFERRED_WSL_DISTRO%'; $distros = & wsl.exe --list --quiet 2>$null | ForEach-Object { ($_ -replace [char]0, '').Trim() } | Where-Object { $_ -and $_ -notmatch '^docker-desktop' }; if ($env:COLORFUL_TIBET_WSL_DISTRO) { $env:COLORFUL_TIBET_WSL_DISTRO.Trim() } elseif ($distros -contains $Preferred) { $Preferred } elseif ($distros) { @($distros)[0] }"
for /f "usebackq tokens=* delims=" %%D in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "%PS_SELECT_WSL%"`) do (
    if not defined WSL_DISTRO set "WSL_DISTRO=%%D"
)

if not defined WSL_DISTRO (
    echo Error: no usable WSL distribution was found.
    exit /b 1
)

if not defined COLORFUL_TIBET_WSL_DISTRO if /I not "%WSL_DISTRO%"=="%PREFERRED_WSL_DISTRO%" (
    echo Preferred WSL distribution "%PREFERRED_WSL_DISTRO%" was not found; using "%WSL_DISTRO%".
)

call :test_wsl_distro
exit /b !ERRORLEVEL!

:test_wsl_distro
wsl.exe -d "%WSL_DISTRO%" -- bash -lc "printf WSL_READY" >nul 2>nul
if not "!ERRORLEVEL!"=="0" (
    echo Error: WSL distribution "%WSL_DISTRO%" could not be started.
    exit /b 1
)
exit /b 0

:convert_project_path
if not "%PROJECT_DIR:~1,1%"==":" (
    echo Error: only local drive paths can be converted to WSL paths: "%PROJECT_DIR%".
    exit /b 1
)

set "DRIVE_LETTER=%PROJECT_DIR:~0,1%"
set "PROJECT_REST=%PROJECT_DIR:~2%"
set "PROJECT_REST=%PROJECT_REST:\=/%"
call :lower_drive "%DRIVE_LETTER%"
set "WSL_PROJECT_DIR=/mnt/%LOWER_DRIVE%%PROJECT_REST%"
exit /b 0

:lower_drive
set "LOWER_DRIVE=%~1"
if /I "%~1"=="A" set "LOWER_DRIVE=a"
if /I "%~1"=="B" set "LOWER_DRIVE=b"
if /I "%~1"=="C" set "LOWER_DRIVE=c"
if /I "%~1"=="D" set "LOWER_DRIVE=d"
if /I "%~1"=="E" set "LOWER_DRIVE=e"
if /I "%~1"=="F" set "LOWER_DRIVE=f"
if /I "%~1"=="G" set "LOWER_DRIVE=g"
if /I "%~1"=="H" set "LOWER_DRIVE=h"
if /I "%~1"=="I" set "LOWER_DRIVE=i"
if /I "%~1"=="J" set "LOWER_DRIVE=j"
if /I "%~1"=="K" set "LOWER_DRIVE=k"
if /I "%~1"=="L" set "LOWER_DRIVE=l"
if /I "%~1"=="M" set "LOWER_DRIVE=m"
if /I "%~1"=="N" set "LOWER_DRIVE=n"
if /I "%~1"=="O" set "LOWER_DRIVE=o"
if /I "%~1"=="P" set "LOWER_DRIVE=p"
if /I "%~1"=="Q" set "LOWER_DRIVE=q"
if /I "%~1"=="R" set "LOWER_DRIVE=r"
if /I "%~1"=="S" set "LOWER_DRIVE=s"
if /I "%~1"=="T" set "LOWER_DRIVE=t"
if /I "%~1"=="U" set "LOWER_DRIVE=u"
if /I "%~1"=="V" set "LOWER_DRIVE=v"
if /I "%~1"=="W" set "LOWER_DRIVE=w"
if /I "%~1"=="X" set "LOWER_DRIVE=x"
if /I "%~1"=="Y" set "LOWER_DRIVE=y"
if /I "%~1"=="Z" set "LOWER_DRIVE=z"
exit /b 0

:wait_before_exit
if "%COLORFUL_TIBET_NO_PAUSE%"=="1" exit /b 0
pause
exit /b 0
