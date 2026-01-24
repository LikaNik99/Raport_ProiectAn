@echo off
REM Script de actualizare automata pentru serverul de productie
REM Descarca ultimele modificari si aplica migrarea

echo ================================================
echo    ACTUALIZARE SERVER - Migrare Salariilor
echo ================================================
echo.

REM 1. Opreste serverul
echo [1/5] Oprire server...
call stop-server.bat
timeout /t 3 /nobreak >nul
echo.

REM 2. Descarca actualizarile
echo [2/5] Descarcare actualizari de pe GitHub...
git pull origin main
if %ERRORLEVEL% NEQ 0 (
    echo EROARE: Nu s-au putut descarca actualizarile!
    echo Verifica conexiunea la internet si GitHub.
    pause
    exit /b 1
)
echo.

REM 3. Recompileaza proiectul
echo [3/5] Recompilare proiect...
call mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo EROARE: Compilarea a esuat!
    pause
    exit /b 1
)
echo.

REM 4. Aplica migrarea
echo [4/5] Aplicare migrare baza de date...
call mvn exec:java "-Dexec.mainClass=md.uzina.server.MigrateSalarySchema" "-Dexec.cleanupDaemonThreads=false"
if %ERRORLEVEL% NEQ 0 (
    echo ATENTIE: Migrarea a esuat sau a fost deja aplicata!
    echo Continuam cu repornirea serverului...
)
echo.

REM 5. Reporneste serverul
echo [5/5] Repornire server...
call start-server.bat
echo.

echo ================================================
echo    ACTUALIZARE COMPLETA!
echo ================================================
echo.
echo Server actualizat si repornit cu succes!
echo Poti accesa acum modulul de Managementul Salariilor.
echo.
pause
