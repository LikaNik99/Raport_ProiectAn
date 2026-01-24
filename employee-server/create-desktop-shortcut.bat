@echo off
REM Create Desktop Shortcut for Client Logare
REM This script creates a Windows shortcut on the desktop to launch the client

setlocal enabledelayedexpansion

REM Get the project root directory (parent of current)
for %%A in ("%~dp0.") do set "PROJECT_ROOT=%%~dpA"
set PROJECT_ROOT=%PROJECT_ROOT:~0,-1%

REM Paths
set CLIENT_DIR=%PROJECT_ROOT%\client-login
set VBS_PATH=%CLIENT_DIR%\ClientLauncher.vbs
set DESKTOP=%USERPROFILE%\Desktop
set SHORTCUT_PATH=%DESKTOP%\Client Logare.lnk

REM Check if VBS exists
if not exist "%VBS_PATH%" (
  echo Error: ClientLauncher.vbs not found at:
  echo %VBS_PATH%
  pause
  exit /b 1
)

REM Create a PowerShell script to create the shortcut
set TEMP_PS=%TEMP%\create_logare_shortcut.ps1

(
  echo $shell = New-Object -ComObject WScript.Shell
  echo $shortcut = $shell.CreateShortcut("%SHORTCUT_PATH%")
  echo $shortcut.TargetPath = "wscript.exe"
  echo $shortcut.Arguments = Chr(34) ^& "%VBS_PATH%" ^& Chr(34)
  echo $shortcut.WorkingDirectory = "%CLIENT_DIR%"
  echo $shortcut.WindowStyle = 1
  echo $shortcut.IconLocation = "C:\Windows\System32\javaw.exe,0"
  echo $shortcut.Description = "Client Logare Angajati - Double-click to launch"
  echo $shortcut.Save()
  echo Write-Host "SUCCESS: Desktop shortcut created!" -ForegroundColor Green
  echo Write-Host "Location: %SHORTCUT_PATH%" -ForegroundColor Green
) > "%TEMP_PS%"

echo Creating shortcut...
powershell -NoProfile -ExecutionPolicy Bypass -File "%TEMP_PS%"

if %ERRORLEVEL% EQU 0 (
  echo.
  echo You can now double-click the "Client Logare" icon on your desktop to start the application.
  echo.
) else (
  echo.
  echo ERROR: Failed to create shortcut.
  echo Please check if you have permission to write to Desktop.
)

del /q "%TEMP_PS%" 2>nul
pause

