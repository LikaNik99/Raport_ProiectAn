#!/bin/bash
# ========================================
# Client Logare - Launcher (Linux/Mac)
# ========================================

cd "$(dirname "$0")"

# Verifică dacă există Java
if ! command -v java &> /dev/null; then
    echo ""
    echo "========================================"
    echo " EROARE: Java nu este instalat!"
    echo "========================================"
    echo ""
    echo "Pentru a rula Client Logare, ai nevoie de Java 17 sau mai nou."
    echo ""
    echo "Download Java 17 de pe:"
    echo "https://adoptium.net/temurin/releases/"
    echo ""
    echo "Alege:"
    echo "- Version: 17 (LTS)"
    echo "- Operating System: Linux / macOS"
    echo "- Architecture: x64 / aarch64"
    echo ""
    read -p "Apasă Enter pentru a continua..."
    exit 1
fi

# Verifică versiunea Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo ""
    echo "========================================"
    echo " AVERTISMENT: Versiune Java prea veche!"
    echo "========================================"
    echo ""
    echo "Ai Java $JAVA_VERSION, dar este nevoie de Java 21+"
    echo ""
    echo "Continuăm oricum..."
    echo ""
fi

# Verifică dacă JAR-ul există
if [ ! -f "client-login.jar" ]; then
    echo ""
    echo "========================================"
    echo " EROARE: client-login.jar nu există!"
    echo "========================================"
    echo ""
    echo "Asigură-te că fișierul client-login.jar"
    echo "este în același folder cu acest script."
    echo ""
    read -p "Apasă Enter pentru a continua..."
    exit 1
fi

# Pornește aplicația
echo ""
echo "========================================"
echo " Pornire Client Logare..."
echo "========================================"
echo ""
echo "Tip: Pentru a vedea mesajele în consolă,"
echo "rulează: java -jar client-login.jar"
echo ""

java -jar client-login.jar &

echo ""
echo "Clientul a pornit!"
echo ""

exit 0
