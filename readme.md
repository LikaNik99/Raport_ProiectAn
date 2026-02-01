# CRM - Management Proiecte Sociale

Aplicație CRM dezvoltată pentru gestionarea proiectelor sociale - proiect realizat în cadrul cursului de **Management Proiecte Sociale**.

---

## 📋 Cerințe software

Instalați următoarele înainte de pornire:

1. **Docker Desktop** - [Descarcă](https://www.docker.com/products/docker-desktop/)
2. **Node.js** (include npm) - [Descarcă](https://nodejs.org/)
3. **Java JDK 17+** - [Descarcă](https://www.oracle.com/java/technologies/downloads/)
4. **Maven** - [Descarcă](https://maven.apache.org/download.cgi)

---

## 🚀 PORNIRE RAPIDĂ (Metoda automată - RECOMANDATĂ)

### Configurare inițială (DOAR PRIMA DATĂ)

```cmd
cd cale\către\Managementul proiectelor Sociale\demo\frontend
npm install
```

```cmd
cd cale\către\Managementul proiectelor Sociale\temp\Composes
docker compose up -d
```

### Pornire aplicație (de fiecare dată)

1. **Asigurați-vă că Docker Desktop rulează**

2. **Rulați scriptul automat:**

```cmd
cd cale\către\Managementul proiectelor Sociale\demo\frontend
launch.cmd
```

**Gata!** 🎉

Scriptul va:
- ✅ Deschide 3 ferestre automat (Frontend, Backend, MySQL logs)
- ✅ Verifica că toate serviciile pornesc corect
- ✅ Afișa adresele de acces:

```
==================================================
SYSTEM READY
Local:   http://localhost:3000
Network: http://172.20.130.34:3000
==================================================
```

⚠️ **NU închideți ferestrele!** Aplicația se va opri dacă închideți vreun terminal.

---

## 🔧 Pornire manuală (Pas cu pas)

Dacă preferați control manual sau dacă `launch.cmd` nu funcționează:

### 1️⃣ Pornește baza de date (Docker)

```cmd
cd cale\către\Managementul proiectelor Sociale\temp\Composes
docker compose up -d
```

Așteptați ~30 secunde ca MySQL să pornească.

---

### 2️⃣ Pornește Frontend-ul (React)

**Deschideți un nou Command Prompt:**

```cmd
cd cale\către\Managementul proiectelor Sociale\demo\frontend
npm install
npm start
```

Veți vedea:
```
Compiled successfully!
Local:   http://localhost:3000
Network: http://172.20.130.34:3000
```

⚠️ **NU închideți această fereastră!**

---

### 3️⃣ Pornește Backend-ul (Spring Boot)

**Deschideți încă un Command Prompt:**

```cmd
cd cale\către\Managementul proiectelor Sociale\demo
mvn spring-boot:run -DskipTests
```

Așteptați mesajul:
```
INFO --- Tomcat started on port 8080 (http)
INFO --- Started CrmDemoApplication
```

⚠️ **NU închideți această fereastră!**

---

### 4️⃣ Accesați aplicația

**Local:**
- http://localhost:3000

**Din rețea:**
- http://[IP-ul_vostru]:3000

Pentru a afla IP-ul: `ipconfig` → căutați **IPv4 Address**

---

## 🌐 Configurare pentru acces din rețea

### Pasul 1: Aflați IP-ul computerului

```cmd
ipconfig
```

Căutați **IPv4 Address** (exemplu: `172.20.130.34`)

### Pasul 2: Configurați Axios (Frontend)

Editați fișierul de configurare API (`src/api.js`, `src/axiosConfig.js` sau similar):

```javascript
import axios from "axios";

// Înlocuiți cu IP-ul VOSTRU real
axios.defaults.baseURL = "http://172.20.130.34:8080";

// SAU
export const api = axios.create({
  baseURL: "http://172.20.130.34:8080"
});
```

❌ **NICIODATĂ `localhost:8080`** - nu va funcționa din rețea!

### Pasul 3: Configurați CORS (Backend)

Editați configurarea CORS în backend (`config/CorsConfig.java` sau `WebConfig.java`):

```java
configuration.setAllowedOrigins(List.of(
    "http://localhost:3000",
    "http://172.20.130.34:3000"  // IP-ul VOSTRU
));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowCredentials(true);
```

**Restartați backend-ul** după modificare (Ctrl+C, apoi din nou `mvn spring-boot:run -DskipTests`).

### Pasul 4: Testați conexiunea

Din alt computer, deschideți browser-ul:

```
http://172.20.130.34:8080/actuator/health
```

✅ Răspuns JSON = backend accesibil  
❌ Nu se încarcă = verificați firewall/configurare

---

## 🛑 Oprirea aplicației

### Metoda automată:
În fiecare fereastră deschisă de `launch.cmd`: **Ctrl + C** → **Y**

### Metoda manuală:
1. În terminalele Frontend și Backend: **Ctrl + C** → **Y**
2. Opriți Docker:
   ```cmd
   cd cale\către\Managementul proiectelor Sociale\temp\Composes
   docker compose down
   ```

---

## ⚠️ Probleme frecvente

### Aplicația nu funcționează din rețea

**Verificați în ordine:**

1. **Backend-ul folosește IP corect?**
   - Axios trebuie configurat cu `http://[IP]:8080`, NU `localhost:8080`

2. **CORS permite IP-ul frontend-ului?**
   - Adăugați `http://[IP]:3000` în `allowedOrigins`
   - Restartați backend-ul

3. **Firewall blochează porturile?**
   - Windows Defender Firewall → permiteți 8080 și 3000

### "Port already in use"

```cmd
netstat -ano | findstr :3000
taskkill /PID [numărul_procesului] /F
```

### "Docker not running"

Porniți Docker Desktop și așteptați să se încarce complet.

### Backend nu răspunde

Verificați dacă backend-ul ascultă pe toate interfețele:

În `application.properties` sau `application.yml`:
```properties
server.address=0.0.0.0
```

---

## 📝 Observații importante

### De ce `localhost` nu funcționează din rețea?

**`localhost` = computerul care DESCHIDE browser-ul, NU serverul!**

Când accesați aplicația din alt computer:
- Frontend-ul se încarcă ✅ (HTML/CSS/JS sunt deja descărcate)
- API-urile eșuează ❌ (Axios caută backend pe computerul CLIENT, nu pe SERVER)

**Soluție:** Folosiți IP-ul real în configurarea Axios!

### Componente necesare simultan

Toate 3 trebuie să ruleze:
- 🗄️ **MySQL** (Docker) - port 3306
- ⚛️ **React** (Frontend) - port 3000
- 🍃 **Spring Boot** (Backend) - port 8080

---



---
**Elaborat de Artenii Vasile CR-221FR**
**Pentru cursul Aplicatii Client Server ,Profesor:Rotaru Lilia **
