#!/bin/bash
# Database Management Script pentru server-logare

echo "=== Database Management pentru Uzina Server ==="
echo ""

# Funcție pentru export date
export_data() {
    echo "📦 Export date din baza de date..."
    docker exec uzina-postgres pg_dump -U postgres uzina_db > backup_$(date +%Y%m%d_%H%M%S).sql
    echo "✅ Backup creat: backup_$(date +%Y%m%d_%H%M%S).sql"
}

# Funcție pentru import date
import_data() {
    if [ -z "$1" ]; then
        echo "❌ Eroare: Trebuie să specifici fișierul SQL"
        echo "Folosire: ./db-manager.sh import backup_20231222.sql"
        exit 1
    fi
    
    if [ ! -f "$1" ]; then
        echo "❌ Eroare: Fișierul $1 nu există"
        exit 1
    fi
    
    echo "📥 Import date din $1..."
    docker exec -i uzina-postgres psql -U postgres uzina_db < "$1"
    echo "✅ Date importate cu succes"
}

# Funcție pentru reset la date inițiale
reset_data() {
    echo "⚠️  ATENȚIE: Aceasta va șterge TOATE datele și va recrea baza de date!"
    read -p "Ești sigur? (da/nu): " confirm
    
    if [ "$confirm" != "da" ]; then
        echo "❌ Anulat"
        exit 0
    fi
    
    echo "🔄 Reset baza de date..."
    docker-compose down -v
    docker-compose up -d
    echo "✅ Baza de date resetată cu date inițiale"
}

# Funcție pentru verificare admin
check_admin() {
    echo "🔍 Verificare user admin..."
    docker exec uzina-postgres psql -U postgres uzina_db -c "SELECT id, name, role FROM users WHERE role = 'ADMIN';"
}

# Funcție pentru creare admin
create_admin() {
    echo "👤 Creare user admin de urgență..."
    docker exec uzina-postgres psql -U postgres uzina_db -c "
    INSERT INTO users (id, name, role, password_hash) 
    VALUES (999, 'Emergency Admin', 'ADMIN', 'admin123')
    ON CONFLICT (id) DO NOTHING;
    "
    echo "✅ Admin creat: ID=999, Password=admin123"
}

# Meniu principal
case "$1" in
    export)
        export_data
        ;;
    import)
        import_data "$2"
        ;;
    reset)
        reset_data
        ;;
    check-admin)
        check_admin
        ;;
    create-admin)
        create_admin
        ;;
    *)
        echo "Folosire: ./db-manager.sh [comandă]"
        echo ""
        echo "Comenzi disponibile:"
        echo "  export         - Export toate datele (backup)"
        echo "  import <file>  - Import date dintr-un backup"
        echo "  reset          - Reset baza de date la datele inițiale"
        echo "  check-admin    - Verifică dacă există useri admin"
        echo "  create-admin   - Creează un admin de urgență"
        echo ""
        echo "Exemple:"
        echo "  ./db-manager.sh export"
        echo "  ./db-manager.sh import backup_20231222.sql"
        echo "  ./db-manager.sh check-admin"
        ;;
esac
