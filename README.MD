# Salon Manager - System Rezerwacji i Zarządzania

Kompleksowa aplikacja webowa typu SaaS dedykowana dla salonów usługowych (fryzjerzy, kosmetyczki). System nie tylko umożliwia rezerwację wizyt, ale pełni rolę centrum dowodzenia biznesem - od zarządzania grafikami pracowników po analitykę finansową.

## Kluczowe Funkcjonalności

### Algorytm Rezerwacji
* Uzytkownik wybiera usługi, które chce wykonać
* Algorytm sumuje czas trwania wybranych usług i przelicza je na jednostki czasowe (sloty 15 min).
* Algorytm filtruje pracowników pod kątem posiadanych specjalizacji wymaganych do wykonania kompletu wybranych usług.
* Algorytm wyszukuje odpowiednio długie okna czasowe w grafikach pracowników.
* Algorytm wyświetla użytkownikowi dostępne terminy do wyboru.

### Moduł Analityczny
* Obliczanie średniej kwoty wizyty klienta.
* Analiza średniego czasu trwania usług.
* Wykorzystanie Group By i RowMapper dla maksymalnej wydajności przy dużych zbiorach danych.

### Panel Klienta
* Historia wizyt: Przegląd zakończonych i odwołanych rezerwacji z funkcją filtrowania i sortowania.
* Zarządzanie kontem: Edycja danych profilowych (Reactive Forms) z walidacją pól.
* Elastyczne zmiany: Możliwość edycji lub odwołania rezerwacji, zanim salon potwierdzi wizytę.
* Wystawianie opinii dla salonu z możliwością usuwania własnych opinii i dodawania zdjęć do opinii.

### Panel Administratora
* Zarządzanie kadrą: Pełny moduł CRUD dla pracowników wraz ze szczegółowymi formularzami.
* Kontrola użytkowników: Monitorowanie statusów kont (Aktywne/Zawieszone) i statystyk użytkowników.
* Przegląd rezerwacji: Centralny system zarządzania wszystkimi wizytami w salonie.

![User dashboard](./images/user.mp4)

![Admin dashboard](./images/admin.mp4)

## Architektura projektu

* **`frontend/`** - Aplikacja kliencka (Angular), serwowana produkcyjnie przez serwer **Nginx** na porcie `80`.
* **`salon-manager/`** - Serwer aplikacji backendowej (Spring Boot, Java 21) nasłuchujący na porcie `8080`.
* **`postgres`** - Relacyjna baza danych nasłuchująca na porcie `5432`.

## Stack Techniczny

### Backend: Spring Boot 3 (Java 21)

Data Access Layer: 
* Spring Data JPA: Obsługa złożonych relacji (@ManyToOne, @ManyToMany) oraz customowe zapytania z paginacją (Pageable).
* JdbcTemplate & SQL: Wykorzystane w module analitycznym. Napisane od zera zapytania SQL z GROUP BY oraz własne mapowanie wyników przez RowMapper do obliczania statystyk biznesowych.

Logika i Bezpieczeństwo:
* Spring Security: System ról (ROLE_CLIENT, ROLE_ADMIN), szyfrowanie BCrypt oraz ochrona endpointów REST.
* State Machine: Zarządzanie cyklem życia rezerwacji (CREATED -> CONFIRMED -> APPROVED -> CANCELLED) z logiką blokady edycji.
* Integrity: Wykorzystanie @Transactional dla zapewnienia spójności procesów wielousługowych.

Komunikacja i Widoki:
* REST API & Swagger: Pełna dokumentacja OpenAPI dostępna pod /swagger-ui.html.
* Hybrid Rendering: Wykorzystanie Thymeleaf do szybkiego renderowania sekcji publicznej (cennik, opinie) oraz Angular dla zaawansowanych paneli interaktywnych.
* Global Error Handling: Centralny mechanizm @RestControllerAdvice zapewniający spójne odpowiedzi API.

### Frontend: Angular 20
* State Management: Reaktywne podejście oparte na Services + RxJS.
* UI/UX: Biblioteka Angular Material, obsługa trybów Dark/Light Mode oraz pełna responsywność.
* Zaawansowane Formularze: Wielokrokowy kreator wizyt (Material Stepper) z autorskimi walidatorami (custom validators).
* Security & Flow: Implementacja Guards (ochrona tras) oraz Interceptors (automatyczne dołączanie autentykacji i obsługa błędów).

### Jakość Kodu i DevOps
* Testowanie (JUnit 5 & Mockito): Pokrycie kodu na poziomie 80% (JaCoCo). Testy obejmują repozytoria (@DataJpaTest), kontrolery (@WebMvcTest) oraz pełne scenariusze integracyjne (@SpringBootTest).
* Konteneryzacja: Pełna orkiestracja środowiska za pomocą Docker Compose (Spring Boot, PostgreSQL, Nginx).

## Model Danych
* User & Role – system autoryzacji.
* Employee & ServiceOffer – zarządzanie kadrą i ofertą (relacja specjalizacji).
* Reservation – centralny punkt systemu łączący klienta, pracownika i wybrane usługi.
* Review – system opinii budujący zaufanie w strefie publicznej.

![Class Diagram](./images/diagram_klas.png)

## Instalacja i Demo

Wejdź do głównego katalogu `salon-manager` (tego, w którym znajduje się plik `docker-compose.yml`) i wykonaj poniższą komendę:

```bash
docker compose up -d --build
```

**Dostępne aplikacje po uruchomieniu:**
* Główny interfejs (Frontend): `http://localhost`
* API Backend: `http://localhost:8080/api/...`
* Baza danych: `localhost:5432`

**Logowanie (Dane testowe):**
* Admin: admin@salon.pl / admin123
* Klient: user@example.com / haslo123

**Uruchomienie testów backendu:**

```bash
./mvnw clean test
```

**Swagger:** http://localhost:8080/swagger-ui/index.html
**JaCoCo:** http://localhost:8080/jacoco/index.html

![jacoco coverage](./images/jacoco.png)