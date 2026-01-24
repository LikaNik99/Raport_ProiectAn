@echo off
echo Starting Server Logare...
cd /d "%~dp0"

REM Setează variabilele de mediu pentru baza de date
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=logare_db
set DB_USER=postgres
set DB_PASS=postgres

echo Config: %DB_NAME% on %DB_HOST%:%DB_PORT%

start "" "C:\Users\VOFF\.jdk\jdk-21.0.8\bin\java.exe" -jar target\server-logare-1.0-SNAPSHOT.jar
echo Server started!
timeout /t 2 >nul
