-- Dane testowe dla Salon Manager
-- Kolejność INSERT jest ważna ze względu na klucze obce (FK)

-- 1. Roles
INSERT INTO roles (id, name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_CLIENT')
ON CONFLICT (id) DO NOTHING;

-- 2. Users (2 klientów)
INSERT INTO users (id, email, password, first_name, last_name, enabled) VALUES
(1, 'anna.kowalska@example.com', 'password123', 'Anna', 'Kowalska', true),
(2, 'jan.nowak@example.com', 'password123', 'Jan', 'Nowak', true)
ON CONFLICT (id) DO NOTHING;

-- 3. User_Roles
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 2),
(2, 2)
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

-- 7. Reservations (6 rezerwacji, różne statusy)
INSERT INTO reservations (id, start_time, end_time, status, total_price, user_id, employee_id) VALUES
-- Zatwierdzone przeszłe
(1, '2025-12-10 10:00:00', '2025-12-10 11:00:00', 'APPROVED', 150.00, 1, 1),
(2, '2025-12-11 14:00:00', '2025-12-11 15:00:00', 'APPROVED', 70.00, 2, 2),
-- Potwierdzone przez klienta
(3, '2025-12-20 11:00:00', '2025-12-20 12:00:00', 'CONFIRMED_BY_CLIENT', 80.00, 1, 1),
(4, '2025-12-21 15:00:00', '2025-12-21 16:00:00', 'CONFIRMED_BY_CLIENT', 250.00, 2, 1),
-- Utworzone (czekają na akcję)
(5, '2025-12-22 10:00:00', '2025-12-22 11:15:00', 'CREATED', 90.00, 1, 2),
-- Anulowane
(6, '2025-12-15 16:00:00', '2025-12-15 17:00:00', 'CANCELLED', 80.00, 2, 1)
ON CONFLICT (id) DO NOTHING;

-- 8. ReservationServices (M2M - wiązanie rezerwacji z usługami)
INSERT INTO reservation_services (reservation_id, service_offer_id) VALUES
(1, 2),  -- Rezerwacja 1: Farbowanie włosów
(2, 4),  -- Rezerwacja 2: Manicure hybrydowy
(3, 1),  -- Rezerwacja 3: Strzyżenie damskie
(4, 3),  -- Rezerwacja 4: Balayage
(5, 5),  -- Rezerwacja 5: Pedicure
(6, 1)   -- Rezerwacja 6: Strzyżenie damskie
ON CONFLICT DO NOTHING;

-- 9. Reviews (3 opinie)
INSERT INTO reviews (id, content, created_at, user_id) VALUES
(1, 'Wspaniała obsługa! Katarzyna wykonała perfekcyjne farbowanie. Polecam każdemu!',
    '2025-12-10 12:00:00', 1),
(2, 'Bardzo profesjonalny manicure. Agnieszka jest mistrzem swojego fachu.',
    '2025-12-11 16:00:00', 2),
(3, 'Balayage wyszedł pięknie! Dokładnie to, czego chciałam. Dziękuję!',
    '2025-12-21 17:00:00', 2)
ON CONFLICT (id) DO NOTHING;

-- 10. Reset sekwencji (PostgreSQL)
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('employees_id_seq', (SELECT MAX(id) FROM employees));
SELECT setval('service_offers_id_seq', (SELECT MAX(id) FROM service_offers));
SELECT setval('employee_specializations_id_seq', (SELECT MAX(id) FROM employee_specializations));
SELECT setval('employee_schedules_id_seq', (SELECT MAX(id) FROM employee_schedules));
SELECT setval('reservations_id_seq', (SELECT MAX(id) FROM reservations));
SELECT setval('reviews_id_seq', (SELECT MAX(id) FROM reviews));
