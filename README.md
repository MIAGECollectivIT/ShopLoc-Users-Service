# ShopLoc - Users Service
User management microservice for the ShopLoc project (MIAGE Collectiv'IT).

## Table of contents
- [0. Prerequisites](#0-prerequisites)
- [1. Tech Stack & Dependencies](#1-tech-stack--dependencies)
- [2. Installation & Setup](#2-installation--setup)
  - [2.1. Husky](#21-husky)
- [3. Launch & Test](#3-launch--test)
  - [3.1 Local Execution (Maven)](#31-local-execution-maven)
  - [3.2 Containerized Execution (Docker & Docker Compose)](#32-containerized-execution-docker--docker-compose)
- [4. API Documentation & Endpoints](#4-api-documentation--endpoints)
  - [4.1 Interactive Documentation (Swagger UI)](#41-interactive-documentation-swagger-ui)
  - [4.2 Authentication in application.yaml](#42-authentication-in-applicationyaml)
  - [4.3 Endpoints Overview](#43-endpoints-overview)
- [5. Project structure](#5-project-structure)
- [6. Workflow](#6-workflow)
  - [6.1 Semantic versioning](#61-semantic-versioning)
  - [6.2 Quality pipeline](#62-quality-pipeline)
    - [6.2.1 (Husky) Pre-commit](#621-husky-pre-commit)
    - [6.2.2 GitHub Actions](#622-github-actions)

## 0. Prerequisites
- **Java 21** (JDK 21 LTS minimum, enforced by `maven-enforcer-plugin`)
- **Maven** (>= 3.8.0, included via the Maven Wrapper `./mvnw` / `mvnw.cmd` and enforced by `maven-enforcer-plugin`)
- **Node.js** (>= 18) & **npm** (for Husky git hooks)
- **Docker** & **Docker Compose** (for containerized execution and local PostgreSQL)

## 1. Tech Stack & Dependencies
- **Spring Boot**: Foundation framework for creating standalone REST microservices.
- **Spring Boot Starter WebMVC**: RESTful API development with embedded Apache Tomcat.
- **Spring Boot Starter Data JPA**: Object-Relational Mapping (Hibernate) and Spring Data repositories.
- **PostgreSQL JDBC Driver**: Relational database driver for production and containerized environments.
- **Spring Security Crypto**: BCrypt password hashing (work factor 12) with automated per-password salting.
- **Spring Boot Starter Validation**: Server-side Bean Validation with Hibernate Validator.
- **Springdoc OpenAPI (Swagger UI)**: OpenAPI 3 interactive documentation (`/swagger-ui.html`) with JWT Bearer authentication support.
- **Project Lombok**: Reduces boilerplate code (getters, setters, builders, constructors) via annotation processing.
- **H2 Database (Test scope)**: In-memory database with PostgreSQL compatibility mode for automated unit and integration tests.
- **Spring Boot Starter Test & Testing Suite**:
  - JUnit Jupiter (JUnit 5)
  - Mockito & Mockito JUnit Jupiter
  - AssertJ, Hamcrest, JSONassert
  - Spring Test Context Framework & MockMvc
- **Maven Enforcer Plugin**: Enforces minimum versions for both Java (>= 21) and Maven (>= 3.8.0).
- **Spotless Maven Plugin**: Automated code formatting using Google Java Format.
- **PMD Maven Plugin**: Static code analysis detecting code smells and anti-patterns.

## 2. Installation & Setup

### 2.1. Husky 
> Do not bypass the root initialization. Husky pre-commit hooks are mandatory. Your code will be rejected automatically if quality standards are not met.
```bash
# At the root of the project (Git hooks for linters and tests)
npm install
```

## 3. Launch & Test

### 3.1 Local Execution (Maven)

```bash
# Run the application locally
./mvnw spring-boot:run

# Run the unit and integration test suite
./mvnw test

# Validate build, enforce version rules, and run all tests & verifications
./mvnw clean verify

# Format code with Spotless (Google Java Format)
./mvnw spotless:apply

# Check code formatting without modifying files
./mvnw spotless:check

# Run PMD static code analysis
./mvnw pmd:check

# Validate environment constraints specifically with Maven Enforcer
./mvnw enforcer:enforce
```

*On Windows (PowerShell / Command Prompt), use `.\mvnw.cmd` instead of `./mvnw`.*

### 3.2 Containerized Execution (Docker & Docker Compose)

#### Docker Compose (Recommended)
A `docker-compose.yml` file is provided for multi-container development, running both the PostgreSQL 16 database and the Spring Boot backend:
- The database is initialized automatically with [db/init.sql](db/init.sql).
- Data persists across runs via the `db_data` Docker volume.
- The backend waits for the database to become healthy before starting.

```bash
# Build and start PostgreSQL and Backend in detached mode
docker compose up --build -d

# View live container logs
docker compose logs -f backend

# Stop services
docker compose down
```

#### Standalone Docker
Build and run the multi-stage container image:
```bash
# Build the Docker image
docker build -t users-service .

# Run the container (mapping port 8080)
docker run -p 8080:8080 users-service
```

## 4. API Documentation & Endpoints

### 4.1 Interactive Documentation (Swagger UI)
Once the service is started, access interactive OpenAPI documentation at:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) (or `http://localhost:8080/swagger-ui.html`)
- **OpenAPI JSON specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 4.2 Authentication in application.yaml
Authentication is configured centrally in `application.yaml` under `application.security`:
```yaml
application:
  security:
    auth-type: "Bearer JWT"
    jwt:
      header-name: "Authorization"
      token-prefix: "Bearer "
      secret-key: "${JWT_SECRET:dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=}"
      issuer: "shoploc-auth-service"
      expiration-ms: 86400000
    description: "Authenticate using a JWT Bearer token."
```
In Swagger UI, click the **Authorize** button (padlock icon) to input your JWT token for protected endpoints.

### 4.3 Endpoints Overview

| Method | Endpoint | Service | Auth Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/v1/users` | `AuthenticationService` | No | Register a new client user (password hashed via BCrypt, 7-char client code generated) |
| `GET` | `/v1/users/{userId}` | `UserProfileService` | Yes (Bearer) | Retrieve user profile by public UUID |
| `PUT` | `/v1/users/{userId}` | `UserProfileService` | Yes (Bearer) | Update user profile personal information |
| `DELETE` | `/v1/users/{userId}` | `UserProfileService` | Yes (Bearer) | Delete user profile |
| `GET` | `/v1/users/{userId}/vehicles` | `UserProfileService` | Yes (Bearer) | Get registered vehicles / license plates |
| `POST` | `/v1/users/{userId}/vehicles` | `UserProfileService` | Yes (Bearer) | Add a vehicle license plate |
| `DELETE` | `/v1/users/{userId}/vehicles/{vehicleId}` | `UserProfileService` | Yes (Bearer) | Remove a vehicle license plate |
| `GET` | `/v1/users/loyalty-cards/{cardNumber}` | `LoyaltyCardIdentificationService` | Yes (Bearer) | Identify customer by loyalty card number / client code |
| `GET` | `/v1/users/status` | `UsersStatusController` | Yes (Bearer) | Service health and operational status |

## 5. Project structure
```
├── .dockerignore                # Docker build context exclusions
├── .github/                     # GitHub Actions workflows (CI & CD)
│   └── workflows/
│       ├── CI.yml               # Spotless lint + PMD analysis + Build & Tests
│       ├── cd-dev.yml           # Continuous Deployment to Development (K3s)
│       └── cd-prod.yml          # Semantic Release, Docker Build & Deployment to Production (K3s)
├── .husky/                      # Git hooks (pre-commit)
├── .mvn/                        # Maven Wrapper configuration
├── db/
│   └── init.sql                 # PostgreSQL database initialization script
├── src/
│   ├── main/
│   │   ├── java/                # Java Spring Boot source code
│   │   │   └── fr/miage/collectivit/usersservice/
│   │   │       ├── config/
│   │   │       │   ├── OpenApiConfig.java          # Swagger & OpenAPI authentication setup
│   │   │       │   └── SecurityConfig.java         # BCrypt PasswordEncoder configuration
│   │   │       ├── controller/
│   │   │       │   ├── AuthenticationController.java   # POST /v1/users
│   │   │       │   ├── LoyaltyCardController.java      # GET /v1/users/loyalty-cards/{cardNumber}
│   │   │       │   ├── UserProfileController.java      # /v1/users/{userId} & vehicles
│   │   │       │   └── UsersStatusController.java      # GET /v1/users/status
│   │   │       ├── dto/
│   │   │       │   ├── UserRegistrationRequest.java
│   │   │       │   ├── UserResponse.java
│   │   │       │   ├── UserUpdateRequest.java
│   │   │       │   ├── VehicleRequest.java
│   │   │       │   └── VehicleResponse.java
│   │   │       ├── entity/
│   │   │       │   ├── User.java                   # JPA entity with PostgreSQL ARRAY mapping
│   │   │       │   └── UserRole.java
│   │   │       ├── exception/
│   │   │       │   ├── EmailAlreadyExistsException.java
│   │   │       │   ├── GlobalExceptionHandler.java # ProblemDetail RFC 7807 handler
│   │   │       │   ├── UserNotFoundException.java
│   │   │       │   ├── VehicleAlreadyExistsException.java
│   │   │       │   └── VehicleNotFoundException.java
│   │   │       ├── repository/
│   │   │       │   └── UserRepository.java         # Spring Data JPA repository
│   │   │       ├── service/
│   │   │       │   ├── AuthenticationService.java
│   │   │       │   ├── LoyaltyCardIdentificationService.java
│   │   │       │   ├── UserProfileService.java
│   │   │       │   └── impl/
│   │   │       │       ├── AuthenticationServiceImpl.java
│   │   │       │       ├── LoyaltyCardIdentificationServiceImpl.java
│   │   │       │       └── UserProfileServiceImpl.java
│   │   │       ├── util/
│   │   │       │   └── ClientCodeGenerator.java    # Base62 7-character code generator
│   │   │       └── UsersServiceApplication.java    # Application entrypoint
│   │   └── resources/
│   │       └── application.yaml                    # Application, datasource & security configuration
│   └── test/
│       ├── java/                # Unit & integration tests
│       │   └── fr/miage/collectivit/usersservice/
│       │       ├── config/
│       │       │   └── SecurityConfigTest.java
│       │       ├── controller/
│       │       │   ├── AuthenticationControllerTest.java
│       │       │   ├── LoyaltyCardControllerTest.java
│       │       │   ├── UserProfileControllerTest.java
│       │       │   └── UsersStatusControllerTest.java
│       │       ├── repository/
│       │       │   └── UserRepositoryTest.java
│       │       ├── service/
│       │       │   ├── AuthenticationServiceTest.java
│       │       │   ├── LoyaltyCardIdentificationServiceTest.java
│       │       │   └── UserProfileServiceTest.java
│       │       ├── util/
│       │       │   └── ClientCodeGeneratorTest.java
│       │       └── UsersServiceApplicationTests.java
│       └── resources/
│           └── application.yaml                    # H2 in-memory test configuration
├── docker-compose.yml           # PostgreSQL 16 + Backend development orchestration
├── Dockerfile                   # Multi-stage container image definition
├── mvnw                         # Maven Wrapper script (Linux / macOS)
├── mvnw.cmd                     # Maven Wrapper batch script (Windows)
├── pmd-ruleset.xml              # PMD customized ruleset configuration
├── pom.xml                      # Maven project configuration, dependencies & plugins
└── README.md
```

## 6. Workflow
### 6.1 Semantic versioning
Automated versioning runs on every deployment and bumps the project version according to Conventional Commits:
- `fix:` → Patch Increment (e.g. 1.0.1)
- `feat:` → Minor Increment (e.g. 1.1.0)
- `BREAKING CHANGE:` (or `feat!:`) → Major Increment (e.g. 2.0.0)

### 6.2 Quality pipeline 
#### 6.2.1 (Husky) Pre-commit
A pre-commit hook runs automatically before every commit to format code with Spotless, run PMD static analysis, and ensure tests compile:
```bash
./mvnw spotless:apply
./mvnw pmd:check
./mvnw clean test-compile
```

#### 6.2.2 GitHub Actions 
- **CI (`CI.yml`)**:
  - **Trigger**: Pull requests targeting `main` and `dev`.
  - **Pipeline**: Executes the complete verification suite (`./mvnw -B -ntp -T 1C verify`) covering Maven Enforcer version rules, Spotless formatting check, PMD static analysis, and all unit/integration tests in a single optimized pass.
- **CD Dev (`cd-dev.yml`)**:
  - **Trigger**: Push to `dev` branch or manual `workflow_dispatch`.
  - **Pipeline**: Runs tests, builds and publishes the Docker image to GitHub Container Registry, and deploys to K3s Development.
- **CD Prod (`cd-prod.yml`)**:
  - **Trigger**: Push to `main` branch or manual `workflow_dispatch`.
  - **Pipeline**: Runs tests, calculates semantic version, publishes GitHub Release, builds and pushes Docker image, and deploys to K3s Production.
