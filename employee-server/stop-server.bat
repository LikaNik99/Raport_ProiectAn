@echo off
echo Stopping Server Logare...
powershell -Command "Get-CimInstance Win32_Process | Where-Object CommandLine -like '*server-logare*' | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }" >nul 2>&1
echo Server stopped!
timeout /t 2 >nul
