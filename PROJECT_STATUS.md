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
3. Цена = `basePrice * количество_ночей` (примитивный расчёт, без сезонности —
   см. TODO 6.1)
4. Статус новой брони всегда `PENDING` (переход в `CONFIRMED`/`COMPLETED`
   нигде не реализован — см. TODO 6.5)

**НЕ проверено вживую до конца** (только код написан и статически
проверен на соответствие пакетов/структуры) — конкретно цепочка
"создать отель → тип номера → номер → бронь" ни разу не прогонялась
от начала до конца, потому что упёрлись в то, что **у только что
зарегистрированного пользователя роль всегда `USER`**, а создавать
отель может только `HOTEL_MANAGER`. См. TODO 6.3 — это следующий
блокер для ручного тестирования.

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
```

Соглашение по коммитам: заголовок в стиле `feat: ...` / `fix: ...` /
`chore: ...` / `docs: ...`, тело коммита — маркированный список
конкретных изменений (смотри `git log` для примеров формулировок,
их стоит придерживаться и дальше).

---

## 6. TODO — что осталось сделать (по приоритету)

### 6.1 Ценообразование и политика отмены — НЕ РЕАЛИЗОВАНО
В самом начале планирования (тема проекта) обсуждались две "фишки" для
бизнес-логики диплома:
- **Динамическое/сезонное ценообразование** — паттерн **Strategy**
  (разные стратегии расчёта цены). Сейчас цена = `basePrice * ночи`,
  без вариаций. Это единственное существенное расхождение с
  изначальным планом темы — стоит либо реализовать, либо явно
  отметить в презентации/README как "не вошло в MVP".
- **Политика отмены** (бесплатно за N дней, иначе штраф) — сейчас
  `BookingServiceImpl.cancel()` просто ставит статус `CANCELLED`
  безусловно, никакой логики штрафа/дедлайна нет.

Если делать — заводить новую ветку `feature/pricing-strategy` и
`feature/cancellation-policy`, паттерн Strategy — явный кандидат для
пункта ТЗ "должны применяться паттерны проектирования" (сейчас в
проекте паттерны представлены слабо — по сути только DI/IoC от
Spring и `@Builder` от Lombok; **это стоит усилить**, см. 6.7).

### 6.2 Swagger: Bearer-авторизация в UI — НЕ РЕАЛИЗОВАНО
Добавить `@Configuration`-бин с `OpenAPI` + `SecurityScheme` (bearerFormat
JWT), чтобы в Swagger UI появилась кнопка "Authorize" со стандартным
полем ввода токена. Небольшая, но важная для удобства демонстрации
задача.

### 6.3 Управление ролями пользователей — НЕ РЕАЛИЗОВАНО, ТЕКУЩИЙ БЛОКЕР
Сейчас единственный способ получить роль `HOTEL_MANAGER` или `ADMIN` —
руками поправить `role` в таблице `users` через SQL-клиент. Это
блокирует ручное end-to-end тестирование через `api-tester.html`/Swagger.
Варианты решения (не выбран ни один — решить с пользователем):
- временный dev-эндпоинт смены роли (быстро, но небезопасно, надо
  не забыть убрать или защитить перед сдачей)
- полноценный admin-эндпоинт `PATCH /api/admin/users/{id}/role`
  доступный только `ADMIN`, с seed-скриптом первого админа через
  Liquibase `<insert>` (правильный путь для диплома)

### 6.4 Amenity / Review / Payment — Entity+Repository есть, Service+Controller НЕТ
Три сущности из 9 таблиц не имеют бизнес-логики и REST-эндпоинтов:
- `Amenity` — нет способа создать удобство или привязать к `RoomType`
  через API (сущность `RoomType.amenities` как `@ManyToMany` есть в
  коде, но ничего её не наполняет)
- `Review` — нет эндпоинта оставить отзыв (хотя
  `ReviewRepository.findAverageRatingByHotelId` уже готов и ждёт
  использования — это была задумка для автоматического пересчёта
  рейтинга отеля)
- `Payment` — нет эндпоинта "оплатить бронь" (по смыслу должен менять
  `Booking.status` на `CONFIRMED` после успешной оплаты — этой связки
  тоже нет)

### 6.5 Переходы статуса бронирования — НЕ РЕАЛИЗОВАНО
`BookingStatus` enum содержит `PENDING, CONFIRMED, CANCELLED, COMPLETED`,
но код умеет проставлять только `PENDING` (при создании) и `CANCELLED`
(при отмене). Переходы в `CONFIRMED` (после оплаты) и `COMPLETED`
(после выезда) нигде не реализованы.

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

### 6.7 Паттерны проектирования — ТРЕБУЕТСЯ ПО ТЗ, СЕЙЧАС СЛАБО ВЫРАЖЕНО
ТЗ курса явно требует "должны применяться паттерны при разработке".
Сейчас в проекте фактически используются только DI/IoC (от Spring
Framework, не written by hand) и `@Builder` (Lombok, тоже не "ручной"
паттерн). **Нет ни одного осознанно реализованного GoF-паттерна.**
Рекомендуется при реализации 6.1 (Strategy для цены) и/или уведомлений
(Factory для разных типов уведомлений при бронировании/отмене — вообще
не начато) явно закрыть этот пункт ТЗ, и явно закомментировать в коде
"// Strategy pattern: ..." — это должно быть видно проверяющему.

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
этот код** — у агента (Claude, в контейнере) не было доступа к Maven
Central (сетевые ограничения песочницы). Все проверки корректности
делались статически: соответствие `package` реальному пути папки,
валидность XML миграций, визуальная проверка импортов. **Реальная
компиляция происходила только на компьютере пользователя** через
Docker (и там всё собиралось и запускалось успешно на момент написания
этого файла). Если новый агент имеет доступ к Maven — **первым делом
стоит прогнать `mvn clean compile` и `mvn test`**, это ещё ни разу не
делалось в CI-подобных условиях.

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
| Spring AOP | ❌ зависимость подключена (`spring-boot-starter-aop`), но ни одного `@Aspect`-класса не написано |
| Spring Data JPA | ✅ |
| Spring Security | ✅ |
| Spring Boot | ✅ |
| Hibernate | ✅ (через JPA) |
| Паттерны проектирования | ⚠️ слабо — см. TODO 6.7 |
| SOLID | ✅ разумно соблюдается (интерфейсы сервисов, разделение по слоям) |
| Java 8+ features | ✅ records (DTO), stream/lambda (`.stream().map().toList()`), `Optional` (репозитории). `var` почти не использован — не критично |
| Docker | ✅ Dockerfile + docker-compose.yml, проверено вживую |
| Обработка ошибок/исключений | ✅ GlobalExceptionHandler на все кейсы |
| Слоистая архитектура | ✅ |
| Тесты 80%+ | ❌ 0%, см. TODO 6.6 — **главный оставшийся риск для сдачи** |
| Swagger/OpenAPI | ⚠️ подключен и размечен, но без Bearer security scheme в UI, см. TODO 6.2 |
| Миграции БД | ✅ Liquibase, 9 файлов |
| Git: ветки/история/README | ✅ |
| БД: 6+ таблиц, нормализация, связи | ✅ 9 таблиц, 3NF, связи описаны в разделе 3 |
| README на английском | ✅ |

**Главные незакрытые риски перед сдачей, по убыванию критичности:**
1. Тесты (0% против требуемых 80%+) — TODO 6.6
2. Явные паттерны проектирования — TODO 6.7
3. Spring AOP не использован нигде, а заявлен в ТЗ — нужен хотя бы один
   `@Aspect` (например, логирование времени выполнения методов Service —
   эта идея уже обсуждалась при подготовке проекта, но не реализована)
4. Роли пользователей (6.3) — блокер для демонстрации, не для ТЗ напрямую

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
