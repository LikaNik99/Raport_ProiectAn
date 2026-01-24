# 🖥️ Instalare Client pe Alt PC din Rețea

## 📋 Ce ai nevoie

### Pe PC-ul cu serverul:
- Serverul pornit și rulând
- Adresa IP a serverului în rețea (ex: `192.168.1.100`)
- Portul serverului (default: `5000`)

### Pe PC-ul client:
- Java 21+ instalat
- Fișierul JAR al clientului
- Conexiune în aceeași rețea cu serverul

---

## 🚀 Pași de instalare

### 1️⃣ Verifică adresa IP a serverului

Pe PC-ul unde rulează serverul, deschide PowerShell/CMD:

```powershell
ipconfig
```

Caută adresa IPv4 (ex: `192.168.1.100`)

---

### 2️⃣ Verifică că serverul ascultă pe rețea

Pe PC-ul cu serverul:

```powershell
# Verifică că serverul ascultă pe toate interfețele
netstat -an | findstr :5000
```

Dacă vezi `0.0.0.0:5000` sau `[::]:5000` - serverul ascultă pe toate interfețele ✅
Dacă vezi `127.0.0.1:5000` - serverul ascultă doar local ❌

---

### 3️⃣ Copiază fișierele pe PC-ul client

**Varianta A: Doar JAR-ul (Simplu)**

Copiază pe PC-ul client:
```
client-login.jar
```

Locație sursă: `client-logare\target\client-login.jar`

**Varianta B: Cu scripturi (Recomandat)**

Copiază pe PC-ul client:
```
📁 ClientLogare\
  ├── client-login.jar
  ├── start-client.bat (pentru Windows)
  └── company_logo.png (opțional)
  └── company_name.txt (opțional)
  └── company_contact.txt (opțional)
```

---

### 4️⃣ Creează script de pornire pe PC-ul client

**Pentru Windows** - `start-client.bat`:

```batch
@echo off
echo Starting Client Logare...
cd /d "%~dp0"

REM Verifică dacă există Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java nu este instalat!
    echo Instalează Java 21+ de pe: https://adoptium.net/
    pause
    exit /b 1
)

REM Pornește clientul
start "" java -jar client-login.jar

echo Client started!
timeout /t 2 >nul
```

**Pentru Linux/Mac** - `start-client.sh`:

```bash
#!/bin/bash
echo "Starting Client Logare..."
cd "$(dirname "$0")"

# Verifică dacă există Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java nu este instalat!"
    exit 1
fi

# Pornește clientul
java -jar client-login.jar &

echo "Client started!"
```

Apoi fă fișierul executabil:
```bash
chmod +x start-client.sh
```

---

### 5️⃣ Pornește clientul

**Windows:**
- Dublu-click pe `start-client.bat`
- SAU deschide CMD și rulează: `java -jar client-login.jar`

**Linux/Mac:**
```bash
./start-client.sh
```
SAU
```bash
java -jar client-login.jar
```

---

### 6️⃣ Conectează-te la server

La deschiderea aplicației vei vedea fereastra de login:

1. **Host:** Introdu adresa IP a serverului (ex: `192.168.1.100`)
2. **Port:** Lasă `5000` (sau portul tău custom)
3. Click **Connect** - indicatorul va deveni verde ✅
4. **ID:** Introdu ID-ul utilizatorului (ex: `1` pentru admin)
5. **Password:** Introdu parola (ex: `admin123`)
6. Click **Login**

---

## 🔥 Firewall - Configurare Server

Dacă clientul NU se poate conecta, verifică firewall-ul pe PC-ul cu serverul:

### Windows Firewall

**Opțiunea 1: Dezactivează temporar pentru test**
```powershell
# Rulează ca Administrator
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False
```

**Opțiunea 2: Adaugă regulă pentru portul 5000** (Recomandat)
```powershell
# Rulează ca Administrator
New-NetFirewallRule -DisplayName "Server Logare" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow
```

**SAU manual prin interfață:**
1. Deschide "Windows Defender Firewall cu Securitate Avansată"
2. Click pe "Inbound Rules" → "New Rule"
3. Type: Port → Next
4. TCP, Specific local ports: `5000` → Next
5. Allow the connection → Next
6. Bifează toate: Domain, Private, Public → Next
7. Nume: "Server Logare Port 5000" → Finish

### Linux (Ubuntu/Debian)

```bash
sudo ufw allow 5000/tcp
sudo ufw reload
```

### Verifică conexiunea de pe PC-ul client:

```powershell
# Windows PowerShell
Test-NetConnection -ComputerName 192.168.1.100 -Port 5000
```

```bash
# Linux/Mac
telnet 192.168.1.100 5000
# SAU
nc -zv 192.168.1.100 5000
```

Dacă conexiunea reușește, vezi: `TcpTestSucceeded : True` ✅

---

## ✅ Checklist Rezolvare Probleme

- [ ] Serverul rulează pe PC-ul server
- [ ] Serverul ascultă pe `0.0.0.0:5000` (nu doar `127.0.0.1`)
- [ ] Firewall-ul permite conexiuni pe portul 5000
- [ ] Ambele PC-uri sunt în aceeași rețea
- [ ] Ai IP-ul corect al serverului
- [ ] Java 21+ este instalat pe PC-ul client
- [ ] Fișierul `client-login.jar` există
- [ ] În interfața client, ai introdus IP-ul serverului (NU `localhost`)

---

## 🌐 Configurare pentru acces din exterior (Internet)

Dacă vrei să accesezi serverul de pe Internet (nu doar rețea locală):

### 1. Port Forwarding pe Router

1. Intră în interfața routerului (ex: `192.168.1.1`)
2. Găsește "Port Forwarding" sau "Virtual Server"
3. Adaugă regulă:
   - **External Port:** 5000
   - **Internal IP:** IP-ul PC-ului cu serverul (ex: `192.168.1.100`)
   - **Internal Port:** 5000
   - **Protocol:** TCP

### 2. IP Public

Găsește IP-ul tău public:
- Site: https://whatismyipaddress.com/
- SAU: `curl ifconfig.me`

### 3. Conectare de pe client

În câmpul **Host** folosește:
- IP-ul public al serverului (ex: `93.115.x.x`)
- SAU un domeniu dacă ai configurat DNS dinamic

⚠️ **Securitate:** Pentru producție, folosește VPN sau tunel criptat (ex: Tailscale, WireGuard)

---

## 📱 Utilizatori Impliciți (Pentru Test)

După instalarea serverului, poți testa cu:

| Rol | ID | Parolă |
|-----|-----|--------|
| ADMIN | 1 | admin123 |
| HR | 2001 | admin123 |
| WORKER | 1001 | parola1 |

---

## 🆘 Probleme Comune

### "Connection refused"
- Serverul nu rulează SAU
- Firewall blochează portul SAU
- IP-ul/portul este greșit

### "Connection timeout"
- Ambele PC-uri nu sunt în aceeași rețea SAU
- Firewall blochează complet SAU
- Serverul nu ascultă pe interfața de rețea

### Indicator roșu după "Connect"
- Server-ul nu este accesibil
- Verifică cu `Test-NetConnection` (Windows) sau `telnet` (Linux)

### Login eșuează
- Server-ul nu are utilizatorul în baza de date
- Parola este greșită
- Verifică cu administratorul că utilizatorul există

---

## 💡 Tips

1. **Pentru teste:** Folosește `localhost` pe PC-ul cu serverul
2. **Pentru producție:** Configurează IP static pentru serverul tău
3. **Backup:** Salvează `company_logo.png`, `company_name.txt` etc.
4. **Update:** Pentru a actualiza clientul, doar înlocuiește `client-login.jar`

---

## 📞 Contact & Support

Pentru probleme sau întrebări despre instalare, contactează administratorul de sistem.
