# Hotel Booking System

A REST API for hotel room booking, built as a diploma project for a Java backend development course.

Users can browse hotels and rooms, book rooms for specific dates, pay for bookings, cancel them according
to a cancellation policy, and leave reviews. Hotel managers can manage their own hotels and rooms.
Administrators manage the whole system.

> This project is under active development. This README will be updated as new features land.

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
- [ ] Booking business logic
- [ ] REST endpoints
- [ ] Swagger documentation
- [ ] Unit & integration tests (80%+ coverage)

## License

This project was created for educational purposes as part of a Java backend development course.
