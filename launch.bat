@echo off
:: Double-click this file (or the Desktop "X Clone" shortcut).
:: It hands off to launch.ps1 which reliably starts server + client.
cd /d "%~dp0"
title X Clone Launcher

echo.
echo  Starting X Clone...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0launch.ps1"
set "ERR=%ERRORLEVEL%"

if not "%ERR%"=="0" (
    echo.
    echo  Launcher exited with an error.
    pause
)
exit /b %ERR%
