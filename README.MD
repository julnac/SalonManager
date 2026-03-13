# Salon Manager

Aplikacja do zarządzania salonem, składająca się z frontendu w **Angular** i backendu w **Spring Boot** (Java), korzystająca z bazy danych **PostgreSQL**.

Wszystkie środowiska mogą zostać uruchomione równolegle za pomocą Dockera, albo osobno w trybie developerskim.

## Architektura projektu

* **`frontend/`** - Aplikacja kliencka (Angular), serwowana produkcyjnie przez serwer **Nginx** na porcie `80`.
* **`salon-manager/`** - Serwer aplikacji backendowej (Spring Boot, Java 21) nasłuchujący na porcie `8080`.
* **`postgres`** - Relacyjna baza danych nasłuchująca na porcie `5432`.

## Wymagania

Aby uruchomić projekt jako całość za pomocą kontenerów (rozwiązanie rekomendowane i najprostsze) będziesz potrzebować:
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) lub samego demona Docker 
* `docker-compose` (zintegrowany z nowszymi wersjami Dockera w komendzie `docker compose`)

## Instrukcja uruchomienia

### 1. Uruchomienie całości przez Docker Compose (Rekomendowane)

Wejdź do głównego katalogu `salon-manager` (tego, w którym znajduje się plik `docker-compose.yml`) i wykonaj poniższą komendę:

```bash
docker compose up -d --build
```

Co się stanie?
1. Zostanie pobrany obraz PostgreSQL i wystartowana baza danych.
2. Zostanie przebudowany (z użyciem Mavena poprzez Dockerfile wewnątrz backendu) i uruchomiony backend w Spring Boot. Backend poczeka, aż baza danych zgłosi gotowość.
3. Zostanie skompilowany frontend (Angular) do statycznych plików i umiejscowiony na Nginxsie. Frontend uruchomi się dopiero, gdy odpowiadać będzie podpięty backend.

**Dostępne aplikacje po uruchomieniu:**
* Główny interfejs (Frontend): `http://localhost/` lub po prostu `http://localhost`
* API Backend: `http://localhost:8080/api/...`
* Baza danych: `localhost:5432`

Aby **zatrzymać** wszystkie kontenery, użyj polecenia:
```bash
docker compose down
```

### 2. Manualne uruchomienie środowiska do pisania kodu (Developer Mode)

Jeżeli chcesz pisać kod i widzieć zmiany na żywo - musisz odpalić obydwie aplikacje ręcznie.

> Uwaga: Upewnij się, że masz zainstalowaną Javę w wersji co najmniej 21 oraz środowisko Node.js. Oprócz tego musisz mieć działającą bazę danych Postgres (możesz np. odpalić samego postgresa z dockera: `docker compose up -d postgres`)

#### Backend (Spring Boot):
1. Przejdź do folderu backendu: 
```bash
cd salon-manager
```
2. Uruchom polecenie:
```bash
./mvnw spring-boot:run
```

#### Frontend (Angular):
1. Przejdź do folderu frontendu (w osobnym oknie terminala):
```bash
cd frontend
```
2. Zainstaluj biblioteki z `package.json` (wymagane tylko za pierwszym razem):
```bash
npm install
```
3. Uruchom deweloperski serwer z auto-odświeżaniem:
```bash
npm start
```
Frontend domyślnie będzie dostępny pod adresem: `http://localhost:4200/`. Dzięki konfiguracji w `SecurityConfig` (backend), port 4200 jest autoryzowany do zapytań CORS w trybie deweloperskim.
