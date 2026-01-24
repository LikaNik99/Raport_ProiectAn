@echo off
REM Server Logare Launcher for Windows
REM This batch file runs the Server Logare application

cd /d "%~dp0"

if not exist "target\server-logare-1.0-SNAPSHOT.jar" (
  echo Error: server-logare jar not found.
  echo Please run: mvn clean package -DskipTests
  pause
  exit /b 1
)

REM Setează variabilele de mediu pentru baza de date
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=logare_db
set DB_USER=postgres
set DB_PASS=postgres

echo Starting Server Logare on port 5000...
echo Database: %DB_NAME% on %DB_HOST%:%DB_PORT%
echo Server will run in the background. Check your firewall if connection issues occur.

start "" "C:\Users\VOFF\.jdk\jdk-21.0.8\bin\java.exe" -jar target\server-logare-1.0-SNAPSHOT.jar

exit /b 0
