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
- **JWT & Cryptographie RSA RS256**: JSON Web Tokens signés asymétriquement (JJWT) avec découverte JWKS RFC 7517 et révocation de jetons.
- **Spring Boot Starter Validation**: Server-side Bean Validation with Hibernate Validator.
- **Springdoc OpenAPI (Swagger UI)**: OpenAPI 3 interactive documentation (`/swagger-ui/index.html`) avec exemples de requêtes pré-remplis, support multi-serveurs (`/` direct et `/api` gateway) et authentification JWT Bearer.
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
- `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`: Clés asymétriques RSA (format PEM / PKCS#8 et X.509 Base64) pour la signature et la vérification des JWT
- `JWT_KEY_ID`: Identifiant du Key ID pour le standard JWKS (défaut `shoploc-key-1`)

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
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) (ou [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html))
- **OpenAPI JSON specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> [!NOTE]
> **Port Configuration:** If you launch all of ShopLoc microservices together like the ingress does, the port might not be `8080`. You might need to check your environment / ingress configuration to access Swagger (the default multi-service configuration uses port `8081`, e.g. `http://localhost:8081/swagger-ui/index.html`).

#### Serveurs disponibles dans Swagger UI
Dans le menu déroulant **Servers** en haut de la page Swagger UI, vous pouvez choisir la cible d'exécution :
1. **`Direct / Local (Défaut)` (`/`)** : sélectionné par défaut, envoie les requêtes directement vers le service en local (ex: `http://localhost:8080/v1/...`).
2. **`Traefik API Gateway (WAN)` (`/api`)** : simule les appels transitant par la passerelle Traefik (ex: `/api/v1/...`).

#### Exemples de requêtes pré-remplis
Toutes les routes disposent d'exemples de requêtes pré-remplis avec des données réalistes :
- `LoginRequest` : email valide et mot de passe conforme à la politique CNIL.
- `UserRegistrationRequest` : identité, date de naissance (> 18 ans), plaque au format français SIV (`AA-123-BB`) et lieu de retrait.
- `VehicleRequest` : validation de plaque d'immatriculation française.
- `UserStoreLinkRequest` : identifiants numériques de partenaire et de commerce.

#### Comment s'authentifier dans Swagger UI
Pour tester les routes protégées directement depuis l'interface :
1. Ouvrez l'opération publique `POST /v1/users/login`.
2. Cliquez sur **Try it out** et exécutez la requête avec les identifiants de test pré-remplis (ou l'un des comptes par défaut : `alice.client@example.com` / `ClientPass123!`, `bob.merchant@example.com` / `MerchantPass123!`, `admin@example.com` / `AdminPass123!`).
3. Copiez la valeur du champ `token` dans la réponse JSON.
4. Cliquez sur le bouton **Authorize** (cadenas vert en haut à droite de la page) et collez votre jeton JWT (le préfixe `Bearer` est géré automatiquement).
5. Vous pouvez désormais tester l'ensemble des routes sécurisées (`/me`, `/me/vehicles`, etc.).

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

Toutes les routes sont supportées à la fois sur le chemin direct (`/v1/...`) et sur le chemin passerelle (`/api/v1/...`).

| Méthode | Endpoint | Service | Auth Requise | Rôle | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Authentification & Session** | | | | | |
| `POST` | `/v1/users` | `AuthenticationService` | Non | Public | Inscription d'un nouveau client (désactivée en prod, HTTP 403) |
| `POST` | `/v1/users/login` | `AuthenticationService` | Non | Public | Connexion utilisateur, émission du jeton JWT Bearer |
| `POST` | `/v1/users/logout` | `TokenRevocationService` | Oui (Bearer) | Tous | Déconnexion et révocation du JTI du jeton JWT |
| **Profils Utilisateurs** | | | | | |
| `GET` | `/v1/users/me` | `UserProfileService` | Oui (Bearer) | Tous | Consultation du profil de l'utilisateur connecté |
| `GET` | `/v1/users/{userId}` | `UserProfileService` | Oui (Bearer) | Soi / ADMIN | Consultation d'un profil par UUID public ou ID numérique |
| `PUT` | `/v1/users/{userId}` | `UserProfileService` | Oui (Bearer) | Soi / ADMIN | Mise à jour des informations personnelles du profil |
| `DELETE` | `/v1/users/{userId}` | `UserProfileService` | Oui (Bearer) | Soi / ADMIN | Suppression d'un compte utilisateur |
| `GET` | `/v1/users/partners` | `UserProfileService` | Non | Public | Liste des commerçants partenaires pour rattachement |
| `POST` | `/v1/users/stores` | `UserProfileService` | Oui (Bearer) | MERCHANT / ADMIN | Rattachement d'un magasin à un commerçant partenaire |
| **Administration des Partenaires (Admin)** | | | | | |
| `GET` | `/v1/admin/partners` | `AdminPartnerService` | Oui (Bearer) | ADMIN | Liste des comptes partenaires avec filtrage optionnel (query, limit) |
| `GET` | `/v1/admin/partners/{id}` | `AdminPartnerService` | Oui (Bearer) | ADMIN | Fiche complète détaillée d'un partenaire avec ses boutiques associées |
| `PUT` | `/v1/admin/partners/{id}` | `AdminPartnerService` | Oui (Bearer) | ADMIN | Mise à jour des informations administratives d'un partenaire |
| `POST` | `/v1/admin/partners/{id}/stores` | `AdminPartnerService` | Oui (Bearer) | ADMIN | Rattachement d'une boutique existante à un compte partenaire |
| **Gestion des Véhicules & Plaques** | | | | | |
| `GET` | `/v1/users/me/vehicles` | `UserProfileService` | Oui (Bearer) | CLIENT | Liste des véhicules du client connecté |
| `POST` | `/v1/users/me/vehicles` | `UserProfileService` | Oui (Bearer) | CLIENT | Ajout d'une plaque d'immatriculation pour le client connecté |
| `PUT` | `/v1/users/me/vehicles/{vehicleId}` | `UserProfileService` | Oui (Bearer) | CLIENT | Modification d'une plaque d'immatriculation client |
| `DELETE` | `/v1/users/me/vehicles/{vehicleId}` | `UserProfileService` | Oui (Bearer) | CLIENT | Suppression d'une plaque d'immatriculation client |
| `GET` | `/v1/users/{userId}/vehicles` | `UserProfileService` | Oui (Bearer) | Soi / ADMIN | Liste des véhicules d'un utilisateur par son ID |
| `POST` | `/v1/users/{userId}/vehicles` | `UserProfileService` | Oui (Bearer) | Soi / ADMIN | Ajout d'un véhicule pour un utilisateur |
| `PUT` | `/v1/users/{userId}/vehicles/{vehicleId}` | `UserProfileService` | Oui (Bearer) | Soi / ADMIN | Modification d'un véhicule d'un utilisateur |
| `DELETE` | `/v1/users/{userId}/vehicles/{vehicleId}` | `UserProfileService` | Oui (Bearer) | Soi / ADMIN | Suppression d'un véhicule d'un utilisateur |
| **Carte de Fidélité & Santé** | | | | | |
| `GET` | `/v1/users/loyalty-cards/{cardNumber}` | `LoyaltyCardIdentificationService` | Oui (Bearer) | Tous | Identification d'un client par son numéro de carte de fidélité |
| `GET` | `/v1/users/status` | `UsersStatusController` | Non | Public | État de santé opérationnel du microservice (`UP`) |
| **Cryptographie & Découverte Gateway** | | | | | |
| `GET` | `/.well-known/jwks.json` | `JwtTokenService` | Non | Public | Découverte JWKS standard RFC 7517 (racine de domaine) |
| `GET` | `/v1/auth/jwks.json` | `JwtTokenService` | Non | Public | Découverte JWKS sous le préfixe `/v1/auth` |
| `GET` | `/v1/auth/public-key` | `JwtTokenService` | Non | Public | Clé publique RSA (X.509 SPKI Base64) pour vérification JWT |
| `GET` | `/v1/auth/revoked-tokens` | `TokenRevocationService` | Non | Public | Liste des JTIs des jetons révoqués (synchro de caches) |
| `GET` | `/v1/auth/check-revocation` | `TokenRevocationService` | Non | Public | Vérification de révocation d'un JTI spécifique |
| `GET` | `/v1/auth/verify` | `JwtAuthInterceptor` | Oui (Bearer/Cookie)| Tous | Endpoint ForwardAuth retournant les headers d'identité |

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
