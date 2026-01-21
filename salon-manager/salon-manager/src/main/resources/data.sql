-- Dane testowe dla Salon Manager
-- Kolejność INSERT jest ważna ze względu na klucze obce (FK)
-- UWAGA: DataInitializer tworzy role ADMIN/USER i użytkowników admin@salon.pl, user@example.com
-- Ten plik dodaje dodatkowe dane testowe

-- 1. Roles (zgodne z DataInitializer: ADMIN, USER - bez prefiksu ROLE_)
INSERT INTO roles (id, name) VALUES
(1, 'ADMIN'),
(2, 'USER')
ON CONFLICT (id) DO NOTHING;

-- 2. Users (2 dodatkowych klientów testowych)
-- Hasła BCrypt - te konta nie będą działać do logowania, użyj user@example.com / haslo123
INSERT INTO users (id, email, password, first_name, last_name, enabled) VALUES
(3, 'anna.kowalska@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye', 'Anna', 'Kowalska', true),
(4, 'jan.nowak@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye', 'Jan', 'Nowak', true)
ON CONFLICT (id) DO NOTHING;

-- 3. User_Roles (przypisz rolę USER do użytkowników testowych)
INSERT INTO user_roles (user_id, role_id) VALUES
(3, 2),
(4, 2)
ON CONFLICT DO NOTHING;

-- 4. Employees (2 pracowników)
INSERT INTO employees (id, first_name, last_name, email) VALUES
(1, 'Katarzyna', 'Zielińska', 'katarzyna.zielinska@salon.pl'),
(2, 'Agnieszka', 'Kamińska', 'agnieszka.kaminska@salon.pl')
ON CONFLICT (id) DO NOTHING;

-- 5. ServiceOffers (5 usług)
INSERT INTO service_offers (id, name, price, duration_minutes) VALUES
(1, 'Strzyżenie damskie', 80.00, 45),
(2, 'Farbowanie włosów', 150.00, 120),
(3, 'Balayage', 250.00, 180),
(4, 'Manicure hybrydowy', 70.00, 60),
(5, 'Pedicure', 90.00, 75)
ON CONFLICT (id) DO NOTHING;

-- 6. EmployeeSpecializations
INSERT INTO employee_specializations (id, employee_id, service_offer_id, experience_years) VALUES
-- Katarzyna - fryzjerka (3 usługi)
(1, 1, 1, 5),
(2, 1, 2, 5),
(3, 1, 3, 3),
-- Agnieszka - stylistka paznokci (2 usługi)
(4, 2, 4, 6),
(5, 2, 5, 6)
ON CONFLICT (id) DO NOTHING;

-- 6a. EmployeeSchedules (grafiki pracowników)
INSERT INTO employee_schedules (id, employee_id, day_of_week, start_time, end_time, is_working_day) VALUES
-- Katarzyna - pracuje Pon-Pt 9:00-17:00
(1, 1, 'MONDAY', '09:00', '17:00', true),
(2, 1, 'TUESDAY', '09:00', '17:00', true),
(3, 1, 'WEDNESDAY', '09:00', '17:00', true),
(4, 1, 'THURSDAY', '09:00', '17:00', true),
(5, 1, 'FRIDAY', '09:00', '17:00', true),
(6, 1, 'SATURDAY', '09:00', '17:00', false),
(7, 1, 'SUNDAY', '09:00', '17:00', false),
-- Agnieszka - pracuje Wt-Sob 10:00-18:00
(8, 2, 'MONDAY', '10:00', '18:00', false),
(9, 2, 'TUESDAY', '10:00', '18:00', true),
(10, 2, 'WEDNESDAY', '10:00', '18:00', true),
(11, 2, 'THURSDAY', '10:00', '18:00', true),
(12, 2, 'FRIDAY', '10:00', '18:00', true),
(13, 2, 'SATURDAY', '10:00', '18:00', true),
(14, 2, 'SUNDAY', '10:00', '18:00', false)
ON CONFLICT (id) DO NOTHING;

-- 7. Reservations (7 rezerwacji, różne statusy) - daty w przyszłości (luty 2026)
INSERT INTO reservations (id, start_time, end_time, status, total_price, user_id, employee_id) VALUES
-- Zatwierdzone przez salon (czekają na potwierdzenie klienta)
(1, '2026-02-10 10:00:00', '2026-02-10 12:00:00', 'APPROVED_BY_SALON', 150.00, 3, 1),
(2, '2026-02-11 14:00:00', '2026-02-11 15:00:00', 'APPROVED_BY_SALON', 70.00, 4, 2),
-- Potwierdzone przez klienta
(3, '2026-02-20 11:00:00', '2026-02-20 11:45:00', 'CONFIRMED_BY_CLIENT', 80.00, 3, 1),
(4, '2026-02-21 15:00:00', '2026-02-21 18:00:00', 'CONFIRMED_BY_CLIENT', 250.00, 4, 1),
-- Utworzone (czekają na zatwierdzenie przez salon)
(5, '2026-02-24 10:00:00', '2026-02-24 11:15:00', 'CREATED', 90.00, 3, 2),
(6, '2026-02-25 09:00:00', '2026-02-25 09:45:00', 'CREATED', 80.00, 4, 1),
-- Anulowane
(7, '2026-02-15 16:00:00', '2026-02-15 17:00:00', 'CANCELLED', 80.00, 4, 1)
ON CONFLICT (id) DO NOTHING;

-- 8. ReservationServices (M2M - wiązanie rezerwacji z usługami)
INSERT INTO reservation_services (reservation_id, service_offer_id) VALUES
(1, 2),  -- Rezerwacja 1: Farbowanie włosów
(2, 4),  -- Rezerwacja 2: Manicure hybrydowy
(3, 1),  -- Rezerwacja 3: Strzyżenie damskie
(4, 3),  -- Rezerwacja 4: Balayage
(5, 5),  -- Rezerwacja 5: Pedicure
(6, 1),  -- Rezerwacja 6: Strzyżenie damskie
(7, 1)   -- Rezerwacja 7: Strzyżenie damskie (anulowana)
ON CONFLICT DO NOTHING;

-- 9. Reviews (3 opinie) - daty po 21 stycznia 2026
INSERT INTO reviews (id, content, created_at, user_id, image_filename) VALUES
(1, 'Wspaniała obsługa! Katarzyna wykonała perfekcyjne farbowanie. Polecam każdemu!',
    '2026-01-20 12:00:00', 3, NULL),
(2, 'Bardzo profesjonalny manicure. Agnieszka jest mistrzem swojego fachu.',
    '2026-01-20 16:00:00', 4, NULL),
(3, 'Balayage wyszedł pięknie! Dokładnie to, czego chciałam. Dziękuję!',
    '2026-01-20 17:00:00', 4, NULL)
ON CONFLICT (id) DO NOTHING;

-- 10. Reset sekwencji (PostgreSQL) - użyj COALESCE dla bezpieczeństwa
SELECT setval('roles_id_seq', COALESCE((SELECT MAX(id) FROM roles), 1));
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval('employees_id_seq', COALESCE((SELECT MAX(id) FROM employees), 1));
SELECT setval('service_offers_id_seq', COALESCE((SELECT MAX(id) FROM service_offers), 1));
SELECT setval('employee_specializations_id_seq', COALESCE((SELECT MAX(id) FROM employee_specializations), 1));
SELECT setval('employee_schedules_id_seq', COALESCE((SELECT MAX(id) FROM employee_schedules), 1));
SELECT setval('reservations_id_seq', COALESCE((SELECT MAX(id) FROM reservations), 1));
SELECT setval('reviews_id_seq', COALESCE((SELECT MAX(id) FROM reviews), 1));
