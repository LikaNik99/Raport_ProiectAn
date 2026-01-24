@echo off
REM ========================================
REM Client Logare - Launcher
REM ========================================

cd /d "%~dp0"

REM Verifică dacă JAR-ul există
if not exist "client-login.jar" (
    echo.
    echo ========================================
    echo  EROARE: client-login.jar nu exista!
    echo ========================================
    echo.
    echo Asigura-te ca fisierul client-login.jar
    echo este in acelasi folder cu acest script.
    echo.
    pause
    exit /b 1
)

REM Caută Java 21+ 
set "JAVA_CMD="

REM 1. Verifică JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javaw.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\javaw.exe"
        goto :found_java
    )
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        goto :found_java
    )
)

REM 2. Caută în locații comune
if exist "%USERPROFILE%\.jdk\jdk-21.0.8\bin\javaw.exe" (
    set "JAVA_CMD=%USERPROFILE%\.jdk\jdk-21.0.8\bin\javaw.exe"
    goto :found_java
)

if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.8-hotspot\bin\javaw.exe" (
    set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-21.0.8-hotspot\bin\javaw.exe"
    goto :found_java
)

if exist "C:\Program Files\Java\jdk-21\bin\javaw.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-21\bin\javaw.exe"
    goto :found_java
)

REM 3. Încearcă java din PATH (ultimă opțiune)
java -version >nul 2>&1
if not errorlevel 1 (
    set "JAVA_CMD=javaw"
    goto :found_java
)

REM Nu am găsit Java
echo.
echo ========================================
echo  EROARE: Java nu a fost gasit!
echo ========================================
echo.
echo Pentru a rula Client Logare, ai nevoie de Java 21 sau mai nou.
echo.
echo Download Java 21 de pe:
echo https://adoptium.net/temurin/releases/
echo.
echo Alege:
echo - Version: 21 (LTS)
echo - Operating System: Windows
echo - Architecture: x64
echo.
echo Dupa instalare, seteaza JAVA_HOME sau adauga Java la PATH.
echo.
pause
exit /b 1

:found_java
REM Pornește aplicația
echo.
echo ========================================
echo  Pornire Client Logare...
echo ========================================
echo.
echo Folosind: %JAVA_CMD%
echo.

start "" "%JAVA_CMD%" -jar client-login.jar

if errorlevel 1 (
    echo.
    echo ========================================
    echo  EROARE LA PORNIRE!
    echo ========================================
    echo.
    echo Aplicatia nu a pornit. Incercam cu consola pentru debug...
    echo.
    "%JAVA_CMD:javaw=java%" -jar client-login.jar
    echo.
    pause
    exit /b 1
)

exit /b 0
