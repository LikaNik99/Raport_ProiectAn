@echo off
REM Database Management Script pentru server-logare (Windows)

echo === Database Management pentru Uzina Server ===
echo.

if "%1"=="" goto usage
if "%1"=="export" goto export
if "%1"=="import" goto import
if "%1"=="reset" goto reset
if "%1"=="check-admin" goto check_admin
if "%1"=="create-admin" goto create_admin
goto usage

:export
echo Exportare date din baza de date...
docker exec logare-postgres pg_dump -U postgres logare_db > backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql
echo Date exportate cu succes!
goto end

:import
if "%2"=="" (
    echo Eroare: Trebuie sa specifici fisierul SQL
    echo Folosire: db-manager.bat import backup_20231222.sql
    goto end
)
if not exist "%2" (
    echo Eroare: Fisierul %2 nu exista
    goto end
)
echo Importare date din %2...
docker exec -i logare-postgres psql -U postgres logare_db < "%2"
echo Date importate cu succes!
goto end

:reset
echo ATENTIE: Aceasta va sterge TOATE datele si va recrea baza de date!
set /p confirm="Esti sigur? (da/nu): "
if not "%confirm%"=="da" (
    echo Anulat
    goto end
)
echo Resetare baza de date...
docker-compose down -v
docker-compose up -d
echo Baza de date resetata cu date initiale!
goto end

:check_admin
echo Verificare user admin...
docker exec logare-postgres psql -U postgres logare_db -c "SELECT id, name, role FROM users WHERE role = 'ADMIN';"
goto end

:create_admin
echo Creare user admin de urgenta...
docker exec logare-postgres psql -U postgres logare_db -c "INSERT INTO users (id, name, role, password_hash) VALUES (999, 'Emergency Admin', 'ADMIN', 'admin123') ON CONFLICT (id) DO NOTHING;"
echo Admin creat: ID=999, Password=admin123
goto end

:usage
echo Folosire: db-manager.bat [comanda]
echo.
echo Comenzi disponibile:
echo   export         - Export toate datele (backup)
echo   import ^<file^>  - Import date dintr-un backup
echo   reset          - Reset baza de date la datele initiale
echo   check-admin    - Verifica daca exista useri admin
echo   create-admin   - Creeaza un admin de urgenta
echo.
echo Exemple:
echo   db-manager.bat export
echo   db-manager.bat import backup_20231222.sql
echo   db-manager.bat check-admin

:end
