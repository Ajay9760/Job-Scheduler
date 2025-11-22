# Chronos - Professional Job Scheduler (Microservices + React)

This is a production-style implementation of **Chronos**, a distributed HTTP job scheduler with:

- **api-service** – REST API, JWT auth, job management, analytics, validation, DTOs, global exception handling.
- **scheduler-service** – DB-backed scheduler with Redis-based distributed lock, pushes jobs to RabbitMQ.
- **worker-service** – RabbitMQ consumer that executes HTTP jobs with per-job timeout, circuit breaker, and webhooks.
- **ui** – A modern React dashboard for login, job creation, filtering, and monitoring.

## Tech Stack

- Java 21, Maven, Spring Boot 3
- Spring Web, Spring Data JPA, Spring Security, Validation, Actuator
- PostgreSQL, RabbitMQ, Redis
- JWT-based authentication, BCrypt password hashing
- Flyway DB migrations
- React + Vite frontend
- JUnit 5, Mockito, Spring Boot Test

## Running the system

### 1. Start infrastructure

```bash
cd infra
docker-compose up -d
```

This starts:

- Postgres (chronos / chronos)
- RabbitMQ (AMQP on 5672, management UI on 15672)
- Redis (6379)

### 2. Build all services

From the project root:

```bash
mvn clean install
```

### 3. Run the Spring Boot services

In three separate terminals:

```bash
cd api-service
mvn spring-boot:run
```

```bash
cd scheduler-service
mvn spring-boot:run
```

```bash
cd worker-service
mvn spring-boot:run
```

### 4. Run the React UI

```bash
cd ui
npm install
npm run dev
```

Open `http://localhost:5173`.

Login with:

- **username:** `admin`
- **password:** `admin123`

### 5. Features visible to evaluator

- DTO-based controllers (no entities exposed directly).
- Proper service layer with validation and business rules.
- JPA repositories with meaningful query methods.
- Centralized `@RestControllerAdvice` handling and structured JSON errors.
- JWT authentication with BCrypt-hashed passwords and stateless security.
- Scheduler microservice using Redis lock to avoid double scheduling.
- Worker microservice with WebClient, per-job timeout, circuit breaker, and webhook callbacks.
- A polished dashboard UI with filters, status cards, and job table.

### 6. Tests

From root:

```bash
mvn test
```

In `api-service`:

- `ApiServiceApplicationTests` – context load.
- `JobServiceTest` – unit test validating service logic and mapping.
- `JobControllerTest` – Web layer test (MockMvc) verifying DTO-based endpoint.

You can extend with Testcontainers for full integration if desired.
