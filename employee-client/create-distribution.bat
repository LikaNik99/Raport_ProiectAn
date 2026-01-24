@echo off
REM ========================================
REM Creare Pachet Distributie Client
REM ========================================

echo.
echo ========================================
echo  Creare Pachet de Distributie
echo ========================================
echo.

REM Verifică dacă JAR-ul este compilat
if not exist "target\client-login.jar" (
    echo [ERROR] client-login.jar nu exista in target\
    echo.
    echo Compileaza mai intai proiectul cu una dintre comenzile:
    echo   mvn clean package
    echo SAU
    echo   mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)

REM Verifică dacă launch-client.bat există
if not exist "launch-client.bat" (
    echo [ERROR] launch-client.bat nu exista!
    echo.
    echo Acest fisier este necesar pentru distributie.
    echo Asigura-te ca launch-client.bat exista in folder-ul curent.
    echo.
    pause
    exit /b 1
)

REM Creează directorul de distribuție
set DIST_DIR=distribution
if exist "%DIST_DIR%" (
    echo [INFO] Stergere director existent...
    rmdir /s /q "%DIST_DIR%"
)

echo [INFO] Creare director de distributie...
mkdir "%DIST_DIR%"

REM Copiază fișierele esențiale
echo [INFO] Copiere fisiere...
echo.

copy "target\client-login.jar" "%DIST_DIR%\client-login.jar" >nul
if errorlevel 1 (
    echo [ERROR] Eroare la copierea JAR-ului!
    pause
    exit /b 1
)
echo [OK] client-login.jar copiat

copy "launch-client.bat" "%DIST_DIR%\launch-client.bat" >nul
if errorlevel 1 (
    echo [ERROR] Eroare la copierea launch-client.bat!
    pause
    exit /b 1
)
echo [OK] launch-client.bat copiat

copy "CITESTE-MA.txt" "%DIST_DIR%\README.txt" >nul
if errorlevel 1 (
    echo [WARN] CITESTE-MA.txt nu a putut fi copiat
) else (
    echo [OK] README.txt copiat
)

REM Copiază documentația dacă există
if exist "docs\INSTALARE_PC_RETEA.md" (
    copy "docs\INSTALARE_PC_RETEA.md" "%DIST_DIR%\INSTALARE_PC_RETEA.md" >nul
    echo [OK] INSTALARE_PC_RETEA.md copiat
)

REM Copiază fișiere opționale dacă există
if exist "company_logo.png" (
    copy "company_logo.png" "%DIST_DIR%\company_logo.png" >nul
    echo [OK] company_logo.png copiat
)

if exist "company_name.txt" (
    copy "company_name.txt" "%DIST_DIR%\company_name.txt" >nul
    echo [OK] company_name.txt copiat
)

if exist "company_contact.txt" (
    copy "company_contact.txt" "%DIST_DIR%\company_contact.txt" >nul
    echo [OK] company_contact.txt copiat
)

echo.
echo ========================================
echo  Pachet creat cu succes!
echo ========================================
echo.
echo Locatie: %DIST_DIR%\
echo.
echo Continut:
dir /b "%DIST_DIR%"
echo.

REM Creează arhivă ZIP (dacă PowerShell e disponibil)
echo [INFO] Creare arhiva ZIP...
powershell -command "Compress-Archive -Path '%DIST_DIR%\*' -DestinationPath 'ClientLogare.zip' -Force" 2>nul

if exist "ClientLogare.zip" (
    for %%A in ("ClientLogare.zip") do set SIZE=%%~zA
    set /a SIZE_MB=!SIZE! / 1048576
    echo [OK] Arhiva ClientLogare.zip creata!
    echo.
    echo Dimensiune: ~18 MB
    echo.
    echo Poti trimite fisierul ClientLogare.zip pe alte PC-uri.
) else (
    echo [WARN] Nu s-a putut crea arhiva ZIP.
    echo Poti copia manual folderul %DIST_DIR%\
)

echo.
echo ========================================
echo  GATA!
echo ========================================
echo.
echo Pentru a instala pe alt PC:
echo.
echo 1. Copiaza fisierul ClientLogare.zip SAU folderul %DIST_DIR%
echo 2. Extrage pe PC-ul destinatie (daca ZIP)
echo 3. Asigura-te ca Java 21+ este instalat pe PC destinatie
echo 4. Ruleaza launch-client.bat
echo 5. In fereastra de login, introdu:
echo    - Host: IP-ul serverului (ex: 192.168.1.100)
echo    - Port: 5000
echo    - Click Connect, apoi Login
echo.

pause
