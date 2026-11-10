# Design

## Context

`backend/` is a bare Spring Boot 4.1 / Java 25 skeleton: `BackendApplication`, a context-load test, an empty
Liquibase master changelog, H2 in PostgreSQL mode, `ddl-auto=validate`, problem details enabled. No security, no
domain code. `backend/CLAUDE.md` already fixes: package = domain, DTOs as records, UUID + Instant, Liquibase only,
secrets from env, problem+json errors. `backend/.claude/platform-flows.md` describes the agreed auth direction.
See proposal.md for motivation. The HTTP contract and behavior are in `specs/player-auth` and
`specs/player-account`; this document does not repeat them.

## Goals / Non-Goals

**Goals:**
- One clean-architecture layout every later domain copies, enforced by tests rather than convention.
- Guest flow end to end, with no path where a retry, an expiry or a lost refresh token loses the account while the
  device still holds its guest credential.
- A data model and ports that make Game Center / Play Games / Apple linking an additive change.
- Security failures look like every other API error (problem+json).

**Non-Goals:**
- Platform verifiers, linking/conflict endpoints, account deletion (D8 is design only).
- OpenAPI/Swagger generation: the contract lives in the OpenSpec specs.
- Rate limiting, device attestation, admin APIs, use-case input-port interfaces, CQRS, domain events.

## Decisions

### D1. Clean architecture inside domain packages

```
com.games.shipmaster
+-- BackendApplication
+-- common/
|   +-- domain/          DomainException hierarchy (NotFound, Conflict, InvalidInput, Unauthorized)
|   +-- api/             ProblemDetailsAdvice, SecurityConfig (filter chain), SecurityProblemHandler
|   |                    (401/403 -> problem+json), @CurrentPlayer UUID argument resolver
|   +-- infrastructure/  ClockConfig (java.time.Clock bean, UTC)
+-- player/
|   +-- domain/          Player (@Entity), PlayerTag, DisplayName (value objects),
|   |                    ports: PlayerRepository, PlayerTagGenerator
|   +-- application/     CreateGuestPlayer, GetPlayerProfile, RenamePlayer (one class per use case),
|   |                    PlayerProfile (result record)
|   +-- infrastructure/  PlayerJpaRepository (Spring Data, implements PlayerRepository),
|   |                    SecureRandomPlayerTagGenerator
|   +-- api/             PlayerController (/api/v1/players/me), dto/ PlayerProfileResponse, RenamePlayerRequest
+-- auth/
    +-- domain/          AuthIdentity (@Entity), AuthProvider, RefreshToken (@Entity),
    |                    ports: AuthIdentityRepository, RefreshTokenRepository, AccessTokenIssuer,
    |                    SecretCodec (generate + hash), IdentityVerifier (future providers, no impl)
    +-- application/     SignInAsGuest, RestoreGuestSession, RefreshSession, Logout; SessionIssuer (shared
    |                    helper); Session, GuestSession (result records); SessionPolicy; AuthIdentityProvidersLookup
    +-- infrastructure/  Spring Data repos implementing the ports, JwtAccessTokenIssuer (Nimbus HS256),
    |                    Sha256SecretCodec, AuthConfig (encoder/decoder beans), AuthProperties
    +-- api/             AuthController (/api/v1/auth/*), dto/ records
```

Dependency rule (arrows point inward):

```
   api ----------> application ----------> domain
   infrastructure -----^  \_______________^
```

- `domain`: entities with behavior, value objects, repository and service ports. May use `java.*`, Lombok and
  `jakarta.persistence` (the agreed exception). No Spring, no Jackson, no `jakarta.servlet`/`validation`.
- `application`: use cases. Domain + Spring `@Service` / `@Transactional` only. Returns result records, never
  entities, so lazy-loading never leaks (`open-in-view=false`).
- `infrastructure`: implements ports. A Spring Data interface extends both `JpaRepository<X, UUID>` and the domain
  port, so no adapter class is needed.
- `api`: controllers + DTO records + Bean Validation; calls application only. Never touches domain entities,
  repositories or infrastructure.
- Cross-domain calls go through the other domain's application layer (`SignInAsGuest` -> `CreateGuestPlayer`).
  The reverse direction (player profile needs linked providers) is an application-level interface
  `PlayerProvidersLookup` in player, implemented in auth, so there is no package cycle.
- SecurityConfig lives in `common/api` (it is web wiring and must not be depended on by inner layers); it only uses
  Spring's `JwtDecoder` type, whose bean comes from `auth/infrastructure/AuthConfig`.
- Player ids are plain `UUID` across layers (no `PlayerId` wrapper) to keep mapping out of controllers.
- An ArchUnit `ArchitectureTest` enforces all of the above.

Keeps the existing CLAUDE.md rule "package = domain, not layer" at the top level.

*Alternatives:* pure POJO domain + separate JPA entities and mappers - cleaner but doubles the model classes for
little gain at this size (rejected by the user). Gradle sub-modules per layer - compile-time enforcement but heavy;
ArchUnit gives the same guarantee in tests.

### D2. Entities in the domain

Entities use field access, a protected no-arg constructor for JPA, static factories (`Player.newGuest(...)`) and
behavior methods (`rename`, `RefreshToken.rotate(now)`). No public setters, no `@Data`. Value objects (`PlayerTag`,
`DisplayName`) validate in their constructors; the entity stores them as `String` columns and exposes the value
object from getters, so no JPA converters are needed. IDs are `UUID` assigned in the factory (not DB-generated) so
domain code knows the id before saving.

### D3. Libraries added to `build.gradle`

| Dependency | Why |
|---|---|
| `spring-boot-starter-security` | filter chain, stateless config |
| `spring-boot-starter-oauth2-resource-server` | bearer JWT validation; brings Nimbus JOSE, also used to *issue* tokens |
| `spring-boot-starter-validation` | `@Valid` on request records |
| test: `spring-boot-starter-security-test` | `jwt()` request post-processor |
| test: `com.tngtech.archunit:archunit-junit5` (1.4.x, pinned) | architecture rule test |

No jjwt: `NimbusJwtEncoder` / `NimbusJwtDecoder` cover issue and validation. No springdoc.

### D4. Tokens and credentials

| Credential | Format | Stored | Lifetime | Rotates | Revoked by |
|---|---|---|---|---|---|
| Access token | JWT HS256: `iss=shipmaster`, `sub=playerId`, `iat`, `exp`, `jti` | not stored | 15 min | - | expiry |
| Refresh token | 32 random bytes, base64url | SHA-256 hex | 60 days | every use | reuse, logout |
| Guest credential | `g1.` + 32 random bytes base64url | SHA-256 hex on the GUEST identity | none | never | (future: account deletion) |

- `iat`/`exp` are truncated to whole seconds (JWT precision) so `accessTokenExpiresAt` equals the token's `exp`.
- JWT signed with `SHIPMASTER_JWT_SECRET` (>= 32 bytes, base64); startup fails if missing or short. Moving to RS256
  later only touches the issuer and decoder bean.
- SHA-256 without salt is fine for 256-bit random secrets (unlike passwords); lookup is by hash (unique index).
- The `g1.` prefix versions the credential format and makes it recognizable in logs/leak scanners.
- `SecretCodec` generates and hashes both secret types.

**Refresh algorithm** (row locked with `PESSIMISTIC_WRITE` by hash):

```
token not found or expired or revoked          -> 401
token unused                                    -> mark used_at=now, insert successor (same family),
                                                   set replaced_by=successor.id, return session
token used AND used_at > now-30s
     AND successor not used and not revoked     -> revoke successor, insert new successor, return session
token used otherwise                            -> revoke whole family, 401   (reuse detected)
```

The grace window (`shipmaster.auth.refresh-grace=30s`) is the guard against the most common real-world loss: the
server rotates, the response is lost on a mobile network, the client retries with the old token.

TTLs and grace are in `AuthProperties` (`shipmaster.auth.*`). A `Clock` bean is injected wherever time is read, so
tests move a fixed clock instead of sleeping.

### D5. Account-loss guard (layers)

```
 refresh token lost / expired / revoked        guest credential lost
              |                                         |
              v                                         v
   POST /auth/guest/restore  <--- server ---      client storage that survives
   (guest credential)                             reinstall (D11) makes this rare;
              |                                   platform linking (D8, next change)
              v                                   removes it for linked players
     same player, new session
```

1. **Grace window** (D4) - retried refresh is not treated as theft.
2. **Guest credential** - independent of the refresh family; restores the same player after expiry (player away
   > 60 days), logout, reuse revocation, or a server-side session wipe.
3. **Durable client storage** (D11) - iOS Keychain survives uninstall; Android Block Store survives uninstall and
   restores onto a new device with Google backup on.
4. **Client never silently creates a second account** (D11) - if it has a guest credential, it restores; it calls
   `POST /auth/guest` only when it has no credential at all.
5. **Platform linking** (D8, later) - Game Center / Play Games identity is the long-term recovery path.

Trade-off: a non-expiring static secret is weaker than rotating tokens if a device is compromised. Accepted for
guest accounts; mitigations later are App Attest / Play Integrity on restore and revocation on account deletion.

### D6. Security filter chain

Stateless, CSRF off (no cookies), CORS off (native client). `permitAll`: `POST /api/v1/auth/guest`,
`/api/v1/auth/guest/restore`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, `/h2-console/**` (dev profile only,
same-origin frames). Everything else under `/api/v1/**` authenticated via resource-server JWT. Custom
`AuthenticationEntryPoint` and `AccessDeniedHandler` write `ProblemDetail` JSON. Controllers receive the player as
`@CurrentPlayer PlayerId` (resolver reads `sub` from the `Jwt` principal), never parse JWTs themselves.

### D7. Errors

`common/domain` defines `DomainException` subclasses; `common/api/ProblemDetailsAdvice` maps them: NotFound -> 404,
Conflict -> 409, InvalidInput -> 400, Unauthorized -> 401; `MethodArgumentNotValidException` -> 400 with an
`errors` property (`[{ "field", "message" }]`). `type` URIs `https://shipmaster.games/problems/<slug>` are
identifiers only.

### D8. Account linking (design only, future change)

```
Port (auth.domain):  IdentityVerifier { AuthProvider provider(); VerifiedSubject verify(ProviderCredential c); }

POST /api/v1/auth/{provider}                       sign in with a platform credential
     identity known   -> 200 session for its player
     identity unknown -> 201 new player + identity (+ guestCredential not issued)
POST /api/v1/players/me/identities/{provider}      link to current player
     -> 201 linked
     -> 409 problem+json with conflictPlayer { tag, displayName, createdAt } if owned by another player
POST /api/v1/players/me/identities/{provider}/resolve  { "keep": "current" | "other" }
     keep current -> identity moved to current player (other player keeps its guest credential)
     keep other   -> 200 session for the other player; client replaces its stored credentials
```

Client flow: after guest session, silently sign in to Game Center / Play Games, call link; on 409 show the
"load existing account?" dialog. Schema needed: only the `auth_identity` table and unique `(provider, subject)` added
now. `AuthProvider` already lists the providers; only `GUEST` has code paths.

### D9. Endpoints (summary; contract in specs)

| Method | Path | Auth | Success | Errors |
|---|---|---|---|---|
| POST | `/api/v1/auth/guest` | public | 201 session + `guestCredential` | - |
| POST | `/api/v1/auth/guest/restore` | public | 200 session | 400, 401 |
| POST | `/api/v1/auth/refresh` | public | 200 session | 400, 401 |
| POST | `/api/v1/auth/logout` | public | 204 | 400 |
| GET | `/api/v1/players/me` | bearer | 200 profile | 401 |
| PATCH | `/api/v1/players/me` | bearer | 200 profile | 400, 401 |

Request bodies: restore `{ "guestCredential": "g1.…" }`, refresh/logout `{ "refreshToken": "…" }`, rename
`{ "displayName": "…" }`. Default display name: `Sailor` + 4 random digits.

### D10. Liquibase changesets

`db/changelog/changes/`, each included in `db.changelog-master.yaml`, PostgreSQL-compatible types only:

- **`001-create-player.yaml`** - `player`: `id uuid pk`, `tag varchar(9) not null unique`,
  `display_name varchar(16) not null`, `created_at timestamp with time zone not null`,
  `version bigint not null default 0` (optimistic lock, later reused as player state version).
- **`002-create-auth-identity.yaml`** - `auth_identity`: `id uuid pk`,
  `player_id uuid not null fk -> player(id) on delete cascade`, `provider varchar(32) not null`,
  `subject varchar(255) not null`, `credential_hash varchar(64) null unique` (GUEST only),
  `created_at timestamptz not null`, `last_used_at timestamptz null`; unique `(provider, subject)`; index `(player_id)`.
  GUEST subject = random UUID generated by the server.
- **`003-create-refresh-token.yaml`** - `refresh_token`: `id uuid pk`,
  `player_id uuid not null fk -> player(id) on delete cascade`, `family_id uuid not null`,
  `token_hash varchar(64) not null unique`, `replaced_by uuid null`, `created_at timestamptz not null`,
  `expires_at timestamptz not null`, `used_at timestamptz null`, `revoked_at timestamptz null`;
  index `(family_id)`, index `(player_id)`.

### D11. Unity client impact

- `ApiClient` (UnityWebRequest), base URL from a ScriptableObject, problem+json parsed into an `ApiException`.
- `ICredentialStore` with two values: guest credential (durable) and refresh token (session).
  - Durable store: iOS Keychain (survives uninstall), Android Block Store (survives uninstall / device restore).
    Needs a small native plugin per platform; until then an `PlayerPrefs` implementation behind the same interface
    for Editor and dev builds.
- Startup:
  ```
  has refresh token?  -> POST /auth/refresh   -- 401 --> restore
  restore: has guest credential? -> POST /auth/guest/restore -- 401 --> show "account not found" (no silent new)
  no credentials at all -> POST /auth/guest, store guestCredential FIRST, then refresh token
  ```
- Requests attach `Authorization: Bearer`; on 401 refresh once and retry; a failed refresh falls back to restore.
- Show `tag` and `displayName` from `GET /players/me`.

## Risks / Trade-offs

- [Guest credential lost together with the device and no backup] -> unrecoverable until platform linking; the
  client nudges linking (D8 change).
- [Static guest credential leaked from a compromised device] -> attacker can restore the account; accepted for
  guests; App Attest / Play Integrity on restore later.
- [HS256 shared secret leak lets anyone mint tokens] -> env only, >= 256 bits, rotate by redeploy; RS256 later.
- [Retry arrives after the grace window] -> family revoked, but guest credential restores the session; no loss.
- [`@Entity` in domain couples domain to JPA] -> accepted; ArchUnit still forbids Spring in domain, keeping use
  cases and entities unit-testable without a context.
- [Tag collisions] -> 32^8 ~ 1.1e12; generator retries on unique-constraint violation (max 5).
- [Orphan guest players from retried `POST /auth/guest`] -> harmless; later cleanup of players never restored or
  refreshed.

## Implementation Notes

- `DATABASE_TO_LOWER=TRUE` was removed from the H2 URLs: Liquibase's H2 support upper-cases its own table names and
  failed on the second start against the same database. Unquoted identifiers work the same on H2 and PostgreSQL.
- Repository ports declare `<S extends X> S save(S)` (Spring Data's signature) and the Spring Data interfaces
  redeclare `save`/`findById`, otherwise calls through the Jpa interface are ambiguous.

## Migration Plan

Greenfield: no existing data. Local dev must set `SHIPMASTER_JWT_SECRET` before `./gradlew bootRun`
(documented in README). Rollback = revert the change; no production database exists yet.
