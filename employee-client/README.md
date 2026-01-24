# 🖥️ Client Logare - Aplicație Client pentru Sistem de Pontaj

Aplicație desktop Java pentru gestionarea pontajului angajaților - partea client.

## ⚡ START RAPID - FĂRĂ COMPILARE!

**NU AI NEVOIE DE MAVEN SAU COMPILARE!** 

Descarcă direct și rulează aplicația pre-compilată:

### 📥 Descarcă și Rulează (2 minute):

**1. Descarcă repository-ul:**
   - Click pe butonul verde **"Code"** → **"Download ZIP"**
   - SAU clonează: `git clone https://github.com/emmy10yes/employee-client.git`

**2. Extrage și intră în folder:**
   ```
   cd employee-client/distribution
   ```

**3. Asigură-te că ai Java 21+:**
   - Download: https://adoptium.net/temurin/releases/
   - Verifică: `java -version`

**4. Pornește aplicația:**
   ```
   Windows: Dublu-click pe launch-client.bat
   Linux/Mac: java -jar client-login.jar
   ```

**5. Conectare:**
   - Host: IP-ul serverului (ex: `192.168.1.100`)
   - Port: `5000`
   - Click **Connect** → **Login**

**✅ GATA! Aplicația pornește INSTANT, fără compilare!**

---

## 📋 Cerințe de Sistem

- **Java:** 21 sau mai nou (LTS)
- **Sistem de operare:** Windows, Linux, macOS
- **RAM:** Minim 256 MB
- **Spațiu:** ~20 MB

## 🚀 Start Rapid

### Pentru Utilizatori Finali (RECOMANDAT - Fără Compilare)

**Folder `distribution/` conține aplicația PRE-COMPILATĂ!**

```bash
# Download repository-ul (ZIP sau git clone)
cd distribution/

# Windows:
launch-client.bat

# Linux/Mac:
java -jar client-login.jar
```

**✅ ZERO compilare necesară! Direct din GitHub!**

---

### Pentru Dezvoltatori

**1. Clonează repository-ul:**
```bash
git clone <repository-url>
cd client-logare
```

**2. Compilează proiectul:**
```bash
mvn clean package
```

**3. Rulează clientul:**
```bash
# Pe Windows:
run-client.bat

# Pe Linux/Mac:
java -jar target/client-login.jar
```

### Pentru Utilizatori Finali

**1. Descarcă pachetul de distribuție:**
- Descarcă `ClientLogare.zip` din Releases
- SAU folosește scriptul `create-distribution.bat` pentru a genera pachetul

**2. Instalează și rulează:**
- Extrage arhiva într-un folder
- Asigură-te că ai Java 21+ instalat
- Rulează `launch-client.bat` (Windows) sau `launch-client.sh` (Linux/Mac)

## 📦 Creare Pachet de Distribuție

Pentru a crea un pachet gata de distribuit:

```bash
# Pe Windows:
create-distribution.bat

# Rezultat:
# - Folder distribution/ cu toate fișierele
# - Arhivă ClientLogare.zip (~18 MB)
```

Pachetul va conține:
- ✅ `client-login.jar` - Aplicația
- ✅ `launch-client.bat` - Script pornire Windows
- ✅ `README.txt` - Instrucțiuni rapide
- ✅ `INSTALARE_PC_RETEA.md` - Ghid instalare rețea
- ⚙️ Fișiere opționale (logo, nume companie, contact)

## 🔌 Conectare la Server

După pornirea aplicației:

1. **Host:** IP-ul serverului (ex: `192.168.1.100` sau `localhost`)
2. **Port:** `5000` (default)
3. Click **Connect** → Indicator verde ✅
4. **ID:** ID-ul tău de utilizator
5. **Password:** Parola ta
6. Click **Login**

### Utilizatori Impliciți (Pentru Test)

| Rol | ID | Parolă |
|-----|-----|--------|
| Administrator | 1 | admin123 |
| HR | 2001 | admin123 |
| Angajat | 1001 | parola1 |

## 🛠️ Dezvoltare

### Structura Proiectului

```
client-logare/
├── src/main/java/md/uzina/client/     # Cod sursă
├── docs/                               # Documentație
├── distribution/                       # Pachet de distribuție (generat)
├── target/                            # Fișiere compilate
├── launch-client.bat                  # Launcher Windows
├── launch-client.sh                   # Launcher Linux/Mac
├── run-client.bat                     # Development launcher
├── create-distribution.bat            # Script creare pachet
└── pom.xml                            # Maven configuration
```

### Dependențe

- **Gson** 2.10.1 - Serializare JSON
- **SLF4J** 2.0.9 - Logging
- **Apache POI** 5.2.3 - Export Excel

### Compilare și Testare

```bash
# Compilare normală
mvn clean package

# Compilare fără teste
mvn clean package -DskipTests

# Rulare din IDE
# Main class: md.uzina.client.ClientApp
```

## 📱 Instalare pe PC-uri din Rețea

Pentru a instala clientul pe alte PC-uri din rețea (fără cod sursă):

**1. Pe PC-ul de dezvoltare:**
```bash
create-distribution.bat
```

**2. Pe PC-ul destinație:**
- Copiază `ClientLogare.zip` sau folderul `distribution/`
- Instalează Java 21+ (dacă nu există)
- Extrage și rulează `launch-client.bat`
- Configurează IP-ul serverului în fereastra de login

**Detalii complete:** [docs/INSTALARE_PC_RETEA.md](docs/INSTALARE_PC_RETEA.md)

## 📚 Documentație

- [GHID_DISTRIBUTIE.md](docs/GHID_DISTRIBUTIE.md) - Ghid complet de distribuție
- [INSTALARE_PC_RETEA.md](docs/INSTALARE_PC_RETEA.md) - Instalare pe rețea
- [CLIENT_README.md](docs/CLIENT_README.md) - Documentație dezvoltator

## ❓ Probleme Frecvente

### Nu se pornește aplicația
```bash
# Verifică versiunea Java
java -version

# Trebuie să fie 21 sau mai nou
# Reinstalează Java dacă e necesar
```

### Nu se poate conecta la server
- Verifică că serverul rulează
- Verifică IP-ul și portul
- Verifică firewall-ul (port 5000 trebuie deschis)
- Testează conexiunea: `ping <server-ip>`

### Eroare la compilare
```bash
# Curăță și recompilează
mvn clean package

# Verifică că ai Maven instalat
mvn -version
```

## 📞 Suport

Pentru probleme tehnice, contactează administratorul de sistem.

## 📄 Licență

Proprietate privată - Uzina Company

---

**Versiune:** 1.0-SNAPSHOT  
**Ultima actualizare:** Ianuarie 2026
