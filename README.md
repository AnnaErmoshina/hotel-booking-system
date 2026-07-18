# Hotel Booking System

A REST API for hotel room booking, built as a diploma project for a Java backend development course.

Users can browse hotels and rooms, book rooms for specific dates, pay for bookings, cancel them according
to a cancellation policy, and leave reviews. Hotel managers can manage their own hotels and rooms.
Administrators manage the whole system.

> Feature-complete for the diploma requirements: layered REST API, JWT auth with roles,
> booking business logic (availability, dynamic pricing, cancellation policy), payments,
> reviews, Docker, Liquibase migrations, Swagger, and 80%+ unit test coverage.

> **For AI agents / continuing developers:** read [`PROJECT_STATUS.md`](./PROJECT_STATUS.md)
> first — it tracks exactly what's implemented, what's not, and known gotchas.
> Keep it updated after any change.

> **For AI agents / continuing developers:** read [`PROJECT_STATUS.md`](./PROJECT_STATUS.md)
> first — it tracks exactly what's implemented, what's not, and known gotchas.
> Keep it updated after any change.

## Tech Stack

- **Java 17**
- **Spring Boot 3** (Web, Data JPA, Security, AOP, Validation)
- **Hibernate**
- **PostgreSQL**
- **Liquibase** — database migrations
- **JWT** — authentication
- **springdoc-openapi (Swagger UI)** — API documentation
- **MapStruct** — DTO ↔ entity mapping
- **Lombok**
- **JUnit 5 + Mockito** — unit testing
- **Testcontainers** — integration testing
- **JaCoCo** — test coverage reporting
- **Docker / Docker Compose**

## Architecture

The application follows a classic layered architecture:

```
Controller  →  Service  →  Repository  →  Database
    ↓             ↓
   DTO         Business logic (booking rules, pricing, cancellation policy)
```

- **controller** — REST endpoints, request validation
- **service** — business logic
- **repository** — Spring Data JPA repositories
- **entity** — JPA entities
- **dto** — request/response objects
- **mapper** — entity ↔ DTO mapping
- **exception** — custom exceptions + global exception handler
- **config** — Spring configuration (security, OpenAPI, etc.)

## Domain Model (planned)

| Table | Description |
|---|---|
| `users` | System users (guests, hotel managers, admins) |
| `hotels` | Hotels, owned by a hotel manager |
| `room_types` | Room categories per hotel (e.g. Standard, Deluxe) |
| `rooms` | Physical rooms belonging to a room type |
| `bookings` | Reservations made by users |
| `payments` | Payments linked to bookings |
| `reviews` | User reviews linked to a hotel and a completed booking |
| `amenities` | Amenities available for rooms (many-to-many with rooms) |

## Getting Started

### Prerequisites

- JDK 17+
- Docker & Docker Compose
- Maven 3.9+ (or use the included wrapper, once added)

### Run with Docker Compose

```bash
cp .env.example .env
docker-compose up --build
```

The API will be available at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Run locally (without Docker)

1. Start a PostgreSQL instance and update `.env` / environment variables accordingly.
2. Run the application:

```bash
mvn spring-boot:run
```

## Authentication

The API uses stateless JWT authentication.

| Method | Endpoint | Description | Auth required |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user (role `USER`) | No |
| POST | `/api/auth/login` | Log in, returns a JWT | No |

For any other endpoint, send the token in the header:
```
Authorization: Bearer <token>
```

## Main Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/hotels?city=` | List hotels by city (paginated) | No |
| GET | `/api/hotels/{id}` | Get hotel details | No |
| POST | `/api/hotels` | Create a hotel | HOTEL_MANAGER, ADMIN |
| PUT | `/api/hotels/{id}` | Update a hotel | Owner or ADMIN |
| DELETE | `/api/hotels/{id}` | Delete a hotel | Owner or ADMIN |
| GET | `/api/hotels/{hotelId}/room-types` | List room types of a hotel | No |
| POST | `/api/hotels/{hotelId}/room-types` | Create a room type | Hotel owner or ADMIN |
| GET | `/api/room-types/{roomTypeId}/rooms` | List rooms of a room type | No |
| POST | `/api/room-types/{roomTypeId}/rooms` | Add a room | Hotel owner or ADMIN |
| POST | `/api/bookings` | Book a room | Authenticated |
| GET | `/api/bookings/my` | List my bookings | Authenticated |
| PATCH | `/api/bookings/{id}/cancel` | Cancel a booking | Owner or ADMIN |
| POST | `/api/payments` | Pay for a booking in full (confirms it) | Owner or ADMIN |
| PATCH | `/api/bookings/{id}/complete` | Mark a past-checkout booking completed | Hotel owner or ADMIN |
| PATCH | `/api/admin/users/{id}/role` | Change a user's role | ADMIN |
| GET | `/api/amenities` | List all amenities | No |
| POST | `/api/amenities` | Create an amenity | HOTEL_MANAGER, ADMIN |
| GET | `/api/room-types/{roomTypeId}/amenities` | List amenities of a room type | No |
| POST | `/api/room-types/{roomTypeId}/amenities/{amenityId}` | Attach an amenity to a room type | Hotel owner or ADMIN |
| POST | `/api/bookings/{bookingId}/reviews` | Leave a review for a COMPLETED booking | Owner of the booking |
| GET | `/api/hotels/{hotelId}/reviews` | List reviews for a hotel | No |
| GET | `/api/hotels/{hotelId}/rating` | Average rating + review count for a hotel | No |

A single ADMIN account is created automatically on first startup (see `app.admin.*` in
`application.yml` / `ADMIN_EMAIL` and `ADMIN_PASSWORD` in `.env.example`) — log in with it and
promote other users to `HOTEL_MANAGER` or `ADMIN` via the endpoint above instead of editing the
database by hand.

## Simple UI

`frontend/index.html` is a single, dependency-free HTML/JS page that gives every
role (guest, USER, HOTEL_MANAGER, ADMIN) a real UI over the whole API: register/login,
browse hotels by city, view room types with amenities/rooms/reviews/rating, book a
room, pay, cancel, mark completed, leave a review, plus basic manager/admin tools.
Just open `frontend/index.html` in a browser while the app is running — see
`frontend/README.md` for details.

## Manual Testing Tool

`tools/api-tester.html` is a single, dependency-free HTML page for manually exercising every
endpoint (register/login, admin/users, hotels, bookings) without touching Swagger or curl.
It always points at `http://localhost:8080` (hardcoded — this is a local dev tool, not a
configurable client). Browsing hotels is a two-screen flow: the hotel list is real cards
(not a raw JSON dump), and clicking "Посмотреть номера" on a hotel opens a separate screen
for that hotel's room types/rooms, with room-type/room creation forms and a quick "Забронировать"
action on each room.
Just open the file directly in a browser while the app is running via Docker Compose — no build
step needed. It is a convenience tool only, not part of the graded REST API itself.

## Running Tests

```bash
mvn test
```

Coverage report will be generated at `target/site/jacoco/index.html`.

## Project Status

- [x] Project skeleton & layered architecture
- [x] Base Spring Boot / Maven configuration
- [x] Docker & Docker Compose setup
- [x] Database schema & Liquibase migrations
- [x] JPA entities
- [x] Repositories
- [x] Authentication & authorization (JWT, roles)
- [x] Booking business logic (date-overlap validation, price calculation)
- [x] REST endpoints (hotels, room types, rooms, bookings)
- [x] Admin role management (`PATCH /api/admin/users/{id}/role`) + seeded initial admin
- [x] Dynamic pricing via Strategy pattern (weekend surcharge)
- [x] AOP logging aspect for the service layer
- [x] Swagger "Authorize" button (Bearer JWT security scheme)
- [x] Cancellation policy (free up to N days before check-in, penalty otherwise)
- [x] Payments (confirms a booking on successful payment)
- [x] Booking lifecycle complete (PENDING → CONFIRMED → COMPLETED, or CANCELLED)
- [x] Amenities (catalog + attach to room types)
- [x] Reviews (leave a review for a completed booking, hotel rating)
- [x] Unit tests (80%+ coverage, 99 tests — see PROJECT_STATUS.md for what's covered)

## License

This project was created for educational purposes as part of a Java backend development course.
