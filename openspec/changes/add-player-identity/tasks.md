# Tasks

## 1. Dependencies and configuration

- [x] 1.1 [backend] Add to `backend/build.gradle`: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-validation`, test `spring-boot-starter-security-test` and `archunit-junit5` (1.4.x, pinned) per design D3; verify `./gradlew compileJava` succeeds
- [x] 1.2 [backend] Add `shipmaster.auth.jwt-secret=${SHIPMASTER_JWT_SECRET}`, `issuer`, `access-ttl=15m`, `refresh-ttl=60d`, `refresh-grace=30s` to `application.properties`; fixed test-only secret in `application-test.properties`; `AuthProperties` record validated at startup (secret present, >= 32 bytes); verify with a test that a short secret fails context startup

## 2. Architecture skeleton and common layer

- [x] 2.1 [backend] Create `common/{domain,api,infrastructure}` per D1: `DomainException` subclasses, `ClockConfig`; verify `./gradlew compileJava`
- [x] 2.2 [backend] Add `ArchitectureTest` (ArchUnit) enforcing D1: domain uses only `java.*`, Lombok, `jakarta.persistence`; application has no infrastructure/api deps; api does not access domain entities, repositories or infrastructure; nothing depends on api; verify it passes on the skeleton and fails on a deliberately wrong test fixture class
- [x] 2.3 [backend] `ProblemDetailsAdvice` mapping domain exceptions and validation errors to problem+json with `errors` (D7); verify with a `@WebMvcTest` over a test-only controller
- [x] 2.4 [backend] Update `backend/CLAUDE.md` structure and rules sections with the layered layout, the `@Entity` exception and the dependency rule (D1, D2); verify the described packages match the code tree

## 3. Player domain

- [x] 3.1 [backend] Liquibase `001-create-player.yaml` + include in master (D10); verify `@DataJpaTest` context starts with `ddl-auto=validate`
- [x] 3.2 [backend] `player.domain`: `PlayerTag`, `DisplayName` (3-16 chars, Unicode letters/digits/space/`_`/`-`, trimmed), `Player` entity (D2), ports; verify plain JUnit tests for tag format, name edges (2/3/16/17 chars, trimming, invalid chars, Cyrillic accepted) and rename
- [x] 3.3 [backend] `player.infrastructure`: `PlayerJpaRepository` implementing the port, `SecureRandomPlayerTagGenerator`; verify `@DataJpaTest` round-trips a player and a duplicate tag violates the unique constraint
- [x] 3.4 [backend] `player.application`: `CreateGuestPlayer` (default name `Sailor` + 4 digits, tag retry up to 5), `GetPlayerProfile`, `RenamePlayer`; verify unit tests with fake ports, incl. tag-collision retry and unknown player -> `Unauthorized`

## 4. Auth domain

- [x] 4.1 [backend] Liquibase `002-create-auth-identity.yaml` and `003-create-refresh-token.yaml` + includes (D10); verify `@DataJpaTest` starts and `(provider, subject)` and `credential_hash` uniqueness are enforced
- [x] 4.2 [backend] `auth.domain`: `AuthProvider`, `AuthIdentity`, `RefreshToken` with the refresh decision logic of D4 (unused / grace retry / reuse) as pure methods taking `Instant now`, ports incl. `IdentityVerifier` (no implementation); verify unit tests for every branch of the D4 table, incl. grace boundary at exactly 30 s
- [x] 4.3 [backend] `auth.infrastructure`: Spring Data repos implementing the ports (lookup by hash with `PESSIMISTIC_WRITE`, revoke-family update), `Sha256SecretCodec`, `JwtAccessTokenIssuer` (Nimbus HS256); verify `@DataJpaTest` for family revocation and a unit test that an issued JWT decodes with expected `sub`, `iss`, `exp`
- [x] 4.4 [backend] `auth.application`: `SignInAsGuest` (player + GUEST identity + credential + session), `RestoreGuestSession`, `RefreshSession`, `Logout` (D4, D5); verify unit tests with a fixed `Clock`: rotation, grace retry, reuse revokes family, restore after revocation and after refresh expiry, logout of unknown token is a no-op and leaves the guest credential valid

## 5. Security and API

- [x] 5.1 [backend] `SecurityConfig` (D6) with problem+json entry point and access-denied handler, `@CurrentPlayer` resolver; verify `@WebMvcTest` that missing, tampered and expired tokens return 401 problem+json and public auth paths need no token
- [x] 5.2 [backend] `AuthController` + DTO records for `/api/v1/auth/guest`, `/guest/restore`, `/refresh`, `/logout`; verify `@WebMvcTest` for every status and body shape in `specs/player-auth`
- [x] 5.3 [backend] `PlayerController` + DTOs for `GET`/`PATCH /api/v1/players/me`; verify `@WebMvcTest` for every status and body shape in `specs/player-account`
- [x] 5.4 [backend] Update `backend/README.md`: `SHIPMASTER_JWT_SECRET` (`openssl rand -base64 32`), auth flow summary and a pointer to `openspec/specs/player-auth` as the API contract; verify `./gradlew bootRun` starts with the variable set and fails clearly without it

## 6. Integration

- [x] 6.1 [backend] `@SpringBootTest` end-to-end: guest sign-in -> `GET /players/me` -> rename -> refresh -> retry old token within grace (200) -> use new token -> replay old token (401, family revoked) -> restore with guest credential (same `playerId`, renamed name kept) -> logout -> restore again (200); verify it passes and `./gradlew test` is green overall

## 7. Unity client

- [ ] 7.1 [unity] `ApiClient` (UnityWebRequest, base URL ScriptableObject, problem+json -> `ApiException`) per D11; verify an EditMode test parses a sample problem+json body
- [ ] 7.2 [unity] `ICredentialStore` with a `PlayerPrefs` implementation and `AuthSession` startup flow from D11 (refresh -> restore -> guest only when no credential; guest credential saved before refresh token; no silent new account); verify EditMode tests with a fake HTTP layer for each startup branch
- [ ] 7.3 [unity] Verify in Play mode against local `./gradlew bootRun`: second launch keeps the same `playerId`; deleting only the refresh token from storage still restores the same `playerId`; `tag` and `displayName` are shown in a debug label

## Workflow follow-up

- Native durable credential stores (iOS Keychain, Android Block Store plugins) - separate [unity] change before release.
- Platform sign-in and linking per design D8 - separate change.
- Archive with `/opsx:archive add-player-identity` after both roles' tasks are done and reviewed.
