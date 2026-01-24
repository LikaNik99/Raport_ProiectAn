@echo off
REM ========================================
REM Client Logare - Development Launcher
REM Acest script rulează clientul din target/
REM (pentru dezvoltare/testare)
REM ========================================

cd /d "%~dp0"

REM Verifică dacă există Java
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ========================================
    echo  EROARE: Java nu este instalat!
    echo ========================================
    echo.
    echo Pentru a rula Client Logare, ai nevoie de Java 21 sau mai nou.
    echo.
    echo Download Java 21 de pe:
    echo https://adoptium.net/temurin/releases/
    echo.
    pause
    exit /b 1
)

REM Verifică dacă JAR-ul este compilat
if not exist "target\client-login.jar" (
    echo.
    echo ========================================
    echo  EROARE: client-login.jar nu exista!
    echo ========================================
    echo.
    echo JAR-ul nu a fost compilat inca.
    echo.
    echo Ruleaza mai intai:
    echo   mvn clean package
    echo.
    echo SAU compileaza din VS Code:
    echo   Ctrl+Shift+P ^> Java: Compile Workspace
    echo.
    pause
    exit /b 1
)

REM Pornește aplicația din target/
echo.
echo ========================================
echo  Pornire Client Logare (Development)...
echo ========================================
echo.
echo Locatie: target\client-login.jar
echo.

start "" javaw -jar target\client-login.jar

REM Așteaptă 2 secunde
timeout /t 2 >nul

exit /b 0
