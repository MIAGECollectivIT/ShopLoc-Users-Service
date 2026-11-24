# AUDIT_REPORT — ShopLoc-Users-Service (`User`)

> Audit en lecture seule. Aucun fichier source modifié. Chemins relatifs à `src/main/java/fr/miage/collectivit/usersservice/` (noté `…/`) sauf mention contraire.

## 1. Carte d'identité & Baseline

| Élément | Valeur |
|---|---|
| Build | Maven (`pom.xml`), wrapper `mvnw` |
| Java / Spring Boot | 21 / **4.1.1** |
| Persistance | PostgreSQL + JPA/Hibernate, **Flyway** (`spring-boot-starter-flyway`), H2 en test |
| Kafka | Non |
| Sécurité | JWT RS256 (jjwt 0.12.6) via `JwtAuthInterceptor` (MVC) ; `spring-security-crypto` seul (pas de Spring Security) |
| Clients HTTP | Aucun |
| Autres | Mail (SMTP), ZXing (QR), springdoc 2.8.5, Lombok, PMD configuré (`pmd-ruleset.xml`) |
| Taille | ~5 560 lignes Java main, 11 migrations SQL |
| Compilation | `./mvnw test-compile` : ✅ **BUILD SUCCESS** |
| `dependency:analyze` | exécuté (cf. §2.4) |
| Temps de démarrage | **Non mesuré** : nécessite PostgreSQL (`localhost:5432`) non lancé ; analyse statique (cf. §2.3) |

## 2. Problèmes détectés

### 🔴 Critique

**C1 — Deux migrations Flyway avec la même version `V10`**
- `src/main/resources/db/migration/V10__add_store_uid_to_user_stores.sql`
- `src/main/resources/db/migration/V10__create_system_settings_table.sql`
- Flyway refuse de démarrer (`Found more than one migration with version 10`) sur une base neuve ou lors de la validation.
- Proposition : renommer l'une en `V11__…` (⚠️ si déjà appliquée en environnement, vérifier `flyway_schema_history` avant de renommer ; sinon utiliser `outOfOrder`/`repair`).

**C2 — Comptes seed avec mots de passe hachés versionnés (admin/merchant/clients réels nominatifs)**
- `src/main/resources/db/migration/V4__seed_default_users.sql:20-` (ex. `marius.vasseur@shop-loc.fr`, rôle `ADMIN`, hash bcrypt en dur).
- Un compte ADMIN provisionné en dur dans le dépôt, identique pour tous les environnements (y compris prod), est un risque d'attaque par dictionnaire hors ligne et de compte connu.
- Proposition : déplacer le seed dans une migration Flyway conditionnelle au profil (`locations` additionnelles via `FLYWAY_ADDITIONAL_LOCATIONS`, déjà prévu `application.yaml`) ou provisionner l'admin via variable d'environnement + changement de mot de passe obligatoire.

**C3 — Endpoint `GET /v1/auth/revoked-tokens` public (`security = {}`)**
- `…/controller/AuthenticationController.java:177-195` + liste blanche `…/security/JwtAuthInterceptor.java:41-44`.
- Expose publiquement la liste des JTI révoqués (fuite d'information sur les sessions, énumération). Utile aux autres services, mais devrait être restreint (réseau interne / clé de service / mTLS).
- Proposition : protéger par un secret inter-services ou ne l'exposer que sur le réseau interne (NetworkPolicy / path non routé par la gateway).

### 🟠 Majeur

**M1 — Pas de pagination sur plusieurs listes**
- `…/controller/AdminPartnerController.java:80` (`List<PartnerResponse>`), `…/controller/UserProfileController.java:152` (partners), `:384`, `:564` (vehicles), `AuthenticationController.java:194` (revoked JTIs, croissant sans borne).
- `AdminUserController.java:57` utilise bien `Pageable` (à généraliser avec `PageResponse` existant).

**M2 — Aucune sonde Actuator / pas de tracing distribué**
- `pom.xml` : pas de `spring-boot-starter-actuator`, pas de `micrometer-tracing`. Pas d'endpoints `/actuator/health/liveness|readiness` pour K3s, pas de corrélation de traces avec Sales/Store/Loyalty.
- Proposition : ajouter actuator + `management.endpoint.health.probes.enabled=true` + `micrometer-tracing-bridge-otel`.

**M3 — Dockerfile sans optimisation de démarrage (Boot ≥ 3 / Java 21)**
- `Dockerfile` : `java -jar app.jar` classique, pas d'AppCDS ni de jar en couches (`layertools`) ; `mvn dependency:go-offline` puis copie du `src` correct mais le JAR n'est pas extrait.
- Proposition : `-Dspring.aot.enabled=true` + AppCDS (`-XX:ArchiveClassesAtExit` en build, `-XX:SharedArchiveFile` au runtime) → gain estimé 30-40 % de temps de démarrage.

**M4 — Réglages de démarrage JPA/Hikari absents**
- `src/main/resources/application.yaml:11-17` : pas de `spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false` (appel métadonnées JDBC au démarrage ; à n'activer qu'en précisant le dialecte), pas de `spring.datasource.hikari.*` (taille du pool, `connection-timeout`, `maximum-pool-size`), `spring.main.lazy-initialization` non configuré.
- Positif : `ddl-auto: validate` (défaut), `open-in-view: false` ✅.

**M5 — `ddl-auto` surchargeable par variable d'environnement**
- `application.yaml:13` : `${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}`. Un `update` injecté en prod contournerait Flyway. Proposition : figer à `validate` hors profil `dev`.

**M6 — Contrôle d'accès par intercepteur MVC plutôt que Spring Security**
- `…/config/WebMvcConfig.java:17-19`, `…/security/JwtAuthInterceptor.java`. Pas de protection CSRF/headers de sécurité standard, listes blanches de chemins en dur (`:41-44`) à maintenir à la main, dualité de préfixes `/v1` et `/api/v1` (`@RequestMapping({"/v1","/api/v1"})` partout) qui double la surface d'exposition.

### 🟡 Mineur / Ergonomie

**m1 — Injection par champ `@Autowired`**
- `…/security/JwtTokenService.java:56` (`@Autowired(required = false) private TokenRevocationService`)
- `…/service/impl/AuthenticationServiceImpl.java:65` (`@Autowired` sur constructeur : OK, mais 2 constructeurs → ambigu, simplifier).
```java
// Avant
@Autowired(required = false)
private TokenRevocationService tokenRevocationService;
// Après
private final ObjectProvider<TokenRevocationService> tokenRevocationService; // injection constructeur
```

**m2 — `@Value` dispersés (≈ 28 occurrences) au lieu de `@ConfigurationProperties`**
- `…/config/OpenApiConfig.java:27-63` (13), `…/security/JwtTokenService.java:38-53` (6), `AuthenticationServiceImpl.java:52-53,73-74`, `AuthenticationController.java:55`, `SystemSettingServiceImpl.java:28`, `PhysicalCardEmailService.java:28-29`.
- Proposition : `@ConfigurationProperties("application.security.jwt") @Validated record JwtProperties(...)`.
- Notes : `JwtTokenService.java:38` lit un ancien `secret-key` en fallback de `private-key` (legacy à supprimer) ; secrets JWT correctement externalisés (`${JWT_PRIVATE_KEY:}`), `.env` non versionné ✅ ; mot de passe datasource par défaut `devpassword` (`application.yaml:10`) à retirer des valeurs par défaut.

**m3 — `catch (Exception)` larges (non vides, loggés)**
- `…/service/impl/PhysicalCardNotificationServiceImpl.java:90`, `…/event/PhysicalCardNotificationListener.java:32` — acceptables (statut FAILED + log) mais préférer des exceptions typées (`MailException`). Aucun `printStackTrace`, aucun catch vide ✅.

**m4 — Dépendances (`dependency:analyze`)**
- « Unused declared » : essentiellement des **faux positifs** (starters, drivers runtime, jjwt-impl/jackson, flyway, springdoc, deps de test).
- À traiter réellement : `com.google.zxing:core` + `javase` versions fixées en dur (3.5.3), `jjwt` 0.12.6 et `springdoc` 2.8.5 hors BOM — vérifier compatibilité Boot 4.1 (springdoc 2.8.x ciblant Boot 3 ; springdoc 3.x est la ligne Boot 4).
- Classes utilisées mais non déclarées : `jackson-databind`, `jsr310`, `spring-context`, `spring-data-*`, `jakarta.validation-api`, `slf4j` (transitives, tolérable).
- Doublons fonctionnels : aucun. Starter surdimensionné : non (service web légitime).

**m5 — Validation & docs**
- `@Valid` absent sur `LoyaltyCardController` (`GET /loyalty-cards/{cardNumber}`) et `UsersStatusController` : ajouter `@Pattern` sur `cardNumber`.
- `@RestControllerAdvice` + `ProblemDetail` ✅ (`…/exception/GlobalExceptionHandler.java:15`), springdoc ✅, aucune entité JPA exposée par contrôleur ✅ (DTOs `dto/`), aucun client HTTP donc pas de timeouts à définir ✅, pas de N+1 détecté (aucune association `@OneToMany`/`@ManyToOne` LAZY détectée dans `entity/`).

## 3. Plan d'action priorisé (checklist de validation)

| # | Action | Gain | Effort |
|---|---|---|---|
| 1 | Renommer une des deux migrations `V10` (C1) | Démarrage garanti sur base neuve | Faible |
| 2 | Retirer/segmenter le seed ADMIN de `V4` (C2) | Suppression du compte admin connu | Moyen |
| 3 | Protéger `/auth/revoked-tokens` (C3) | Réduit fuite d'infos | Faible |
| 4 | Ajouter Actuator + probes + micrometer-tracing (M2) | Déploiement K8s fiable, traçabilité | Faible |
| 5 | Paginer les listes (M1) | Scalabilité mémoire | Moyen |
| 6 | AOT + AppCDS dans Dockerfile (M3) | −30-40 % démarrage | Moyen |
| 7 | Hikari + `use_jdbc_metadata_defaults` + dialecte (M4) | −100-300 ms démarrage, pool maîtrisé | Faible |
| 8 | Figer `ddl-auto=validate` hors dev (M5) | Sécurité du schéma | Faible |
| 9 | Migrer `@Value` vers `@ConfigurationProperties` (m2) | Config typée/validée | Moyen |
| 10 | Injection par constructeur (m1) | Testabilité | Faible |
| 11 | Vérifier compat. springdoc 2.8.5 / Boot 4.1 (m4) | Évite régressions | Faible |
| 12 | Unifier `/v1` vs `/api/v1` (M6) | Surface API réduite | Moyen |


## 4. Annexe — Build Docker (mesuré, build à froid `--no-cache`)

| Étape | Durée |
|---|---|
| `RUN mvn dependency:go-offline -B` (builder 4/6) | **59.8 s** (≈ 1378 artefacts téléchargés) |
| `RUN mvn clean package -DskipTests` (builder 6/6) | 4.5 s |
| Copie JAR + image finale | ~2 s (image 418 Mo) |
| **Total** | **68 s** |

- 🟠 **D1 — `dependency:go-offline` re-télécharge tout à chaque invalidation du cache** (`Dockerfile`, `RUN mvn dependency:go-offline -B`) : il ramène aussi des plugins/dépendances jamais utilisés au build. Aucun cache Maven persistant entre builds. Le cache de couche saute dès que `pom.xml` change, ou après `--no-cache` / prune.
- Proposition (gain estimé : ~50-70 s → ~5-10 s sur rebuild après modif du `pom.xml`) :
```dockerfile
# syntax=docker/dockerfile:1.7
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:resolve-plugins dependency:resolve
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -o clean package -DskipTests
```
- `mvnw`/`.mvn` copiés dans le contexte mais non utilisés : le conteneur utilise l'image `maven:3.9` (OK, mais version Maven non épinglée au wrapper).
- Compilation seule : ~4 s → le build applicatif n'est pas le problème, seul le téléchargement des dépendances l'est.
