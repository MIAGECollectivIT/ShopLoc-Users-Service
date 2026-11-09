# ShopLoc - Users Service
User management microservice for the ShopLoc project (MIAGE Collectiv'IT).

## Table of contents
- [0. Prerequisites](#0-prerequisites)
- [1. Tech Stack & Dependencies](#1-tech-stack--dependencies)
- [2. Installation & Setup](#2-installation--setup)
  - [2.1. Environment Configuration (.env)](#21-environment-configuration-env)
  - [2.2. Husky](#22-husky)
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
- **JWT & RSA RS256 Cryptography**: Asymmetrically signed JSON Web Tokens (JJWT) with RFC 7517 JWKS discovery and token revocation tracking.
- **Spring Boot Starter Validation**: Server-side Bean Validation with Hibernate Validator.
- **Springdoc OpenAPI (Swagger UI)**: OpenAPI 3 interactive documentation (`/swagger-ui/index.html`) with prefilled request examples, multi-server support (`/` direct and `/api` gateway), and JWT Bearer authentication.
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

### 2.1. Environment Configuration (.env)
A template file `.env.example` is provided at the root of the repository. Duplicate it to create your local `.env` configuration:
```bash
# Duplicate example file
cp .env.example .env
```
The file defines customizable environment variables for local Docker Compose and standalone runs:
- `PORT`: Service HTTP port (default `8080`)
- `DB_HOST`, `DB_PORT`: PostgreSQL connection host and port (`db` for Docker Compose, `localhost` for local IDE)
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: PostgreSQL container credentials
- `DB_NAME`, `DB_USER`, `DB_PASSWORD`: Spring Boot datasource configuration
- `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`: Asymmetric RSA keys (PEM / PKCS#8 and X.509 Base64 format) for JWT signing and verification
- `JWT_KEY_ID`: Key ID identifier for the JWKS standard (default `shoploc-key-1`)

> In production (K3s cluster), secrets are provided directly by Kubernetes Secrets (`db-credentials-users`).

### 2.2. Husky 
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
- The database is initialized and migrated automatically with Flyway ([V1__create_users_table.sql](src/main/resources/db/migration/V1__create_users_table.sql)).
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
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) (or [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html))
- **OpenAPI JSON specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> [!NOTE]
> **Port Configuration:** If you launch all of ShopLoc microservices together like the ingress does, the port might not be `8080`. You might need to check your environment / ingress configuration to access Swagger (the default multi-service configuration uses port `8081`, e.g. `http://localhost:8081/swagger-ui/index.html`).

#### Available Servers in Swagger UI
In the **Servers** dropdown menu at the top of the Swagger UI page, you can choose the execution target:
1. **`Direct / Local (Default)` (`/`)**: Selected by default, routes requests directly to the local service (e.g. `http://localhost:8080/v1/...`).
2. **`Traefik API Gateway (WAN)` (`/api`)**: Simulates calls routed through the Traefik API gateway (e.g. `/api/v1/...`).

#### Prefilled Request Examples
All endpoints provide prefilled request body examples with realistic data:
- `LoginRequest`: Valid email address and CNIL-compliant password.
- `UserRegistrationRequest`: Identity, date of birth (> 18 years old), French SIV license plate format (`AA-123-BB`), and pickup location.
- `VehicleRequest`: French license plate format validation.
- `UserStoreLinkRequest`: Partner and store numeric identifiers.

#### How to Authenticate in Swagger UI
To test protected endpoints directly from the UI:
1. Open the public operation `POST /v1/users/login`.
2. Click on **Try it out** and execute the request with prefilled test credentials (or one of the default seed accounts: `alice.client@example.com` / `ClientPass123!`, `bob.merchant@example.com` / `MerchantPass123!`, `admin@example.com` / `AdminPass123!`).
3. Copy the `token` value from the JSON response body.
4. Click on the **Authorize** button (green padlock at the top right of the page) and paste your JWT token (the `Bearer ` prefix is handled automatically).
5. You can now test all secured routes (`/me`, `/me/vehicles`, etc.).

### 4.2 Authentication in application.yaml
Authentication is configured centrally in `application.yaml` under `application.security`:
```yaml
application:
  security:
    registration-enabled: ${REGISTRATION_ENABLED:false}
    auth-type: "Bearer JWT"
    jwt:
      header-name: "Authorization"
      token-prefix: "Bearer "
      private-key: "${JWT_PRIVATE_KEY:}"
      public-key: "${JWT_PUBLIC_KEY:}"
      key-id: "${JWT_KEY_ID:shoploc-key-1}"
      issuer: "shoploc-auth-service"
      expiration-ms: 7200000
    lockout:
      max-attempts: ${SECURITY_MAX_LOGIN_ATTEMPTS:5}
      lockout-minutes: ${SECURITY_LOCKOUT_MINUTES:15}
    description: "Authenticate using a JWT Bearer token."
```

### 4.3 Endpoints Overview

All endpoints are supported both on the direct path (`/v1/...`) and on the gateway path (`/api/v1/...`).

| Method | Endpoint | Service | Required Auth | Role | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Authentication & Session** | | | | | |
| `POST` | `/v1/users` | `AuthenticationService` | None | Public | Register a new client user (disabled in production, HTTP 403) |
| `POST` | `/v1/users/login` | `AuthenticationService` | None | Public | User authentication, returns signed Bearer JWT token |
| `POST` | `/v1/users/logout` | `TokenRevocationService` | Yes (Bearer) | All | User logout and JWT JTI revocation |
| **User Profiles** | | | | | |
| `GET` | `/v1/users/me` | `UserProfileService` | Yes (Bearer) | All | Retrieve current authenticated user profile |
| `GET` | `/v1/users/{userId}` | `UserProfileService` | Yes (Bearer) | Self / ADMIN | Retrieve user profile by public UUID or numeric ID |
| `PUT` | `/v1/users/{userId}` | `UserProfileService` | Yes (Bearer) | Self / ADMIN | Update personal profile details |
| `DELETE` | `/v1/users/{userId}` | `UserProfileService` | Yes (Bearer) | Self / ADMIN | Delete user account |
| `GET` | `/v1/users/partners` | `UserProfileService` | None | Public | List partner merchants available for store linking |
| `POST` | `/v1/users/stores` | `UserProfileService` | Yes (Bearer) | MERCHANT / ADMIN | Link a store to a partner merchant |
| **Partner Administration (Admin)** | | | | | |
| `GET` | `/v1/admin/partners` | `AdminPartnerService` | Yes (Bearer) | ADMIN | List partner merchant accounts with optional filtering (query, limit) |
| `GET` | `/v1/admin/partners/{id}` | `AdminPartnerService` | Yes (Bearer) | ADMIN | Detailed partner profile including associated stores |
| `PUT` | `/v1/admin/partners/{id}` | `AdminPartnerService` | Yes (Bearer) | ADMIN | Update partner administrative details |
| `POST` | `/v1/admin/partners/{id}/stores` | `AdminPartnerService` | Yes (Bearer) | ADMIN | Associate an existing store with a partner account |
| **Vehicle & License Plate Management** | | | | | |
| `GET` | `/v1/users/me/vehicles` | `UserProfileService` | Yes (Bearer) | CLIENT | List vehicles for authenticated client |
| `POST` | `/v1/users/me/vehicles` | `UserProfileService` | Yes (Bearer) | CLIENT | Add a license plate for authenticated client |
| `PUT` | `/v1/users/me/vehicles/{vehicleId}` | `UserProfileService` | Yes (Bearer) | CLIENT | Update a client license plate |
| `DELETE` | `/v1/users/me/vehicles/{vehicleId}` | `UserProfileService` | Yes (Bearer) | CLIENT | Delete a client license plate |
| `GET` | `/v1/users/{userId}/vehicles` | `UserProfileService` | Yes (Bearer) | Self / ADMIN | List vehicles for a user by user ID |
| `POST` | `/v1/users/{userId}/vehicles` | `UserProfileService` | Yes (Bearer) | Self / ADMIN | Add a vehicle for a specified user |
| `PUT` | `/v1/users/{userId}/vehicles/{vehicleId}` | `UserProfileService` | Yes (Bearer) | Self / ADMIN | Update a vehicle for a specified user |
| `DELETE` | `/v1/users/{userId}/vehicles/{vehicleId}` | `UserProfileService` | Yes (Bearer) | Self / ADMIN | Delete a vehicle for a specified user |
| **Loyalty Card & Health** | | | | | |
| `GET` | `/v1/users/loyalty-cards/{cardNumber}` | `LoyaltyCardIdentificationService` | Yes (Bearer) | All | Identify client by loyalty card number or client code |
| `GET` | `/v1/users/status` | `UsersStatusController` | None | Public | Microservice operational health status (`UP`) |
| **Cryptography & Gateway Discovery** | | | | | |
| `GET` | `/.well-known/jwks.json` | `JwtTokenService` | None | Public | Standard RFC 7517 JWKS discovery (domain root) |
| `GET` | `/v1/auth/jwks.json` | `JwtTokenService` | None | Public | JWKS discovery under `/v1/auth` path |
| `GET` | `/v1/auth/public-key` | `JwtTokenService` | None | Public | RSA Public Key (X.509 SPKI Base64) for JWT verification |
| `GET` | `/v1/auth/revoked-tokens` | `TokenRevocationService` | None | Public | List of active revoked token JTIs (cache synchronization) |
| `GET` | `/v1/auth/check-revocation` | `TokenRevocationService` | None | Public | Check revocation status of a specific JTI |
| `GET` | `/v1/auth/verify` | `JwtAuthInterceptor` | Yes (Bearer/Cookie)| All | ForwardAuth endpoint returning identity headers |

## 5. Project structure
```
├── .dockerignore                # Docker build context exclusions
├── .env.example                 # Environment variables configuration template
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
│   │   │       │   ├── OpenApiConfig.java          # Swagger & OpenAPI authentication setup, customizer
│   │   │       │   ├── SecurityConfig.java         # BCrypt PasswordEncoder configuration
│   │   │       │   └── WebMvcConfig.java           # CORS & Interceptor exclusions
│   │   │       ├── controller/
│   │   │       │   ├── AdminPartnerController.java     # /v1/admin/partners admin operations
│   │   │       │   ├── AuthenticationController.java   # Auth, Login, Logout, Public Keys, Revocation
│   │   │       │   ├── JwksDiscoveryController.java    # RFC 7517 root discovery endpoint
│   │   │       │   ├── LoyaltyCardController.java      # Customer loyalty card identification
│   │   │       │   ├── UserProfileController.java      # /v1/users/{userId} & vehicles
│   │   │       │   └── UsersStatusController.java      # GET /v1/users/status
│   │   │       ├── dto/
│   │   │       │   ├── AdminAddStoreRequest.java
│   │   │       │   ├── AdminPartnerUpdateRequest.java
│   │   │       │   ├── AuthResponse.java
│   │   │       │   ├── LoginRequest.java
│   │   │       │   ├── PartnerDetailResponse.java
│   │   │       │   ├── PartnerResponse.java
│   │   │       │   ├── UserRegistrationRequest.java
│   │   │       │   ├── UserResponse.java
│   │   │       │   ├── UserStoreLinkRequest.java
│   │   │       │   ├── UserUpdateRequest.java
│   │   │       │   ├── VehicleRequest.java
│   │   │       │   └── VehicleResponse.java
│   │   │       ├── entity/
│   │   │       │   ├── Admin.java                  # Polymorphic admin entity
│   │   │       │   ├── Client.java                 # Client entity with license plates & client code
│   │   │       │   ├── Merchant.java               # Merchant partner entity
│   │   │       │   ├── RevokedToken.java           # Revoked JWT tracking entity
│   │   │       │   ├── User.java                   # JPA base entity with inheritance mapping
│   │   │       │   ├── UserRole.java
│   │   │       │   └── UserStore.java              # Merchant-to-store relationship
│   │   │       ├── exception/
│   │   │       │   ├── AccountLockedException.java
│   │   │       │   ├── EmailAlreadyExistsException.java
│   │   │       │   ├── ForbiddenException.java
│   │   │       │   ├── GlobalExceptionHandler.java # ProblemDetail RFC 7807 handler
│   │   │       │   ├── UnauthorizedException.java
│   │   │       │   ├── UserNotFoundException.java
│   │   │       │   ├── VehicleAlreadyExistsException.java
│   │   │       │   └── VehicleNotFoundException.java
│   │   │       ├── repository/
│   │   │       │   ├── ClientRepository.java
│   │   │       │   ├── RevokedTokenRepository.java
│   │   │       │   ├── UserRepository.java
│   │   │       │   └── UserStoreRepository.java
│   │   │       ├── security/
│   │   │       │   ├── JwtAuthInterceptor.java     # Route protection & identity resolution
│   │   │       │   └── JwtTokenService.java        # RSA RS256 token provider & JWKS generator
│   │   │       ├── service/
│   │   │       │   ├── AdminPartnerService.java
│   │   │       │   ├── AuthenticationService.java
│   │   │       │   ├── LoyaltyCardIdentificationService.java
│   │   │       │   ├── TokenRevocationService.java
│   │   │       │   ├── UserProfileService.java
│   │   │       │   └── impl/
│   │   │       │       ├── AdminPartnerServiceImpl.java
│   │   │       │       ├── AuthenticationServiceImpl.java
│   │   │       │       ├── LoyaltyCardIdentificationServiceImpl.java
│   │   │       │       ├── TokenRevocationServiceImpl.java
│   │   │       │       ├── UserIdentityResolver.java
│   │   │       │       └── UserProfileServiceImpl.java
│   │   │       ├── util/
│   │   │       │   └── ClientCodeGenerator.java    # Base62 7-character code generator
│   │   │       ├── validation/
│   │   │       │   ├── FrenchLicensePlate.java          # French license plate validation annotation
│   │   │       │   ├── FrenchLicensePlateValidator.java # SIV/FNI format validator & normalizer
│   │   │       │   ├── MinAge.java                      # Minimum age validation annotation
│   │   │       │   ├── MinAgeValidator.java             # Bean Validation constraint validator
│   │   │       │   ├── ValidPassword.java               # CNIL password complexity annotation
│   │   │       │   └── ValidPasswordValidator.java
│   │   │       └── UsersServiceApplication.java    # Application entrypoint
│   │   └── resources/
│   │       ├── application.yaml                    # Application, datasource & security configuration
│   │       └── db/migration/                       # Flyway migration scripts
│   └── test/
│       ├── java/                # Unit & integration tests
│       │   └── fr/miage/collectivit/usersservice/
│       │       ├── config/
│       │       │   └── SecurityConfigTest.java
│       │       ├── controller/
│       │       │   ├── AuthenticationControllerTest.java
│       │       │   ├── LoyaltyCardControllerTest.java
│       │       │   ├── OpenApiIntegrationTest.java      # Swagger UI & OpenAPI validation
│       │       │   ├── UserPasswordSecurityIntegrationTest.java
│       │       │   ├── UserProfileControllerTest.java
│       │       │   └── UsersStatusControllerTest.java
│       │       ├── repository/
│       │       │   └── UserRepositoryTest.java
│       │       ├── security/
│       │       │   ├── JwtTokenServiceTest.java
│       │       │   └── UserSecurityIntegrationTest.java
│       │       ├── service/
│       │       │   ├── AuthenticationServiceTest.java
│       │       │   ├── LoyaltyCardIdentificationServiceTest.java
│       │       │   └── UserProfileServiceTest.java
│       │       ├── util/
│       │       │   └── ClientCodeGeneratorTest.java
│       │       ├── validation/
│       │       │   ├── FrenchLicensePlateValidatorTest.java
│       │       │   ├── MinAgeValidatorTest.java
│       │       │   └── ValidPasswordValidatorTest.java
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
