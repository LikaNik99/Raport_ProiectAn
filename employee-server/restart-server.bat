@echo off
echo Restarting Server Logare...
cd /d "%~dp0"

echo Stopping server...
powershell -Command "Get-CimInstance Win32_Process | Where-Object CommandLine -like '*server-logare*' | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }" >nul 2>&1
timeout /t 1 >nul

echo Starting server...
start "" "C:\Users\VOFF\.jdk\jdk-21.0.8\bin\java.exe" -jar target\server-logare-1.0-SNAPSHOT.jar
echo Server restarted!
timeout /t 2 >nul
