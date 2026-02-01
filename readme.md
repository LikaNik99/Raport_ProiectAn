# Chat Multiutilizator

Navigați în folderul proiectului:
```bash
cd Chat Multiutilizator
```

---

## Pornire cu Docker

Pornire:
```bash
docker-compose up --build
```

Oprire:
```bash
docker-compose down
```

Rulare în fundal:
```bash
docker-compose up -d --build
```

Aplicația va fi disponibilă pe http://localhost:5555

---

## Pornire manuală (fără Docker)

### Server

1. Instalare dependințe:
```bash
npm run install:all
```

2. Pornire server:
```bash
npm run start:server
```

### Electron

Pornire client (alt terminal):
```bash
npm run start:electron
```
