# PayBank Docker Instructions

Acest proiect a fost containerizat folosind Docker. Urmează pașii de mai jos pentru a rula aplicația.

## Pre-rechizite

- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalat și pornit.
- Fișierul `script.sql` în directorul rădăcină (necesar pentru crearea tabelelor).

## Pornire Aplicație

1. Deschide un terminal în directorul rădăcină al proiectului.
2. Rulează comanda:

```bash
docker-compose up --build
```

Această comandă va:
1.  Porni containerul SQL Server (`db`).
2.  Aștepta ca serverul să fie gata și apoi va rula automat scriptul de inițializare (`script.sql`) folosind containerul `db-init`.
3.  Construi și porni aplicația PayBank doar după ce baza de date a fost populată.

**Notă:** Prima pornire poate dura câteva secunde în plus până se inițializează baza de date.

## Accesare

- Aplicația web web: `http://localhost:5000`
- Baza de date SQL Server: `localhost,1433`

## Depanare Bază de Date

Dacă aplicația nu se conectează:
- Verifică log-urile containerului `db-init` pentru erori la rularea scriptului:
  ```bash
  docker-compose logs db-init
  ```
- Asigură-te că `script.sql` este valid și codat în UTF-8 sau ASCII (nu UTF-16 dacă întâmpini probleme).

## Oprire

Pentru a opri: `Ctrl+C` sau:

```bash
docker-compose down
```
Pentru a șterge și volumul bazei de date (resetare completă):
```bash
docker-compose down -v
```
