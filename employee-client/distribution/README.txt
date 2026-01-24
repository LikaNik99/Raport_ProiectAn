# 🚀 Client Logare - Ghid Rapid de Instalare

## 📦 Ce ai primit

- `client-login.jar` - Aplicația client
- `launch-client.bat` - Script de pornire (Windows)
- Acest fișier README

## ⚡ Start Rapid (3 pași)

### 1. Instalează Java (dacă nu ai)

Download Java 21: https://adoptium.net/temurin/releases/

Alege:
- **Version:** 21 (LTS)
- **Operating System:** Windows / Linux / macOS
- **Architecture:** x64

Instalează cu setările implicite.

### 2. Pune fișierele într-un folder

Creează un folder (ex: `C:\ClientLogare\`) și copiază toate fișierele acolo.

### 3. Pornește aplicația

**Windows:**
- Dublu-click pe `launch-client.bat`

**SAU Orice sistem (Windows/Linux/Mac):**
```bash
java -jar client-login.jar
```

---

## 🔌 Conectare la Server

### În fereastra de login:

1. **Host:** 
   - `localhost` (dacă serverul e pe același PC)
   - IP-ul serverului (ex: `192.168.1.100`)
   
2. **Port:** `5000`

3. Click **Connect** (indicatorul devine verde ✅)

4. **ID:** ID-ul tău de utilizator (ex: `1` pentru admin)

5. **Password:** Parola ta

6. Click **Login**

---

## 👥 Utilizatori Impliciți (Pentru Test)

După instalarea serverului:

| Rol | ID | Parolă |
|-----|-----|--------|
| Administrator | 1 | admin123 |
| HR | 2001 | admin123 |
| Angajat | 1001 | parola1 |

---

## ❓ Probleme?

### Nu se pornește aplicația
- Verifică că ai Java 21+ instalat
- Rulează: `java -version` în CMD/Terminal
- Reinstalează Java dacă e necesar

### Nu te poți conecta la server
- Verifică că serverul rulează
- Verifică IP-ul serverului
- Dacă serverul e pe alt PC, verifică firewall-ul

### Indicator roșu după "Connect"
- Serverul nu este pornit SAU
- IP-ul/portul este greșit SAU
- Firewall blochează conexiunea

---

## 📞 Ajutor

Pentru asistență tehnică, contactează administratorul de sistem.

---

## 🔄 Actualizare

Pentru a actualiza aplicația:
1. Șterge vechiul `client-login.jar`
2. Copiază noul `client-login.jar`
3. Repornește aplicația

Setările tale (logo, nume companie) vor fi păstrate.
