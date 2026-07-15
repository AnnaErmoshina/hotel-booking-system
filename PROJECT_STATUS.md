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

### 6.4 Amenity / Review — Entity+Repository есть, Service+Controller НЕТ; Payment — ЗАКРЫТО
Было три сущности без бизнес-логики, теперь осталось две:
- `Amenity` — нет способа создать удобство или привязать к `RoomType`
  через API (сущность `RoomType.amenities` как `@ManyToMany` есть в
  коде, но ничего её не наполняет)
- `Review` — нет эндпоинта оставить отзыв (хотя
  `ReviewRepository.findAverageRatingByHotelId` уже готов и ждёт
  использования — это была задумка для автоматического пересчёта
  рейтинга отеля)
- ~~`Payment`~~ — ✅ **закрыто** в ветке `feature/payments`:
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
1. Тесты (0% против требуемых 80%+) — TODO 6.6, **самый большой риск,
   всё ещё не тронут** (сознательно откладывается уже третью сессию —
   см. раздел 10)
2. Ничего из кода последних трёх сессий не скомпилировано и не
   запущено живьём (TODO 6.9) — первым делом при следующем доступе к
   Maven/Docker, включая миграцию 010
3. `Amenity`/`Review` без Service/Controller (TODO 6.4, `Payment`
   теперь закрыт)
4. Переход брони в `COMPLETED` не реализован (TODO 6.5, `CONFIRMED`
   теперь закрыт через оплату)

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
