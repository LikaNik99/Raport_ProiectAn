@echo off
setlocal EnableDelayedExpansion
set "ROOT_DIR=%~dp0..\.."
echo ==================================================
echo SYSTEM BOOTSTRAP
echo ==================================================
REM ==================================================
REM START FRONTEND
REM ==================================================

echo Starting React frontend...
cd /d "%ROOT_DIR%\demo\frontend" || exit /b 1
start "React Frontend" cmd /k ^
set PORT=3000 ^&^& ^
set BROWSER=none ^&^& ^
npm start

call :wait_for_http http://localhost:3000 || exit /b 1

REM ==================================================
REM ENSURE MYSQL (PORT 3306)
REM ==================================================

echo Checking MySQL on port 3306...

call :is_port_listening 3306
if %ERRORLEVEL%==0 (
    echo MySQL already running. Skipping Docker startup.
) else (
    echo MySQL not detected. Starting Docker Compose...
    cd /d "%ROOT_DIR%\temp\Composes" || exit /b 1
    docker compose up -d || exit /b 1
    call :wait_for_port 3306 || exit /b 1
)

REM ---- MYSQL LOGS ----
cd /d "%ROOT_DIR%\temp\Composes"
start "MySQL Logs" cmd /k docker compose logs -f

REM ==================================================
REM START BACKEND
REM ==================================================

echo Starting Spring Boot backend...
cd /d "%ROOT_DIR%\demo" || exit /b 1 || exit /b 1
start "Spring Boot Backend" cmd /k mvn spring-boot:run -Dmaven.test.skip=true

call :wait_for_http http://localhost:8080/actuator/health || exit /b 1



REM ==================================================
REM GET LOCAL IP
REM ==================================================

for /f "tokens=2 delims=:" %%A in ('
    ipconfig ^| findstr /R /C:"IPv4 Address"
') do (
    set IP=%%A
    goto :ipfound
)


:ipfound
set IP=%IP:~1%

echo ==================================================
echo SYSTEM READY
echo Local:   http://localhost:3000
echo Network: http://%IP%:3000
echo ==================================================
exit /b 0


REM ==================================================
REM ================== FUNCTIONS ======================
REM ==================================================

:is_port_listening
set PORT=%1
netstat -an | find ":%PORT% " | find "LISTENING" >nul
exit /b %ERRORLEVEL%


:wait_for_port
set PORT=%1
set TIMEOUT=30

echo Waiting for port %PORT% ...

for /L %%i in (1,1,%TIMEOUT%) do (
    netstat -an | find ":%PORT% " | find "LISTENING" >nul
    if not errorlevel 1 (
        echo Port %PORT% is UP
        exit /b 0
    )
    timeout /t 1 >nul
)

echo ERROR: Port %PORT% not available
exit /b 1


:wait_for_http
set URL=%1
set TIMEOUT=30

echo Waiting for %URL% ...

for /L %%i in (1,1,%TIMEOUT%) do (
    curl -sf %URL% >nul
    if not errorlevel 1 (
        echo %URL% is READY
        exit /b 0
    )
    timeout /t 1 >nul
)

echo ERROR: %URL% not responding
exit /b 1