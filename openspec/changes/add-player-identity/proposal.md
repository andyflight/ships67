# Proposal

## Why

The backend is an empty Spring Boot skeleton, and every other domain (game config, economy, ship progress, ads,
purchases) needs a player id and an authenticated session first. This change also fixes the backend's code
structure (clean architecture) before the first domain lands, so later changes follow one pattern.

## What Changes

- Establish the backend architecture: clean architecture layers (`domain` <- `application` <- `infrastructure` /
  `api`) inside each domain package, enforced by architecture tests. Pragmatic exception: domain entities may carry
  JPA annotations (`@Entity`). Update `backend/CLAUDE.md` to describe it.
- Add backend libraries: Spring Security, OAuth2 resource server / JOSE (JWT issue + validation), Bean Validation,
  ArchUnit and Spring Security test support.
- **Guest sign-in**: a client with no session calls one endpoint and gets a new player, a session (short-lived
  access JWT + opaque rotating refresh token) and a long-lived **guest credential**. No login screen.
- **Account-loss guard**: the guest credential restores a session after the refresh token is lost, expired or
  revoked; a short grace window makes a retried refresh (lost response) safe instead of revoking the session.
- **Sessions**: refresh with rotation and reuse detection (reusing an old refresh token outside the grace window
  revokes the whole family), logout.
- **Player account**: every player gets a UUID, a unique public tag and a display name; the player can read their
  profile and change the display name.
- **Prepared for account linking (design only)**: one player has many auth identities (`provider` + `subject`,
  unique). Only `GUEST` is implemented; Game Center, Play Games and Apple are designed for a later change.
- The full HTTP contract (paths, request/response JSON, statuses) is part of the specs; errors are RFC 9457
  problem+json, including 401/403 from security.

Out of scope: platform sign-in, account linking endpoints, account deletion, device attestation, game progress.

## Capabilities

### New Capabilities

- `player-auth`: how a client obtains, renews, restores and ends a session - guest sign-in, guest credential,
  access/refresh tokens, rotation, reuse detection and grace window, logout, auth identities, and how the API rejects
  unauthenticated calls.
- `player-account`: the player record exposed to the client - id, public tag, display name, linked identity
  providers, reading the profile and renaming.

### Modified Capabilities

None. (Existing specs `art-pipeline` and `art-ship-modularity` are unaffected.)

## Impact

- **Backend code**: new packages `common/`, `player/`, `auth/` under `com.games.shipmaster`, each layered.
- **Dependencies**: `backend/build.gradle` gains the libraries above.
- **Database**: Liquibase changesets 001-003 (players, auth identities, refresh tokens).
- **Configuration**: new required env var `SHIPMASTER_JWT_SECRET`; test profile gets a fixed test-only secret.
- **API**: new endpoints under `/api/v1/auth/*` and `/api/v1/players/me`; all other `/api/v1/**` require a bearer token.
- **Unity client**: HTTP client that signs in as guest on first launch, stores the guest credential in storage that
  survives reinstall (iOS Keychain, Android Block Store), stores the refresh token, attaches the bearer token,
  refreshes on 401 and restores via the guest credential instead of ever silently creating a new account.
- **Docs**: `backend/CLAUDE.md` (architecture section), `backend/README.md` (env var, auth flow).
