# ⏰ Chronos - Distributed Job Scheduler

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green?style=for-the-badge&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?style=for-the-badge&logo=postgresql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12+-orange?style=for-the-badge&logo=rabbitmq)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)

**A powerful, enterprise-grade distributed job scheduling system built with Spring Boot**

[Features](#-features) • [Architecture](#-architecture) • [Quick Start](#-quick-start) • [API Documentation](#-api-documentation) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [Project Structure](#-project-structure)
- [Security](#-security)
- [Testing](#-testing)
- [Monitoring](#-monitoring)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Overview

**Chronos** is a distributed job scheduling system designed to handle complex scheduling requirements in microservices architectures. It provides a robust, scalable solution for executing recurring tasks, managing job lifecycles, and monitoring execution history with enterprise-grade reliability.

### Three-Service Architecture

Chronos consists of three independent, scalable microservices:

1. **API Service** (Port 8080) - REST API for job management and user authentication
2. **Scheduler Service** (Port 8083) - Monitors and triggers jobs based on cron expressions
3. **Worker Service** (Port 8081) - Executes jobs and handles webhooks

### Why Chronos?

- **🔄 Distributed Architecture**: Scale horizontally across multiple nodes
- **⚡ Separation of Concerns**: Independent services for API, scheduling, and execution
- **🎯 High Reliability**: Fault-tolerant design with automatic retries
- **🔐 Secure**: JWT-based authentication with role-based access control
- **📊 Observable**: Built-in metrics and health monitoring
- **🚀 Production-Ready**: Battle-tested patterns and best practices
- **🎨 Developer-Friendly**: Clean API design with comprehensive documentation

---

## ✨ Features

### Core Functionality
- ✅ **Flexible Job Scheduling** - Support for cron expressions and interval-based scheduling
- ✅ **Job Lifecycle Management** - Create, update, pause, resume, and delete jobs
- ✅ **Execution History** - Complete audit trail of all job executions
- ✅ **Retry Mechanisms** - Configurable retry policies with exponential backoff
- ✅ **Priority Queues** - Job prioritization for critical tasks
- ✅ **Webhook Support** - HTTP callbacks for job execution results

### Advanced Features
- 🔔 **Real-time Notifications** - Get notified on job success/failure
- 📈 **Performance Metrics** - Track success rates, execution times, and trends
- 🔄 **Automatic Recovery** - Self-healing capabilities for failed jobs
- 👥 **Multi-tenancy** - Isolated job spaces per user/organization
- 🎯 **Smart Routing** - Intelligent task distribution across worker nodes
- 📦 **Payload Management** - Support for complex job parameters
- ⏰ **Precise Scheduling** - Quartz-based scheduling engine for reliable execution

### Enterprise Features
- 🔒 **JWT Authentication** - Secure API access with token-based auth
- 👮 **Role-Based Access Control** - Fine-grained permission management
- 📊 **Health Checks** - Kubernetes-ready liveness and readiness probes
- 🐰 **Message Queue Integration** - RabbitMQ for reliable message delivery
- 📝 **Comprehensive Logging** - Structured logging with SLF4J
- 🔍 **API Versioning** - Backward-compatible API evolution

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
│                     (Web UI, Mobile Apps, CLI)                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTPS/REST
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway Layer                        │
│                  (JWT Auth, Rate Limiting, CORS)                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                ▼                         ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│    API Service (Port     │  │  Scheduler Service (Port │
│         8080)            │  │         8083)            │
│                          │  │                          │
│  • Job CRUD Operations   │  │  • Quartz Scheduler      │
│  • User Authentication   │  │  • Cron Management       │
│  • Job Validation        │  │  • Job Triggering        │
│  • Health Monitoring     │  │  • Schedule Monitoring   │
│  • Authorization         │  │  • Time-based Events     │
└──────────┬───────────────┘  └───────────┬──────────────┘
           │                              │
           │         ┌────────────────────┴──────────────┐
           │         │                                   │
           ▼         ▼                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                       RabbitMQ Message Broker                    │
│                                                                   │
│  Exchanges:                      Queues:                         │
│  • job.exchange (topic)          • job.execution.queue           │
│  • job.dlx (dead-letter)         • job.retry.queue               │
│                                  • job.dlq (dead-letter-queue)   │
└────────────────────────────┬────────────────────────────────────┘
                             │
           ┌─────────────────┴─────────────────┐
           ▼                                   ▼
┌──────────────────────────┐      ┌────────────────────────┐
│   Worker Service (Port   │      │  PostgreSQL Database   │
│         8081)            │      │                        │
│                          │      │  Tables:               │
│  • Job Execution         │◄─────┤  • jobs                │
│  • Webhook Calls         │      │  • job_instances       │
│  • Retry Management      │      │  • users               │
│  • Status Updates        │      │  • execution_history   │
│  • Result Processing     │      │  • qrtz_* (Quartz)     │
└──────────────────────────┘      └────────────────────────┘
```

### Component Breakdown

#### 1. API Service (Port 8080)
**Responsibilities:**
- Handle all client HTTP requests
- User authentication and JWT token generation
- Job CRUD operations (Create, Read, Update, Delete)
- Input validation and authorization
- Expose health and metrics endpoints

**Key Components:**
- REST Controllers (`JobController`, `AuthController`)
- Security Layer (JWT filters, authentication)
- Service Layer (Business logic)
- Repository Layer (Database access)

#### 2. Scheduler Service (Port 8083)
**Responsibilities:**
- Monitor scheduled jobs from the database
- Trigger jobs at specified times using Quartz Scheduler
- Send job execution messages to RabbitMQ
- Handle cron expression parsing and validation
- Manage job schedules dynamically

**Key Components:**
- Quartz Scheduler Engine
- Job Trigger Listeners
- Cron Expression Parser
- Schedule Synchronization (DB to Quartz)
- RabbitMQ Producer

**How it Works:**
1. Polls database for active scheduled jobs
2. Registers jobs with Quartz based on cron expressions
3. When job time arrives, creates execution message
4. Publishes message to `job.execution.queue`
5. Updates job metadata (last run, next run)

#### 3. Worker Service (Port 8081)
**Responsibilities:**
- Consume job execution messages from RabbitMQ
- Execute webhook calls with job payload
- Handle retries with exponential backoff
- Update job instance status and execution history
- Process job results and error handling

**Key Components:**
- RabbitMQ Consumer/Listener
- Webhook Executor (HTTP client)
- Retry Handler
- Job Instance Manager
- Result Processor

**Execution Flow:**
1. Listens to `job.execution.queue`
2. Receives job execution message
3. Creates `JobInstance` record
4. Executes webhook with payload
5. Records result (success/failure)
6. Updates job statistics
7. Handles retries if needed

### Data Flow

```
1. User creates job via API Service
2. API Service saves job to PostgreSQL
3. Scheduler Service detects new job
4. Scheduler registers job with Quartz
5. At scheduled time, Scheduler publishes to RabbitMQ
6. Worker Service consumes message
7. Worker executes webhook
8. Worker updates execution history
9. User queries results via API Service
```

### Service Communication

- **API ↔ Database**: Direct JDBC connection
- **Scheduler ↔ Database**: Reads jobs, updates schedules
- **Scheduler → RabbitMQ**: Publishes execution messages
- **RabbitMQ → Worker**: Message delivery
- **Worker ↔ Database**: Updates execution results

---

## 🛠️ Tech Stack

### Backend
- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.2+** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database abstraction layer
- **Spring AMQP** - RabbitMQ integration
- **Quartz Scheduler** - Enterprise job scheduling

### Database & Messaging
- **PostgreSQL 15+** - Primary database
- **RabbitMQ 3.12+** - Message broker
- **Flyway** - Database migration management

### Development Tools
- **Maven** - Build automation
- **Lombok** - Boilerplate code reduction
- **MapStruct** - Object mapping
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework

### Observability
- **SLF4J + Logback** - Logging
- **Spring Actuator** - Health checks and metrics
- **Micrometer** - Application metrics

---

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

| Tool | Version | Purpose |
|------|---------|---------|
| **Java JDK** | 21+ | Runtime environment |
| **Maven** | 3.8+ | Build tool |
| **PostgreSQL** | 15+ | Database |
| **RabbitMQ** | 3.12+ | Message broker |
| **Docker** (Optional) | 20+ | Containerization |
| **Postman** (Optional) | Latest | API testing |

### Quick Version Check

```bash
java --version        # Should show Java 21+
mvn --version         # Should show Maven 3.8+
psql --version        # Should show PostgreSQL 15+
docker --version      # Should show Docker 20+ (if using Docker)
```

---

## 🚀 Installation

### Option 1: Local Installation (Recommended for Development)

#### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/chronos.git
cd chronos
```

#### Step 2: Set Up PostgreSQL

```bash
# Create database
psql -U postgres
CREATE DATABASE chronos_db;
CREATE USER chronos_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE chronos_db TO chronos_user;
\q
```

#### Step 3: Set Up RabbitMQ

**Option A: Using Docker** (Easiest)
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  rabbitmq:3-management
```

**Option B: Native Installation**
```bash
# Ubuntu/Debian
sudo apt-get install rabbitmq-server
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# Enable management plugin
sudo rabbitmq-plugins enable rabbitmq_management

# Create user
sudo rabbitmqctl add_user admin admin123
sudo rabbitmqctl set_user_tags admin administrator
sudo rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```

Access RabbitMQ Management UI: `http://localhost:15672` (admin/admin123)

#### Step 4: Configure Application Properties

Create `application-local.properties` in all three services:

**API Service** (`api-service/src/main/resources/application-local.properties`)
```properties
# Server Configuration
server.port=8080
spring.application.name=chronos-api-service

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/chronos_db
spring.datasource.username=chronos_user
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin123

# JWT Configuration
jwt.secret=your-256-bit-secret-key-change-this-in-production
jwt.expiration=86400000

# Logging
logging.level.com.example.chronos=DEBUG
logging.level.org.springframework.security=DEBUG
```

**Scheduler Service** (`scheduler-service/src/main/resources/application-local.properties`)
```properties
# Server Configuration
server.port=8083
spring.application.name=chronos-scheduler-service

# Database Configuration (same as API)
spring.datasource.url=jdbc:postgresql://localhost:5432/chronos_db
spring.datasource.username=chronos_user
spring.datasource.password=your_secure_password

# RabbitMQ Configuration (same as API)
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin123

# Quartz Scheduler Configuration
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=always
spring.quartz.properties.org.quartz.scheduler.instanceName=ChronosScheduler
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.threadPool.threadCount=10

# Scheduler Configuration
scheduler.poll-interval-ms=30000
scheduler.enabled=true

# Logging
logging.level.com.example.chronos=DEBUG
logging.level.org.quartz=INFO
```

**Worker Service** (`worker-service/src/main/resources/application-local.properties`)
```properties
# Server Configuration
server.port=8081
spring.application.name=chronos-worker-service

# Database Configuration (same as API)
spring.datasource.url=jdbc:postgresql://localhost:5432/chronos_db
spring.datasource.username=chronos_user
spring.datasource.password=your_secure_password

# RabbitMQ Configuration (same as API)
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin123

# Worker Configuration
worker.thread-pool-size=10
worker.max-retry-attempts=3
worker.retry-delay-ms=5000
worker.webhook-timeout-ms=30000

# Logging
logging.level.com.example.chronos=DEBUG
```

#### Step 5: Build the Project

```bash
# From project root
mvn clean install

# Or build individual services
cd api-service && mvn clean install
cd ../scheduler-service && mvn clean install
cd ../worker-service && mvn clean install
```

#### Step 6: Run Database Migrations

```bash
cd api-service
mvn flyway:migrate -Dflyway.configFiles=src/main/resources/application-local.properties
```

---

### Option 2: Docker Installation (Recommended for Production)

#### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/chronos.git
cd chronos
```

#### Step 2: Configure Environment Variables

Create `.env` file in project root:

```env
# Database
POSTGRES_DB=chronos_db
POSTGRES_USER=chronos_user
POSTGRES_PASSWORD=your_secure_password

# RabbitMQ
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=admin123

# JWT
JWT_SECRET=your-256-bit-secret-key-change-this-in-production
JWT_EXPIRATION=86400000

# Ports
API_PORT=8080
SCHEDULER_PORT=8083
WORKER_PORT=8081
POSTGRES_PORT=5432
RABBITMQ_PORT=5672
RABBITMQ_MGMT_PORT=15672
```

#### Step 3: Build and Run with Docker Compose

```bash
# Build images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f api-service
docker-compose logs -f scheduler-service
docker-compose logs -f worker-service

# Stop services
docker-compose down
```

#### Docker Compose Configuration

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: chronos-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - chronos-network

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: chronos-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports:
      - "${RABBITMQ_PORT}:5672"
      - "${RABBITMQ_MGMT_PORT}:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - chronos-network

  api-service:
    build:
      context: ./api-service
      dockerfile: Dockerfile
    container_name: chronos-api
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION}
    ports:
      - "${API_PORT}:8080"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - chronos-network

  scheduler-service:
    build:
      context: ./scheduler-service
      dockerfile: Dockerfile
    container_name: chronos-scheduler
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
    ports:
      - "${SCHEDULER_PORT}:8083"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      api-service:
        condition: service_started
    networks:
      - chronos-network

  worker-service:
    build:
      context: ./worker-service
      dockerfile: Dockerfile
    container_name: chronos-worker
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
    ports:
      - "${WORKER_PORT}:8081"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      scheduler-service:
        condition: service_started
    networks:
      - chronos-network

volumes:
  postgres_data:
  rabbitmq_data:

networks:
  chronos-network:
    driver: bridge
```

---

## 🔧 Configuration

### Environment Profiles

The application supports multiple profiles:

- **`local`** - Development environment
- **`docker`** - Docker containerized environment
- **`test`** - Testing environment
- **`prod`** - Production environment

Activate a profile:
```bash
# Command line
java -jar app.jar --spring.profiles.active=prod

# Environment variable
export SPRING_PROFILES_ACTIVE=prod
```

### Key Configuration Properties

#### API Service
| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `server.port` | API port | 8080 | Yes |
| `spring.datasource.url` | Database URL | - | Yes |
| `jwt.secret` | JWT signing key | - | Yes |
| `jwt.expiration` | Token validity (ms) | 86400000 | Yes |

#### Scheduler Service
| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `server.port` | Scheduler port | 8083    | Yes |
| `scheduler.poll-interval-ms` | Job polling interval | 30000   | No |
| `scheduler.enabled` | Enable scheduler | true    | No |
| `spring.quartz.threadPool.threadCount` | Thread pool size | 10      | No |

#### Worker Service
| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `server.port` | Worker port | 8081 | Yes |
| `worker.thread-pool-size` | Worker threads | 10 | No |
| `worker.max-retry-attempts` | Max retries | 3 | No |
| `worker.webhook-timeout-ms` | HTTP timeout | 30000 | No |

---

## 🏃 Running the Application

### Local Development

You need to start all **THREE** services:

**Terminal 1: API Service**
```bash
cd api-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Terminal 2: Scheduler Service**
```bash
cd scheduler-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Terminal 3: Worker Service**
```bash
cd worker-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Using JAR Files

```bash
# Build all services
mvn clean package

# Run API Service
java -jar api-service/target/api-service-1.0.0.jar --spring.profiles.active=local &

# Run Scheduler Service
java -jar scheduler-service/target/scheduler-service-1.0.0.jar --spring.profiles.active=local &

# Run Worker Service
java -jar worker-service/target/worker-service-1.0.0.jar --spring.profiles.active=local &
```

### Startup Order

**Important**: Services should start in this order:
1. PostgreSQL
2. RabbitMQ
3. API Service (creates database schema)
4. Scheduler Service (depends on DB schema)
5. Worker Service (depends on RabbitMQ queues)

### Verify All Services are Running

```bash
# Check API Service
curl http://localhost:8080/actuator/health

# Check Scheduler Service
curl http://localhost:8083/actuator/health

# Check Worker Service
curl http://localhost:8081/actuator/health

# Expected Response from all
{"status":"UP"}
```

### Service Port Reference

| Service | Port  | Purpose |
|---------|-------|---------|
| API Service | 8080  | REST API endpoints |
| Worker Service | 8081  | Job execution |
| Scheduler Service | 8083  | Job scheduling |
| PostgreSQL | 5432  | Database |
| RabbitMQ | 5672  | Message broker |
| RabbitMQ Management | 15672 | Web UI |

---

## 📚 API Documentation

### Base URLs

- **API Service**: `http://localhost:8080`
- **Scheduler Service**: `http://localhost:8083` (Health/Metrics only)
- **Worker Service**: `http://localhost:8081` (Health/Metrics only)

> **Note**: All job management happens through the API Service. Scheduler and Worker services don't expose public APIs.

### Authentication

All endpoints (except registration and login) require JWT authentication.

#### 1. Register User

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response** (201 Created):
```json
{
  "message": "User registered successfully"
}
```

#### 2. Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123!"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 86400000
}
```

### Job Management

All job endpoints require `Authorization: Bearer <token>` header.

#### 3. Create Job

```http
POST /api/v1/jobs
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Daily Report Generator",
  "description": "Generates daily sales reports",
  "cronExpression": "0 0 9 * * ?",
  "webhookUrl": "https://api.example.com/webhook",
  "payload": {
    "reportType": "sales",
    "format": "pdf"
  },
  "maxRetries": 3,
  "retryDelaySeconds": 300,
  "priority": "HIGH"
}
```

**What Happens:**
1. API Service validates and saves job to database
2. Scheduler Service detects new job (within 30 seconds)
3. Scheduler registers job with Quartz
4. At 9 AM daily, Scheduler publishes execution message
5. Worker Service executes the webhook

**Response** (201 Created):
```json
{
  "id": 1,
  "name": "Daily Report Generator",
  "description": "Generates daily sales reports",
  "cronExpression": "0 0 9 * * ?",
  "webhookUrl": "https://api.example.com/webhook",
  "status": "SCHEDULED",
  "priority": "HIGH",
  "totalRuns": 0,
  "successfulRuns": 0,
  "failedRuns": 0,
  "avgDurationMs": 0,
  "nextRunTime": "2025-12-23T09:00:00Z",
  "createdAt": "2025-12-22T10:30:00Z",
  "createdBy": "johndoe"
}
```

#### 4. List All Jobs

```http
GET /api/v1/jobs
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Daily Report Generator",
    "status": "SCHEDULED",
    "nextRunTime": "2025-12-23T09:00:00Z",
    "totalRuns": 5,
    "successfulRuns": 5,
    "failedRuns": 0
  }
]
```

#### 5. Get Job Details

```http
GET /api/v1/jobs/{id}
Authorization: Bearer <token>
```

#### 6. Update Job

```http
PUT /api/v1/jobs/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Job Name",
  "cronExpression": "0 0 10 * * ?",
  "status": "SCHEDULED"
}
```

**Note**: Scheduler Service will automatically detect the update and re-register the job.

#### 7. Pause Job

```http
POST /api/v1/jobs/{id}/pause
Authorization: Bearer <token>
```

**What Happens**: Scheduler Service removes job from Quartz until resumed.

#### 8. Delete Job

```http
DELETE /api/v1/jobs/{id}
Authorization: Bearer <token>
```

**What Happens**: Scheduler Service detects deletion and removes from Quartz.

### Job Instances (Execution History)

#### 9. Get Job Execution History

```http
GET /api/v1/jobs/{id}/instances
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
[
  {
    "id": 101,
    "jobId": 1,
    "status": "SUCCESS",
    "startTime": "2
    
    
    
     🐛 Troubleshooting (Continued)
    
    ### Common Issues
    
    #### 1. Database Connection Failed
    
    **Symptoms**: 
```
org.postgresql.util.PSQLException: Connection refused
```

**Solutions**:
```bash
# Check if PostgreSQL is running
sudo systemctl status postgresql

# Start PostgreSQL
sudo systemctl start postgresql

# Verify connection
psql -U chronos_user -d chronos_db -h localhost

# Check port availability
netstat -an | grep 5432
```

#### 2. RabbitMQ Connection Error

**Symptoms**:
```
java.net.ConnectException: Connection refused (Connection refused)
```

**Solutions**:
```bash
# Check RabbitMQ status
sudo systemctl status rabbitmq-server

# Check RabbitMQ logs
sudo tail -f /var/log/rabbitmq/rabbit@hostname.log

# Restart RabbitMQ
sudo systemctl restart rabbitmq-server

# Verify port
telnet localhost 5672
```

#### 3. JWT Token Invalid

**Symptoms**:
```
401 Unauthorized: JWT token is invalid
```

**Solutions**:
- Token might be expired (default: 24 hours)
- Login again to get new token
- Check JWT secret matches between services
- Ensure token includes "Bearer " prefix

#### 4. Flyway Migration Failed

**Symptoms**:
```
FlywayException: Validate failed: Migration checksum mismatch
```

**Solutions**:
```bash
# Repair Flyway schema history
mvn flyway:repair

# Clean and re-migrate (⚠️ destroys data)
mvn flyway:clean flyway:migrate

# Baseline existing database
mvn flyway:baseline
```

#### 5. Port Already in Use

**Symptoms**:
```
Web server failed to start. Port 8080 was already in use.
```

**Solutions**:
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in application.properties
server.port=8090
```

#### 6. Out of Memory Error

**Symptoms**:
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions**:
```bash
# Increase heap size
java -Xmx2048m -Xms512m -jar app.jar

# Or set in MAVEN_OPTS
export MAVEN_OPTS="-Xmx2048m"
```

### Debug Logging

Enable debug logging for specific packages:

```properties
# Application logs
logging.level.com.example.chronos=DEBUG

# Security logs
logging.level.org.springframework.security=DEBUG

# SQL logs
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# RabbitMQ logs
logging.level.org.springframework.amqp=DEBUG
```

### Getting Help

If you're still stuck:

1. **Check logs**: Look in `logs/` directory or console output
2. **Search issues**: Check GitHub issues for similar problems
3. **Ask the community**: Open a new GitHub issue with:
    - Error message and stack trace
    - Steps to reproduce
    - Environment details (OS, Java version, etc.)
    - Configuration files (remove sensitive data)

---

## 📮 Postman Collection

### How to Share Your Postman Collection

#### Method 1: Export Collection (Recommended)

1. **Export from Postman**:
    - Open Postman
    - Click on your collection "Chronos API"
    - Click the three dots (...) → "Export"
    - Choose "Collection v2.1" format
    - Save as `Chronos-API.postman_collection.json`

2. **Export Environment** (if you have one):
    - Click "Environments" in sidebar
    - Click three dots on your environment
    - "Export"
    - Save as `Chronos-Environment.postman_environment.json`

3. **Add to Repository**:
   ```bash
   mkdir -p postman
   mv Chronos-API.postman_collection.json postman/
   mv Chronos-Environment.postman_environment.json postman/
   git add postman/
   git commit -m "Add Postman collection"
   git push
   ```

4. **Users Import It**:
    - Download files from GitHub
    - Open Postman → "Import"
    - Drag files or click "Upload Files"
    - Click "Import"

#### Method 2: Public Link (Alternative)

1. **Publish Collection**:
    - Right-click collection → "Share"
    - Click "Get Public Link"
    - Toggle "Public" on
    - Copy the link
    - Add link to README

2. **Add to README**:
   ```markdown
   ### Import Postman Collection
   
   [![Run in Postman](https://run.pstmn.io/button.svg)](your-public-link-here)
   ```

### Sample Postman Collection Structure

Here's what your collection should include:

```
Chronos API/
├── 📁 Auth
│   ├── Register User
│   └── Login
├── 📁 Jobs
│   ├── Create Job
│   ├── List All Jobs
│   ├── Get Job by ID
│   ├── Update Job
│   ├── Pause Job
│   ├── Resume Job
│   └── Delete Job
├── 📁 Job Instances
│   ├── Get Execution History
│   └── Get Instance Details
└── 📁 Health
    ├── API Health Check
    └── Worker Health Check
```

### Collection Variables Setup

Create these collection variables:

| Variable | Initial Value | Current Value |
|----------|--------------|---------------|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8080` |
| `workerUrl` | `http://localhost:8081` | `http://localhost:8081` |
| `authToken` | *(empty)* | *(auto-filled after login)* |

### Pre-request Script for Auth

Add this to your collection's Pre-request Scripts:

```javascript
// Auto-attach token to requests
const token = pm.collectionVariables.get("authToken");
if (token) {
    pm.request.headers.add({
        key: "Authorization",
        value: "Bearer " + token
    });
}
```

### Test Script for Login Endpoint

Add this to your "Login" request's Tests tab:

```javascript
// Save token automatically after login
if (pm.response.code === 200) {
    const jsonData = pm.response.json();
    pm.collectionVariables.set("authToken", jsonData.token);
    pm.test("Token saved successfully", function () {
        pm.expect(jsonData.token).to.not.be.empty;
    });
}

// Verify response
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has token", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("token");
});
```

### Sample Request Examples

#### 1. Register User
```json
POST {{baseUrl}}/api/v1/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Test123!@#"
}
```

#### 2. Login
```json
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "Test123!@#"
}
```

#### 3. Create Job
```json
POST {{baseUrl}}/api/v1/jobs
Authorization: Bearer {{authToken}}
Content-Type: application/json

{
  "name": "Test Job",
  "description": "A test job",
  "cronExpression": "0 */5 * * * ?",
  "webhookUrl": "https://webhook.site/unique-id",
  "payload": {
    "key": "value"
  },
  "maxRetries": 3,
  "retryDelaySeconds": 60,
  "priority": "MEDIUM"
}
```

#### 4. List Jobs
```json
GET {{baseUrl}}/api/v1/jobs
Authorization: Bearer {{authToken}}
```

### Ready-to-Use Collection Template

Create a file `postman/Chronos-API.postman_collection.json`:

```json
{
  "info": {
    "name": "Chronos API",
    "description": "Complete API collection for Chronos Job Scheduler",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "bearer",
    "bearer": [
      {
        "key": "token",
        "value": "{{authToken}}",
        "type": "string"
      }
    ]
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080",
      "type": "string"
    },
    {
      "key": "workerUrl",
      "value": "http://localhost:8081",
      "type": "string"
    },
    {
      "key": "authToken",
      "value": "",
      "type": "string"
    }
  ],
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register User",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"johndoe\",\n  \"email\": \"john@example.com\",\n  \"password\": \"SecurePass123!\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/v1/auth/register",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "auth", "register"]
            }
          }
        },
        {
          "name": "Login",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "if (pm.response.code === 200) {",
                  "    const jsonData = pm.response.json();",
                  "    pm.collectionVariables.set(\"authToken\", jsonData.token);",
                  "    console.log(\"Token saved: \" + jsonData.token);",
                  "}",
                  "",
                  "pm.test(\"Status code is 200\", function () {",
                  "    pm.response.to.have.status(200);",
                  "});",
                  "",
                  "pm.test(\"Response has token\", function () {",
                  "    const jsonData = pm.response.json();",
                  "    pm.expect(jsonData).to.have.property(\"token\");",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "auth": {
              "type": "noauth"
            },
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"johndoe\",\n  \"password\": \"SecurePass123!\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/v1/auth/login",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "auth", "login"]
            }
          }
        }
      ]
    },
    {
      "name": "Jobs",
      "item": [
        {
          "name": "Create Job",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"Daily Report\",\n  \"description\": \"Generate daily report\",\n  \"cronExpression\": \"0 0 9 * * ?\",\n  \"webhookUrl\": \"https://webhook.site/your-unique-url\",\n  \"payload\": {\n    \"reportType\": \"sales\"\n  },\n  \"maxRetries\": 3,\n  \"retryDelaySeconds\": 300,\n  \"priority\": \"HIGH\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/v1/jobs",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "jobs"]
            }
          }
        },
        {
          "name": "List All Jobs",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/api/v1/jobs",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "jobs"]
            }
          }
        },
        {
          "name": "Get Job by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/api/v1/jobs/1",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "jobs", "1"]
            }
          }
        },
        {
          "name": "Update Job",
          "request": {
            "method": "PUT",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"Updated Job Name\",\n  \"cronExpression\": \"0 0 10 * * ?\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/v1/jobs/1",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "jobs", "1"]
            }
          }
        },
        {
          "name": "Pause Job",
          "request": {
            "method": "POST",
            "url": {
              "raw": "{{baseUrl}}/api/v1/jobs/1/pause",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "jobs", "1", "pause"]
            }
          }
        },
        {
          "name": "Delete Job",
          "request": {
            "method": "DELETE",
            "url": {
              "raw": "{{baseUrl}}/api/v1/jobs/1",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "jobs", "1"]
            }
          }
        }
      ]
    },
    {
      "name": "Health Checks",
      "item": [
        {
          "name": "API Health",
          "request": {
            "auth": {
              "type": "noauth"
            },
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/actuator/health",
              "host": ["{{baseUrl}}"],
              "path": ["actuator", "health"]
            }
          }
        },
        {
          "name": "Worker Health",
          "request": {
            "auth": {
              "type": "noauth"
            },
            "method": "GET",
            "url": {
              "raw": "{{workerUrl}}/actuator/health",
              "host": ["{{workerUrl}}"],
              "path": ["actuator", "health"]
            }
          }
        }
      ]
    }
  ]
}
```

### Usage Instructions for Team

Add this to your README:

```markdown
## 📮 Import Postman Collection

### Quick Start

1. **Download Collection**:
   - [Download Chronos API Collection](postman/Chronos-API.postman_collection.json)

2. **Import to Postman**:
   - Open Postman
   - Click "Import" button (top left)
   - Drop the JSON file or click "Upload Files"
   - Click "Import"

3. **Configure Environment** (Optional):
   - Collection variables are already set
   - Default: `http://localhost:8080`
   - To change: Edit collection → Variables tab

4. **Start Testing**:
   - Run "Register User" first
   - Then run "Login" (token auto-saves)
   - All other requests will use the saved token automatically

### Testing Workflow

```
1. Auth/Register User → Create account
2. Auth/Login → Get JWT token (auto-saved)
3. Jobs/Create Job → Create your first job
4. Jobs/List All Jobs → See all jobs
5. Jobs/Get Job by ID → View specific job
6. Health Checks → Verify services
```

### Troubleshooting Postman

**Issue**: "Could not get response"
- **Fix**: Ensure services are running (`curl http://localhost:8080/actuator/health`)

**Issue**: "401 Unauthorized"
- **Fix**: Run the "Login" request first to refresh token

**Issue**: "Connection refused"
- **Fix**: Check `baseUrl` variable matches your server address
```

---

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Getting Started

1. **Fork the repository**
2. **Create a feature branch**:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Make your changes**
4. **Run tests**:
   ```bash
   mvn test
   ```
5. **Commit with conventional commits**:
   ```bash
   git commit -m "feat: add amazing feature"
   ```
6. **Push to your fork**:
   ```bash
   git push origin feature/amazing-feature
   ```
7. **Open a Pull Request**

### Commit Message Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting, etc.)
- `refactor:` Code refactoring
- `test:` Adding or updating tests
- `chore:` Maintenance tasks

### Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add JavaDoc for public methods
- Keep methods small and focused
- Write tests for new features

### Pull Request Guidelines

- **Title**: Clear and descriptive
- **Description**: What, why, and how
- **Tests**: Include unit/integration tests
- **Documentation**: Update README if needed
- **Breaking Changes**: Clearly document

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Chronos Job Scheduler

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 Acknowledgments

- Spring Boot Team for the amazing framework
- RabbitMQ Team for reliable messaging
- PostgreSQL Team for the robust database
- All contributors who help improve Chronos

---

## 📞 Contact & Support

- **GitHub Issues**: [Report bugs](https://github.com/yourusername/chronos/issues)
- **Email**: support@chronos-scheduler.dev
- **Documentation**: [Full Docs](https://chronos-scheduler.dev/docs)
- **Community**: [Discord](https://discord.gg/chronos) | [Slack](https://chronos-slack.dev)

---

<div align ="center">

**⭐ If you find Chronos useful, please give it a star on GitHub! ⭐**

Made with ❤️ by the Chronos Team

[Back to Top](#-chronos---distributed-job-scheduler)

</div>