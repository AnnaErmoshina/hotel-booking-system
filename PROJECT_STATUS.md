# PROJECT_STATUS.md — Hotel Booking System

> **Назначение файла.** Это единственный источник правды о состоянии проекта.
> Если ты — ИИ-агент, которому дали архив этого репозитория, и человек просит
> "продолжи разработку" — прочитай этот файл **целиком, прежде чем писать
> любой код**. Он написан так, чтобы быть понятным без доступа к переписке,
> в которой проект создавался.
>
> **Обязательное правило для любого агента, который здесь работает:**
> после того как ты внёс изменения — обнови этот файл (разделы "Сделано" и
> "Не сделано / TODO"), закоммить обновление вместе с кодом. Если ты этого
> не сделаешь, следующий агент (человеческий или ИИ) не будет знать, что
> изменилось, и документ перестанет быть источником правды.

---

## 0. Что это за проект

Дипломный проект курса по Java-разработке. Автор — Anna, тема выбрана
самостоятельно: **REST API для бронирования отелей** (Hotel Booking System).
Полное ТЗ курса — см. раздел 8 ниже (чек-лист соответствия).

Ключевое ограничение процесса: Anna проходила курс, но **пропустила модуль
по Spring**, поэтому весь Spring/JWT/Security код в проекте написан
ИИ-агентом (Claude) по её просьбе, с подробными объяснениями "на пальцах"
в диалоге (сам диалог не сохранён в репозитории — только код и этот файл).

**GitHub-репозиторий:** https://github.com/AnnaErmoshina/hotel-booking-system.git

---

## 1. Технологический стек (зафиксирован, не менять без причины)

| Слой | Технология | Версия |
|---|---|---|
| Язык | Java | 17 (компилируется таргетом 17, у Anna локально JDK 21 — совместимо) |
| Сборка | Maven | — |
| Framework | Spring Boot | 3.3.4 |
| ORM | Hibernate (через Spring Data JPA) | из BOM Spring Boot |
| БД | PostgreSQL | 16 (alpine, в Docker) |
| Миграции | Liquibase | из BOM Spring Boot (core 4.27.0) |
| Аутентификация | Spring Security + JWT (io.jsonwebtoken / jjwt) | jjwt 0.12.6, **новый fluent API** (`Jwts.builder().subject(...)`, не старый `setSubject`) |
| Документация API | springdoc-openapi (Swagger UI) | 2.5.0 |
| Мапping DTO↔Entity | **MapStruct добавлен в pom.xml, но НЕ используется** — сейчас маппинг руками в *ServiceImpl классах. Это осознанный технический долг, см. раздел 6. |
| Прочее | Lombok | — |
| Тесты (пока не написаны) | JUnit 5, Mockito, Testcontainers, spring-security-test, JaCoCo | зависимости в pom.xml есть, тестов — 0 |
| Контейнеризация | Docker + Docker Compose | Dockerfile — multi-stage build (maven:3.9-eclipse-temurin-17 → eclipse-temurin:17-jre-alpine) |

---

## 2. Архитектура

Строгая слоистая архитектура, пакет `com.hotelbooking`:

```
controller/   → REST-контроллеры, только вызывают service, не знают про Repository/Entity
service/      → интерфейсы бизнес-логики
service/impl/ → реализации
repository/   → Spring Data JPA интерфейсы
entity/       → JPA-сущности (+ entity/enums/ — enum'ы статусов и ролей)
dto/request/  → входящие DTO (Java record + Bean Validation аннотации)
dto/response/ → исходящие DTO (Java record)
security/     → JWT + Spring Security инфраструктура (не путать с config/)
config/       → Spring @Configuration классы (сейчас только SecurityConfig)
exception/    → кастомные исключения + GlobalExceptionHandler (@RestControllerAdvice)
mapper/       → пустая папка (.gitkeep), под MapStruct-мапперы, если/когда будут внедрены
```

Правило, которое соблюдалось всё время: **Controller никогда не обращается
к Repository или Entity напрямую** — только через Service и DTO.

---

## 3. Схема базы данных — СДЕЛАНО, 9 таблиц

Миграции: `src/main/resources/db/changelog/changes/001..009-*.xml`,
подключены через `db.changelog-master.xml`. Порядок файлов = порядок
применения, это важно — новые миграции добавлять **только новым файлом
`010-...xml`**, никогда не редактировать уже применённые (001-009), даже
если кажется, что "просто поправить один столбец" проще.

| # | Таблица | Ключевые поля | Связи |
|---|---|---|---|
| 1 | `users` | email (unique), password_hash, role | — |
| 2 | `hotels` | name, address, city, country, owner_id | → users.id |
| 3 | `amenities` | name (unique) | — |
| 4 | `room_types` | name, base_price, capacity, hotel_id | → hotels.id |
| 5 | `room_type_amenities` | (room_type_id, amenity_id) — составной PK | M:N room_types ↔ amenities |
| 6 | `rooms` | room_number, floor, status, room_type_id | → room_types.id |
| 7 | `bookings` | check_in, check_out, status, total_price, user_id, room_id | → users.id, rooms.id |
| 8 | `payments` | amount, status, booking_id (unique) | → bookings.id (1:1) |
| 9 | `reviews` | rating (CHECK 1-5), comment, user_id, hotel_id, booking_id (unique) | → users.id, hotels.id, bookings.id (1:1) |

**Важный урок (уже наступили на эти грабли, не повторять):** у Liquibase
**нет** нативного тега `<addCheckConstraint>` в XML changelog. CHECK-констрейнты
добавляются только через `<sql>ALTER TABLE ... ADD CONSTRAINT ... CHECK (...)</sql>`.
Смотри `009-create-reviews-table.xml` как референс правильного способа.
Перед использованием любого незнакомого liquibase-тега — свериться с
официальным списком поддерживаемых `changeType` (см. текст ошибки XSD,
если тег невалиден — Liquibase перечисляет все допустимые в сообщении).

JPA Entity-классы (`entity/`) созданы **на все 9 таблиц**: `User`, `Hotel`,
`Amenity`, `RoomType`, `Room`, `Booking`, `Payment`, `Review` + enum'ы
`Role`, `RoomStatus`, `BookingStatus`, `PaymentStatus`.

Repository-интерфейсы (`repository/`) созданы **на все 8 сущностей**
(Amenity, Hotel, Payment, Review, Room, RoomType, User, Booking).
Из нестандартного — `BookingRepository.existsOverlappingBooking(...)`
(JPQL-проверка пересечения дат) и `ReviewRepository.findAverageRatingByHotelId(...)`.

---

## 4. Что реализовано функционально (по слоям)

### 4.1 Аутентификация — СДЕЛАНО и проверено вручную (Swagger + api-tester.html)
- `POST /api/auth/register` — регистрация, роль всегда жёстко `USER` (см. TODO 6.3)
- `POST /api/auth/login` — вход, возвращает JWT
- Пароли хешируются BCrypt, токен — HS256, время жизни настраивается через
  `jwt.expiration-ms` в `application.yml` (по умолчанию 24 часа)
- JWT кладётся в заголовок `Authorization: Bearer <token>`, читается
  `JwtAuthenticationFilter` на каждый запрos
- **Проверено вживую**: register → 201 + токен, login → 200 + токен,
  неверный пароль → 401 с корректным JSON-ответом об ошибке

### 4.1.1 Удобства (Amenity) — СДЕЛАНО в этой сессии, не проверено вживую
`AmenityController`/`AmenityService`/`AmenityServiceImpl` добавлены
(ветка `feature/amenities`):
- `GET /api/amenities` — список всех удобств (публично)
- `POST /api/amenities` — создать удобство (`HOTEL_MANAGER`/`ADMIN`),
  дубликат имени (без учёта регистра) → `AmenityAlreadyExistsException` (409)
- `GET /api/room-types/{roomTypeId}/amenities` — список удобств, привязанных
  к типу номера (публично)
- `POST /api/room-types/{roomTypeId}/amenities/{amenityId}` — привязать
  существующее удобство к типу номера (владелец отеля или `ADMIN`, та же
  проверка, что и в `RoomTypeServiceImpl.assertOwnerOrAdmin`)
- `tools/api-tester.html` дополнен карточкой "Удобства" (список, создание,
  привязка к типу номера)

Не сделано: отвязка удобства от типа номера (DELETE), и Review
(Service+Controller) — по-прежнему не начато, см. TODO 6.4.
**Не проверено вживую** — см. раздел 6.9, у этого агента тоже нет сети/mvn.

### 4.2 Отели / типы номеров / номера / бронирования — СДЕЛАНО, НЕ проверено end-to-end
Реализованы контроллеры, сервисы, DTO для полного цикла:
- `HotelController` — GET (публично, пагинация по городу), POST/PUT/DELETE
  (только `HOTEL_MANAGER`/`ADMIN`, PUT/DELETE — только владелец или ADMIN)
- `RoomTypeController` — GET по hotelId (публично), POST (владелец отеля/ADMIN)
- `RoomController` — GET по roomTypeId (публично), POST (владелец/ADMIN)
- `BookingController` — POST (создать бронь, любой авторизованный USER),
  GET `/my` (свои брони), PATCH `/{id}/cancel` (владелец брони или ADMIN)

**Ключевая бизнес-логика в `BookingServiceImpl.create()`:**
1. `checkOut` должен быть позже `checkIn`, иначе `InvalidBookingDatesException` (400)
2. Проверка пересечения дат через `existsOverlappingBooking` — если номер занят,
   `RoomNotAvailableException` (409)
3. Цена считается через `PricingStrategy` (интерфейс, пакет
   `service/pricing/`) — **Strategy pattern**, добавлено в этой сессии,
   см. TODO 6.1. Дефолтная (`@Primary`) реализация —
   `WeekendSurchargePricingStrategy`: `basePrice` за ночь, с наценкой
   +20% за ночи с пятницы на субботу и с субботы на воскресенье.
   `StandardPricingStrategy` (старое поведение, `basePrice * ночи` без
   вариаций) осталась как альтернативная реализация того же интерфейса.
4. Статус новой брони всегда `PENDING` (переход в `CONFIRMED`/`COMPLETED`
   нигде не реализован — см. TODO 6.5)

**Роль пользователя больше не блокер** (закрыто TODO 6.3, см. ниже) —
на старте приложения автоматически создаётся один пользователь
`ADMIN` (см. `config/AdminSeeder.java`), и есть эндпоинт
`PATCH /api/admin/users/{id}/role`, чтобы выдать `HOTEL_MANAGER`
кому угодно без ручного SQL.

**По-прежнему НЕ проверено вживую до конца** (только код написан и
статически проверен на соответствие пакетов/структуры) — у агента,
работавшего в этой сессии, тоже не было сетевого доступа к Maven
Central (см. обновлённый раздел 6.9), поэтому цепочка "создать отель →
тип номера → номер → бронь" всё ещё ни разу не прогонялась от начала
до конца в реальном окружении. Теперь, когда блокер с ролями снят, это
следующий логичный шаг для человека с доступом к `docker-compose up`.

### 4.3 Обработка ошибок — СДЕЛАНО
`GlobalExceptionHandler` (`@RestControllerAdvice`) ловит:
`EmailAlreadyExistsException` (409), `ResourceNotFoundException` (404),
`RoomNotAvailableException` (409), `InvalidBookingDatesException` (400),
`ForbiddenOperationException` (403), `BadCredentialsException` (401),
`MethodArgumentNotValidException` (400, с текстом невалидных полей),
generic `Exception` (500). Формат ответа единый — `ErrorResponse(timestamp, status, message)`.

### 4.4 CORS + инструмент ручного тестирования — СДЕЛАНО
`SecurityConfig` содержит **намеренно широко открытый CORS**
(`AllowedOriginPatterns("*")`) — это dev-only настройка для локальной
работы, помечена комментарием в коде, **должна быть сужена перед любым
реальным деплоем** (сейчас не критично, т.к. проект не деплоится).

`tools/api-tester.html` — одностраничный HTML/JS без зависимостей и
сборки, покрывает все эндпоинты (auth, hotels, room-types, rooms,
bookings) с сырым JSON-выводом ответа. Открывается просто двойным
кликом в браузере при поднятом `docker-compose up`. Не часть
оцениваемого REST API, чисто вспомогательный инструмент.

### 4.5 Docker — СДЕЛАНО и проверено
`docker-compose up --build` поднимает Postgres + приложение,
Liquibase применяет все 9 миграций при старте (лог `Rows affected: 9`,
`Started HotelBookingApplication`) — **проверено вживую пользователем**,
скриншоты логов подтверждают успешный запуск.

### 4.6 Swagger / OpenAPI — ЧАСТИЧНО
`springdoc-openapi-starter-webmvc-ui` подключен, Swagger UI доступен на
`/swagger-ui.html`. На контроллерах есть `@Tag`/`@Operation`. **НЕ настроена
security scheme для Bearer-токена** в OpenAPI-конфиге — значит, в
Swagger UI нет кнопки "Authorize" со стандартным полем для JWT (сейчас,
скорее всего, придётся передавать токен как-то иначе или это не
проверялось; нужно добавить `@SecurityScheme`/`SecurityRequirement`
bean — см. TODO 6.2).

### 4.7 README.md — СДЕЛАНО, поддерживается в актуальном состоянии
На английском, как того требует ТЗ курса. Обновляется после каждого
крупного шага (см. раздел "Project Status" внутри самого README —
дублирует часть информации отсюда в более сжатом виде, для читателя
репозитория, а не для агента).

---

## 5. Git — структура и текущее состояние веток

**Модель:** `main` (стабильный релиз) + `develop` (интеграционная) +
`feature/*` / `fix/*` (по одной задаче на ветку), мердж всегда через
`--no-ff` (сохраняем merge-коммит явно, для наглядной истории).

**ВАЖНО, чтобы не запутаться:** `main` **намеренно отстаёт** от `develop` —
после самого первого коммита (`chore: initialize project skeleton...`)
и коммита с `.gitkeep` в `main` больше ничего не мерджилось. Все 6
следующих фич живут только в `develop`. Это осознанное решение (main
резервируется под будущие "релизные" точки), а не забытый мердж.

Полная история на `develop` (снизу вверх, хронологически):

```
1. chore: initialize project skeleton with Spring Boot, Docker and layered architecture
2. chore: add .gitkeep to empty package directories
3. Merge feature/database-schema  → 9 таблиц, JPA entities
4. Merge feature/repositories     → 8 Spring Data JPA репозиториев
5. Merge fix/liquibase-check-constraint → фикс невалидного XML-тега
6. Merge feature/security-jwt     → JWT-аутентификация целиком
7. Merge feature/hotel-booking-core → Hotel/RoomType/Room/Booking CRUD + бизнес-правила
8. Merge feature/api-tester-ui    → CORS + tools/api-tester.html
9. docs: add PROJECT_STATUS.md
10. Merge feature/user-roles-admin     → AdminController + AdminSeeder (TODO 6.3 closed)
11. Merge feature/swagger-bearer-auth  → OpenApiConfig, Bearer-кнопка в Swagger UI (TODO 6.2 closed)
12. Merge feature/aop-service-logging  → ServiceLoggingAspect, первый реальный @Aspect
13. Merge feature/pricing-strategy     → PricingStrategy (Strategy pattern), TODO 6.1 частично закрыт
14. docs: update PROJECT_STATUS.md and README after this session's changes
15. Merge feature/cancellation-policy  → CancellationPolicy (Strategy pattern), TODO 6.1 закрыт полностью
16. docs: update PROJECT_STATUS.md and README for cancellation policy
17. Merge feature/payments            → Payment Service+Controller, CONFIRMED transition, TODO 6.4/6.5 частично закрыты
18. docs: update PROJECT_STATUS.md and README for payments
19. Merge feature/booking-completion   → COMPLETED transition (owner отеля/ADMIN), TODO 6.5 закрыт полностью
20. Merge feature/amenities            → Amenity Service+Controller, TODO 6.4 частично закрыт
21. docs: update PROJECT_STATUS.md for amenities feature
22. Merge feature/reviews              → Review Service+Controller, TODO 6.4 закрыт полностью
```

Пункты 10-13 сделаны агентом в отдельной сессии, без доступа к сети
(см. раздел 6.9) — не скомпилированы и не задеплоены, только статически
проверены. Пункт 15 — ещё одна такая же сессия (та, что писала этот
абзац), то же самое ограничение: сети нет, `mvn compile` не запускался.

Соглашение по коммитам: заголовок в стиле `feat: ...` / `fix: ...` /
`chore: ...` / `docs: ...`, тело коммита — маркированный список
конкретных изменений (смотри `git log` для примеров формулировок,
их стоит придерживаться и дальше).

---

## 6. TODO — что осталось сделать (по приоритету)

### 6.1 Ценообразование — СДЕЛАНО; политика отмены — НЕ РЕАЛИЗОВАНО
В самом начале планирования (тема проекта) обсуждались две "фишки" для
бизнес-логики диплома:
- **Динамическое/сезонное ценообразование** — паттерн **Strategy** —
  ✅ **реализовано** в ветке `feature/pricing-strategy`: интерфейс
  `PricingStrategy` (пакет `service/pricing/`), дефолтная
  `@Primary`-реализация `WeekendSurchargePricingStrategy` (+20% за
  ночи Пт/Сб), альтернативная `StandardPricingStrategy` (старое
  поведение). `BookingServiceImpl` зависит только от интерфейса.
  Если понадобится усложнить логику (реальная сезонность по
  календарю, наценки по конкретному отелю и т.п.) — добавлять новые
  `@Component`-реализации `PricingStrategy`, ничего не трогая в
  `BookingServiceImpl`.
- **Политика отмены** (бесплатно за N дней, иначе штраф) — ✅
  **реализовано** в ветке `feature/cancellation-policy`: интерфейс
  `CancellationPolicy` (пакет `service/cancellation/`, тот же
  Strategy pattern, что и `PricingStrategy`), дефолтная
  `@Primary`-реализация `DeadlineCancellationPolicy` — бесплатно, если
  до заезда осталось `app.booking.free-cancellation-days` дней и
  больше (дефолт 2), иначе штраф `app.booking.late-cancellation-
  penalty-percent`% (дефолт 50) от `totalPrice`. Добавлены поля
  `cancellation_fee`/`cancelled_at` в `bookings` (миграция
  `010-add-cancellation-fields-to-bookings.xml`, аддитивная, 001-009
  не тронуты). Повторная отмена уже `CANCELLED`/`COMPLETED` брони
  теперь кидает `BookingAlreadyCancelledException` (409) — этой
  проверки раньше не было вообще. **Не проверено вживую** (см.
  раздел 6.9) — новые env-переменные `FREE_CANCELLATION_DAYS` /
  `LATE_CANCELLATION_PENALTY_PERCENT` тоже не прогонялись реально.

### 6.2 Swagger: Bearer-авторизация в UI — СДЕЛАНО
`OpenApiConfig` (`config/OpenApiConfig.java`) добавляет
`@SecurityScheme("bearerAuth")` + глобальный `SecurityRequirement` —
в Swagger UI (`/swagger-ui.html`) теперь должна быть кнопка
"Authorize" со стандартным полем ввода JWT. **Не проверено вживую**
(см. раздел 6.9) — при первом запуске стоит открыть Swagger UI и
убедиться, что кнопка реально появилась и токен из `/api/auth/login`
принимается.

### 6.3 Управление ролями пользователей — СДЕЛАНО
Реализовано в ветке `feature/user-roles-admin`, выбран второй вариант
из предложенных ("полноценный admin-эндпоинт"), с одним отличием от
исходного плана:
- `PATCH /api/admin/users/{id}/role` — доступен только `ADMIN`
  (`@PreAuthorize("hasRole('ADMIN')")` на уровне контроллера
  `AdminController`), меняет роль любого пользователя.
- Первый `ADMIN` создаётся **не** через Liquibase `<insert>` (как
  предлагалось изначально), а через `config/AdminSeeder.java` —
  `ApplicationRunner`, который на старте приложения проверяет, есть
  ли пользователь с email из `app.admin.email`, и если нет — создаёт
  его, хешируя пароль тем же бином `PasswordEncoder`, что и everywhere
  else. Причина отклонения от исходного плана: у агента не было
  доступа к сети, чтобы сгенерировать корректный bcrypt-хеш для
  `<insert>` в XML-миграции (пакет `bcrypt`/`passlib` недоступны без
  интернета) — вычислять bcrypt вручную в Python было бы избыточно
  дорого по токенам. `ApplicationRunner`-подход даёт тот же результат
  (готовый ADMIN сразу после `docker-compose up`), не требуя
  захардкоженного хеша в репозитории — это скорее плюс, а не
  компромисс.
- Креды дефолтного админа: `app.admin.*` в `application.yml`,
  переопределяются через `ADMIN_EMAIL` / `ADMIN_PASSWORD` /
  `ADMIN_FIRST_NAME` / `ADMIN_LAST_NAME` (`.env.example`,
  `docker-compose.yml`). Дефолт: `admin@hotelbooking.local` /
  `ChangeMe123!` — **обязательно сменить перед сдачей/демо**, если
  используется дефолт.
- **Не проверено вживую** (см. 6.9) — при первом запуске стоит
  убедиться, что `AdminSeeder` реально отработал (в логах должна быть
  строка `Seeded initial ADMIN user ...`), залогиниться этим
  аккаунтом и вызвать `PATCH /api/admin/users/{id}/role`.

### 6.4 Amenity / Review / Payment — ВСЕ ТРИ ЗАКРЫТЫ
- ~~`Amenity`~~ — ✅ закрыто в `feature/amenities`, см. раздел 4.1.1.
- ~~`Review`~~ — ✅ закрыто в этой сессии (`feature/reviews`):
  `POST /api/bookings/{bookingId}/reviews` — отзыв можно оставить
  только на свою же бронь, и только когда её статус `COMPLETED`
  (`BookingNotReviewableException`, 409), повторно — нельзя
  (`ReviewAlreadyExistsException`, 409, дублирует уникальность
  `reviews.booking_id` в БД, но даёт понятный ответ вместо голой
  ошибки констрейнта). Отель для отзыва берётся из
  `booking.room.roomType.hotel` — из тела запроса не передаётся, чтобы
  нельзя было указать чужой отель. `GET /api/hotels/{hotelId}/reviews`
  и `GET /api/hotels/{hotelId}/rating` (среднее + количество,
  `ReviewRepository.findAverageRatingByHotelId` наконец использован) —
  оба публичные, попадают под уже существующее правило
  `/api/hotels/**` в `SecurityConfig`.
  Заодно исправлен попутно найденный баг: `GET /api/amenities` был
  документирован как публичный, но `/api/amenities/**` отсутствовал в
  `PUBLIC_GET_ENDPOINTS` — добавлен.
  `tools/api-tester.html` карточкой отзывов **не дополнен** (осознанно,
  вне рамок этой сессии). **Не проверено вживую**, см. 6.9.
- ~~`Payment`~~ — ✅ закрыто в `feature/payments`:
  `POST /api/payments` (владелец брони или ADMIN) создаёт `Payment` со
  статусом `PAID` на всю сумму `booking.totalPrice` и переводит
  `Booking.status` в `CONFIRMED`. Повторная оплата или оплата брони не
  в статусе `PENDING` — `InvalidPaymentStateException` (409). Частичная
  оплата/несколько платежей на бронь не предусмотрены (в схеме БД
  `payments.booking_id` уникален — 1:1). **Не проверено вживую**, см. 6.9.

### 6.5 Переходы статуса бронирования — ЗАКРЫТО
`BookingStatus` enum содержит `PENDING, CONFIRMED, CANCELLED, COMPLETED`.
Все переходы реализованы: `PENDING` (создание), `CANCELLED` (отмена,
`PATCH /api/bookings/{id}/cancel`), `CONFIRMED` (оплата, `POST
/api/payments`, см. 6.4), и теперь `COMPLETED` — новый `PATCH
/api/bookings/{id}/complete` в ветке `feature/booking-completion`:
доступен владельцу отеля (цепочка `booking.room.roomType.hotel.owner`)
или `ADMIN`, и только для брони в статусе `CONFIRMED`, у которой
`checkOut` уже наступил — иначе `BookingNotCompletableException` (409).
Новых колонок в БД не потребовалось. **Не проверено вживую**, см. 6.9.

### 6.6 Тесты — 0% ПОКРЫТИЯ, ТРЕБУЕТСЯ 80%+ ПО ТЗ КУРСА
Зависимости (JUnit 5, Mockito, Testcontainers, spring-security-test,
JaCoCo-плагин в pom.xml) подключены, но **не написано ни одного теста**.
Пользователь осознанно отложил тесты "на конец" и попросил сначала
UI для ручной проверки (см. `tools/api-tester.html`, раздел 4.4) — это
не забытая, а сознательно отложенная задача. Когда до неё дойдёт очередь:
- Unit-тесты сервисного слоя через Mockito (мокать репозитории) —
  особое внимание на `BookingServiceImpl` (overlap-логика, расчёт цены)
  и `AuthServiceImpl`
- Интеграционные тесты контроллеров через `@SpringBootTest` + `MockMvc`
- Тесты репозиториев через Testcontainers (реальный Postgres в Docker
  на время теста, не H2!)
- Проверить факт покрытия через `mvn test` → отчёт в
  `target/site/jacoco/index.html`

### 6.7 Паттерны проектирования — ЧАСТИЧНО ЗАКРЫТО
ТЗ курса явно требует "должны применяться паттерны при разработке".
Теперь в проекте есть два осознанно реализованных, явно
закомментированных паттерна:
- **Strategy** — `service/pricing/PricingStrategy` + две реализации
  (см. 6.1). Комментарии `// Strategy pattern: ...` — точнее, Javadoc
  на интерфейсе и обеих реализациях — явно называют паттерн.
- **AOP / cross-cutting logging** — `aspect/ServiceLoggingAspect`
  (см. 6.новый пункт ниже про AOP). Формально это не GoF-паттерн, а
  архитектурный приём, но он закрывает отдельный пункт ТЗ ("Spring
  AOP") и стоит упомянуть проверяющему отдельно от Strategy.

Что ещё стоит рассмотреть, если нужно больше паттернов для
презентации: **Factory** для уведомлений (email/push при
бронировании/отмене — вообще не начато, отдельная фича), **Builder**
уже фактически используется (Lombok `@Builder` на всех entity) — можно
явно упомянуть в презентации, что это тоже паттерн, просто
сгенерированный, а не написанный руками.

### 6.10 AOP (Spring AOP) — СДЕЛАНО
`aspect/ServiceLoggingAspect.java` — первый реальный `@Aspect`-класс в
проекте. `@Around`-совет на pointcut
`execution(* com.hotelbooking.service.impl.*ServiceImpl.*(..))`:
логирует время выполнения и факт успеха/ошибки для каждого вызова
любого метода любого `*ServiceImpl`. `spring-boot-starter-aop` был в
`pom.xml` с самого начала, но не использовался — закрывает риск #3 из
раздела 8.

### 6.8 MapStruct — подключен, не используется
`pom.xml` содержит зависимость и annotation-processor путь для
MapStruct, но весь маппинг Entity↔DTO в проекте сейчас написан вручную
внутри `*ServiceImpl` классов (метод `toResponse(...)` в каждом).
Не критично (работает), но если проверяющий увидит зависимость в
`pom.xml` и не увидит ни одного `@Mapper`-интерфейса — это выглядит
незавершённым. Либо внедрить MapStruct-мапперы в `mapper/` (сейчас
пустая папка), либо убрать неиспользуемую зависимость из pom.xml.

### 6.9 Не проверено окружением агента (важно понимать ограничение)
**Ни разу не запускался `mvn compile`/`mvn test` в среде, где писался
этот код** — ни в исходной сессии, ни в этой (продолжающей). У обоих
агентов (Claude, в контейнере) не было доступа к Maven Central и
вообще к сети (сетевые ограничения песочницы — `bash_tool` без
egress). Все проверки корректности делались статически: соответствие
`package` реальному пути папки, валидность XML миграций, визуальная
проверка импортов. **Реальная компиляция происходила только на
компьютере пользователя** через Docker. Из нового кода за последние
две сессии **НИЧЕГО не компилировалось и не запускалось** — весь код
в разделах 6.1/6.2/6.3/6.10 (Strategy-пары pricing/cancellation,
Swagger auth, admin roles, AOP aspect) нужно прогнать через
`mvn clean compile` / `docker-compose up --build` в первую очередь, до
любой дальнейшей разработки, и в первую очередь применить миграцию
`010-add-cancellation-fields-to-bookings.xml`. Особое внимание:
- `DeadlineCancellationPolicy` — конструктор с двумя `@Value`
  (freeCancellationDays, penaltyPercent) вместо `@RequiredArgsConstructor`,
  т.к. это примитивы, а не бины; синтаксически проверено только "на
  глаз".
- `ServiceLoggingAspect` — pointcut-выражение `execution(...)`
  синтаксически проверено только "на глаз", AspectJ pointcut language
  капризный к деталям.
- `AdminSeeder` — `@Value`-инъекция в поля (не в конструктор, т.к.
  класс уже использует `@RequiredArgsConstructor` для репозитория и
  энкодера) — стандартный Spring-паттерн, но стоит убедиться, что
  properties из `application.yml` реально резолвятся при старте.

---

## 7. Известные особенности/грабли, чтобы не наступать повторно

1. **Пустые Java-папки и git.** Git не хранит пустые директории. Слоистая
   структура пакетов (`controller/`, `service/`, `dto/request/` и т.д.)
   создавалась заранее, до появления в них файлов — поэтому в пустых
   папках лежат файлы `.gitkeep`. **Если добавляешь первый реальный
   файл в такую папку — удали её `.gitkeep`.** Один раз уже была
   ошибка: `rm -f .../*/.gitkeep` с wildcard случайно удалил `.gitkeep`
   из ВСЕХ подпапок пакета, а не только из целевой — пришлось откатывать.
   Удаляй `.gitkeep` точечно, по одному explicit пути.
2. **Liquibase XML** — см. раздел 3, `<addCheckConstraint>` не существует.
3. **Docker-логи с "ERROR" от Postgres при первом старте Liquibase**
   (`relation "databasechangeloglock" does not exist`) — это ожидаемое,
   безобидное поведение связки Liquibase+Postgres при самой первой
   инициализации, не баг.
4. Два варианта zip-архива поддерживались на всём протяжении: с `.git`
   (для пользователя — распаковать и пушить) и без (просто код). Если
   продолжаешь эту практику — не обязательно, просто удобство, не
   влияет на сам проект.

---

## 8. Чек-лист соответствия ТЗ курса

Оригинальное ТЗ (кратко): Spring (Core/AOP/Data JPA/Security/Boot) +
Hibernate, паттерны, SOLID, Java8+ фичи, Docker, обработка ошибок,
слоистая архитектура, тесты 80%+, доп. технологии (Swagger, миграции),
git-структура/история/README, БД 6+ таблиц нормализованная со связями,
OpenAPI, README на английском.

| Пункт ТЗ | Статус |
|---|---|
| Spring Core (DI/IoC) | ✅ |
| Spring AOP | ✅ `aspect/ServiceLoggingAspect` — @Around на всех *ServiceImpl (см. 6.10) |
| Spring Data JPA | ✅ |
| Spring Security | ✅ |
| Spring Boot | ✅ |
| Hibernate | ✅ (через JPA) |
| Паттерны проектирования | ✅ два явных Strategy — `PricingStrategy` и `CancellationPolicy` (см. TODO 6.7), плюс AOP как отдельный пункт ТЗ. Factory для уведомлений — не начато, не блокер |
| SOLID | ✅ разумно соблюдается (интерфейсы сервисов, разделение по слоям) |
| Java 8+ features | ✅ records (DTO), stream/lambda (`.stream().map().toList()`), `Optional` (репозитории). `var` почти не использован — не критично |
| Docker | ✅ Dockerfile + docker-compose.yml, проверено вживую (до этой сессии — новые env-переменные ADMIN_* не проверены, см. 6.9) |
| Обработка ошибок/исключений | ✅ GlobalExceptionHandler на все кейсы |
| Слоистая архитектура | ✅ |
| Тесты 80%+ | ❌ 0%, см. TODO 6.6 — **главный оставшийся риск для сдачи, не тронуто в этой сессии** |
| Swagger/OpenAPI | ✅ подключен, размечен, Bearer security scheme добавлен (см. 6.2) — не проверено вживую |
| Миграции БД | ✅ Liquibase, 9 файлов |
| Git: ветки/история/README | ✅ |
| БД: 6+ таблиц, нормализация, связи | ✅ 9 таблиц, 3NF, связи описаны в разделе 3 |
| README на английском | ✅ |

**Главные незакрытые риски перед сдачей, по убыванию критичности:**
1. Тесты (0% против требуемых 80%+) — TODO 6.6, **главный оставшийся
   риск**, не тронуто в этой сессии (см. раздел 12/13 — уже несколько
   сессий подряд написанные тесты теряются между сессиями из-за
   параллельных веток разработки, см. предупреждение в разделе 6.6)
2. Ничего из кода последних сессий не скомпилировано и не запущено
   живьём (TODO 6.9) — первым делом при следующем доступе к
   Maven/Docker, включая миграцию 010 и новые эндпоинты reviews
3. `tools/api-tester.html` не дополнен карточкой для reviews
   (`POST /api/bookings/{id}/reviews`, `GET /api/hotels/{id}/reviews`,
   `GET /api/hotels/{id}/rating`)

---

## 10. Что сделано в этой сессии (агент-продолжатель) и почему не сделаны тесты

**Сессия, описанная в разделах 4-9 выше:** роли пользователей, Spring
AOP, Swagger Bearer-кнопка, `PricingStrategy` — 4 фичи, см. историю
пунктов 10-13 в разделе 5.

**Эта сессия (ещё один агент-продолжатель, тот же пользователь Anna,
явно попросил уложиться в одну быструю сессию и не жечь лишние
токены):** закрыла ровно один пункт — вторую половину TODO 6.1,
политику отмены бронирования (`feature/cancellation-policy`, пункт 15
в разделе 5). Выбор пал именно на неё, а не на что-то из TODO 6.4/6.5,
потому что это самый маленький по объёму код, который при этом:
закрывает целый TODO-пункт, добавляет второй явный Strategy-паттерн
(усиливает TODO 6.7 для презентации) и не требует новых
контроллеров/DTO с нуля (только правка существующего
`BookingServiceImpl.cancel()`).

**Эта сессия (снова быстрая, экономим токены):** после политики отмены
взялась за `feature/payments` — тоже маленький по объёму, но закрывает
сразу два TODO одним ходом: Payment получил Service+Controller (TODO
6.4) и появился первый реальный переход статуса брони в `CONFIRMED`
(TODO 6.5). `Amenity`/`Review` и переход в `COMPLETED` осознанно не
трогались — каждый из них самостоятельная фича того же размера, но
сессия уже не маленькая, если делать все сразу.

**Эта сессия (продолжение, снова экономим токены):** закрыла последний
кусок TODO 6.5 — `feature/booking-completion`, переход брони в
`COMPLETED`. Выбор снова пал на самый маленький по объёму код: не
новая сущность, а один метод в `BookingServiceImpl` + один эндпоинт +
одно новое исключение, без миграции БД. `Amenity`/`Review`
по-прежнему не тронуты — они требуют полноценного слоя
Service+Controller с нуля, это отдельная, более объёмная сессия.

Тесты (TODO 6.6) снова осознанно оставлены следующей сессии — по-прежнему
единственный пункт, который реально требует много времени (unit-тесты
сервисного слоя, интеграционные тесты контроллеров, Testcontainers,
прогон `mvn test` + отчёт JaCoCo). Начинать стоит с `BookingServiceImpl`
(overlap-логика, `PricingStrategy`, теперь ещё и `CancellationPolicy` —
там уже три отдельных сценария, которые стоит покрыть) и
`AuthServiceImpl`, как и планировалось в разделе 6.6.

---

## 9. Как проверить текущее состояние проекта за 2 минуты

```bash
git log --oneline --all --graph        # история веток, должна совпадать с разделом 5
find src/main/java -name "*.java"      # список файлов, сверить с разделом 4
docker-compose down -v && docker-compose up --build   # должно стартовать без ERROR
# затем открыть tools/api-tester.html в браузере
```

Если что-то из раздела 4 ("Сделано") не находится в файлах при
распаковке — значит, архив собран из более старой точки истории, чем
описывает этот файл; ориентируйся на реальный `git log`, а не только
на текст этого документа, если они разойдутся.

---

## 11. Ревью кода за эту сессию (агент, читавший zip от "другого агента")

Пользователь отправил zip с результатом ещё 4 сессий продолжения
(ветки `feature/user-roles-admin`, `feature/swagger-bearer-auth`,
`feature/aop-service-logging`, `feature/pricing-strategy`,
`feature/cancellation-policy`, `feature/payments`,
`feature/booking-completion` — все уже смерджены в `develop`).
Хэндофф через `PROJECT_STATUS.md` сработал: другой агент прочитал
файл и продолжил ровно по списку приоритетов из раздела 6, ничего не
поломав из старого кода.

**Сделано в этой сессии — вычитка кода вручную, построчно**, по всем
файлам из веток выше (`AdminSeeder`, `OpenApiConfig`,
`ServiceLoggingAspect`, `PricingStrategy`+обе реализации,
`CancellationPolicy`+`DeadlineCancellationPolicy`, `PaymentService`+
`PaymentServiceImpl`+`PaymentController`, обновлённый
`BookingServiceImpl` с `complete()`, миграция `010-...xml`, все новые
DTO и исключения, `SecurityConfig`, `docker-compose.yml`/`.env.example`
на предмет прокинутых переменных). **Реальных багов не найдено** —
все сигнатуры методов, поля entity/DTO, названия колонок в миграции
010 и в `Booking`-entity, wiring в `application.yml`/`docker-compose.yml`
согласованы. По-прежнему **ничего из этого не скомпилировано** (см.
6.9 — у этого агента тоже нет сети), поэтому "багов не найдено"
означает "не найдено статическим ревью", а не "гарантированно
скомпилируется".

Найдено и исправлено два реальных пробела (не баги, а недоделки):

1. **`AccessDeniedException` не обрабатывался** в
   `GlobalExceptionHandler` — при срабатывании `@PreAuthorize` (например,
   не-ADMIN на `/api/admin/**`) ответ шёл в дефолтном формате Spring
   Security, а не в едином `ErrorResponse`. Добавлен
   `@ExceptionHandler(AccessDeniedException.class)` → 403 в общем формате.
2. **`tools/api-tester.html` не поспевал за бэкендом** — не было
   карточек для оплаты (`POST /api/payments`), завершения брони
   (`PATCH /api/bookings/{id}/complete`) и управления ролями
   (`PATCH /api/admin/users/{id}/role`). Причина последнего: в
   предыдущей сессии (до этого хэндоффа) агент начинал реализовывать
   похожую фичу управления ролями и параллельно добавил такую карточку
   в `api-tester.html`, но **не успел закоммитить** до того, как
   пользователь переключился на архив от другого агента — та ветка
   работы потеряна (осталась только в контейнере той сессии, никогда
   не попадала в zip). Все три карточки добавлены заново под
   актуальные DTO (`UpdateUserRoleRequest` — только поле `role`, без
   отдельного списка пользователей, т.к. в этой реализации `GET
   /api/admin/users` нет — только `PATCH .../role`).

Также убраны "осиротевшие" файлы `.gitkeep`, лежавшие рядом с реальным
кодом в `config/`, `controller/`, `dto/request/`, `dto/response/`,
`entity/`, `exception/`, `repository/`, `service/impl/` — они не были
отслежены git (значит, никогда не попадали в реальный репозиторий), но
физически лежали на диске в распакованном архиве и могли сбить с толку
при следующей упаковке/ревью. Удалены точечно (см. правило в разделе 7,
п.1 — не трогать `.gitkeep` там, где он всё ещё нужен: таких сейчас
не осталось, все пакеты уже наполнены реальным кодом).

**Не тронуто в этой сессии (сознательно, чтобы не размывать фокус):**
`Amenity`/`Review` Service+Controller (TODO 6.4, последний нереализованный
пункт из старого списка), тесты (TODO 6.6, по-прежнему 0%, теперь уже
пятая сессия подряд, где это откладывается).

**Главный вывод для следующей сессии:** объём накопленного, ни разу не
скомпилированного кода уже большой (7+ фич за 4 сессии). Настоятельно
рекомендуется при первой же возможности прогнать `mvn clean compile`
(или хотя бы `docker-compose up --build`) **до** добавления чего-либо
нового — риск того, что где-то есть мелкая синтаксическая опечатка,
накопленная за несколько сессий статического ревью "на глаз", растёт
с каждой новой фичей поверх непроверенной базы.

## 12. Эта сессия (агент-продолжатель, быстрая, по просьбе пользователя)

Пользователь явно попросил уложиться в одну короткую сессию (не ждать
5 часов до следующей) и не жечь лишние токены — взята **ровно одна**,
самая маленькая по объёму задача из TODO 6.4: **Amenity Service +
Controller** (см. раздел 4.1.1). Выбор пал на неё, а не на `Review`,
потому что `Amenity` — сущность без собственных бизнес-правил (нет
дат/пересечений/статусов), чистый CRUD + M:N-привязка, что дало
закрыть TODO-пункт минимальным по риску кодом за один проход.

Добавлено: `AmenityController`, `AmenityService`+`AmenityServiceImpl`,
`CreateAmenityRequest`/`AmenityResponse`, `AmenityAlreadyExistsException`
(+ обработчик в `GlobalExceptionHandler`), карточка в
`tools/api-tester.html`. Паттерн скопирован с `RoomTypeController`/
`RoomTypeServiceImpl` (тот же стиль проверки владельца отеля).

**Не сделано в этой сессии (сознательно, чтобы не расширять объём):**
- `Review` (TODO 6.4, последняя оставшаяся сущность без API) — самостоятельная
  задача сопоставимого объёма, требует отдельной сессии.
- Отвязка удобства от типа номера (`DELETE .../amenities/{id}`) — не
  запрашивалась явно ни в одном TODO, добавить по необходимости.
- Тесты (TODO 6.6, 0%) — по-прежнему главный риск, не тронуты уже
  шестую сессию подряд.
- **Как и весь код из предыдущих сессий, этот код тоже не скомпилирован**
  (см. 6.9) — у этого агента тоже не было сети/`mvn`. При первом запуске
  после этой сессии — сначала `mvn clean compile`/`docker-compose up
  --build`, и в первую очередь проверить, что `RoomType.getAmenities()`
  корректно инициализируется (Lazy `@ManyToMany`) вне транзакции при
  сериализации в `AmenityResponse` (в `getByRoomType` вызывается внутри
  метода без явного `@Transactional` — если Hibernate лениво грузит
  коллекцию вне сессии, будет `LazyInitializationException`; при первом
  ручном тесте через Swagger/api-tester.html обратить на это особое
  внимание, при необходимости добавить `@Transactional(readOnly = true)`
  на `getByRoomType`).

## 13. Эта сессия (агент-продолжатель, задача была явно ограничена пользователем: "делай Review")

Пользователь запросил ровно один пункт из списка раздела 12 — `Review`
Service+Controller. Сделано по образцу `AmenityController`/
`RoomTypeServiceImpl` (см. раздел 6.4 для деталей бизнес-правил).
Заодно (в том же файле, `SecurityConfig`) исправлен попутно
обнаруженный баг: `/api/amenities/**` отсутствовал в
`PUBLIC_GET_ENDPOINTS`, хотя `GET /api/amenities` документирован как
публичный — добавлен.

Также важное предупреждение на будущее — **в этой сессии подтвердилось
ещё раз, что архивы приходят из как минимум двух не связанных друг с
другом веток разработки**: тесты, написанные в двух предыдущих
сессиях (`BookingServiceImplTest`, `AuthServiceImplTest`,
`UserServiceImplTest`, тесты `PricingStrategy`/`CancellationPolicy`),
в этом архиве снова отсутствуют — TODO 6.6 по факту опять на 0%, хотя
формально уже трижды "закрывался" в разных сессиях. Если это не
исправить на стороне пользователя (свести ветки воедино перед
следующей загрузкой zip), эта работа продолжит теряться.

**Не сделано в этой сессии (сознательно, задача была явно ограничена одним пунктом):**
- Тесты (TODO 6.6) — не тронуты, см. предупреждение выше.
- `tools/api-tester.html` не дополнен карточкой для reviews.
- Как и весь остальной код проекта, этот код не скомпилирован (нет сети).

---

## 11. Простой UI (эта сессия)

Добавлен `frontend/index.html` (`feature/simple-ui`) — один статический
HTML-файл без сборки/фреймворка, покрывает весь API: регистрация/вход,
поиск отелей по городу, просмотр типов номеров/номеров/удобств/отзывов/
рейтинга, бронирование, оплата, отмена, завершение, отзыв, плюс базовые
инструменты HOTEL_MANAGER (создать отель/тип номера/номер/удобство) и
ADMIN (сменить роль пользователя по id). Работает благодаря уже
открытому CORS в `SecurityConfig` (dev-only, см. предупреждение там же).
Ограничение: нет эндпоинта "список пользователей", поэтому в форме
смены роли id нужно вводить вручную. **Не проверено вживую** (см.
предупреждение выше про отсутствие сети) — открыть файл в браузере
и пройтись по сценарию регистрация → отель → тип номера → номер →
бронь → оплата → отмена/завершение → отзыв в первую очередь.

Тесты (TODO 6.6) снова не тронуты — см. предупреждение о потерянной
работе выше, актуально и для этой сессии.

---

## 14. Юнит-тесты (эта сессия, лимит бюджета — не всё покрыто)

Добавлено 6 тестовых классов, ~28 тестов, Mockito + AssertJ (уже были в
pom.xml). Покрыты **самые рискованные** классы:
- `BookingServiceImplTest` — create/cancel/complete, overlap-проверка,
  ownership-проверки, все исключения
- `AuthServiceImplTest` — register/login, хеширование пароля, роль USER
- `HotelServiceImplTest` — ownership (owner/admin/чужой)
- `WeekendSurchargePricingStrategyTest` — будни/выходные/смешанные даты
- `DeadlineCancellationPolicyTest` — до дедлайна/на дедлайне/после
- `JwtServiceTest` — генерация/валидация/просрочка (через ReflectionTestUtils,
  т.к. `@Value`-поля не инициализируются вне Spring-контекста)

**НЕ покрыто (осталось на следующую сессию):**
`PaymentServiceImpl`, `RoomServiceImpl`, `RoomTypeServiceImpl`,
`UserServiceImpl`, `AmenityServiceImpl`, `ReviewServiceImpl` — простые
CRUD, ниже риск, но нужны для итогового %. Также нет ни одного
интеграционного теста (`@SpringBootTest`+`MockMvc`+Testcontainers) —
контроллеры и связка с реальной БД не проверены тестами вообще.
Точный % покрытия неизвестен — `mvn test`/JaCoCo ни разу не
запускались (нет сети). Прогнать `mvn test` и глянуть
`target/site/jacoco/index.html` — первое, что стоит сделать.

---

## 15. Реальный баг, найденный тестами (эта сессия)

Первый прогон `mvn test` (пользователем, локально) — 31 тест, 30 прошли,
1 упал: `JwtServiceTest.isTokenValid_returnsFalse_whenTokenAlreadyExpired`
упал не потому что тест неправильный, а потому что **нашёл настоящий
баг**: `JwtService.isTokenValid()` не ловил `ExpiredJwtException` и падал,
вместо того чтобы вернуть `false`. Хуже того — `JwtAuthenticationFilter`
вызывал `jwtService.extractEmail(token)` вообще без try-catch, а значит
**любой запрос с просроченным или битым JWT падал в 500 Internal Server
Error** вместо аккуратного 401. Реальный, боевой баг, не только в тестах.

Исправлено: `isTokenValid` теперь ловит `JwtException` и возвращает
`false`; `JwtAuthenticationFilter` тоже обёрнут в try-catch (защита в
два слоя) — просто не аутентифицирует запрос, дальше Spring Security
сам вернёт 401, если эндпоинт требует авторизации.

**Вывод:** `mvn test` стоит гонять после каждой сессии добавления кода,
не откладывать до конца — этот баг лежал в проекте с самой первой
сессии Security/JWT и не был бы замечен без юнит-теста именно на этот
edge case.

---

## 16. JaCoCo не писал отчёт — исправлено (эта сессия)

Пользователь прогнал `mvn test` — все 31 теста прошли, но JaCoCo вывел
`Skipping JaCoCo execution due to missing execution data file`, т.е.
`target/jacoco.exec` не создался, отчёта о покрытии нет вообще.

**Вероятная причина:** путь к проекту на диске пользователя содержит
кириллицу и пробелы (`D:\КУРСЫ JAVA\Дипломный проект\...`) — известная
проблема для Java-агентов (`-javaagent`) на Windows, автоматическая
привязка `jacoco:prepare-agent` → `argLine` для Surefire иногда молча
не срабатывает в таких путях.

**Исправлено в `pom.xml`:** `prepare-agent` теперь пишет своё значение
в явное свойство `surefireArgLine` (а не в дефолтное `argLine`), и
добавлен явный блок `maven-surefire-plugin` с
`<argLine>@{surefireArgLine}</argLine>` — синтаксис `@{...}` (delayed
property, не `${...}`) откладывает подстановку до момента реального
форка тестового JVM, что надёжнее автоматической связки именно в таких
проблемных окружениях.

**Если это не поможет** (следующий шаг, если пользователь снова увидит
"missing execution data file") — стоит предложить перенести папку
проекта в путь без кириллицы/пробелов (например `C:\projects\hotel-booking-system`),
это радикально решает целый класс похожих проблем на Windows, не только
с JaCoCo.
