# Spec Delta

## Purpose

Defines how the game client obtains, renews, restores and ends an authenticated session with the backend, the HTTP
contract for it, and how auth identities (guest now, platform providers later) map to one player.

## ADDED Requirements

### Requirement: Session response
Every endpoint that starts or renews a session SHALL return this JSON body: `playerId` (UUID string),
`accessToken` (string), `accessTokenExpiresAt` (ISO-8601 UTC), `refreshToken` (string),
`refreshTokenExpiresAt` (ISO-8601 UTC).

#### Scenario: Session body shape
- **WHEN** `POST /api/v1/auth/refresh` succeeds
- **THEN** the body is `{ "playerId": "3f1c…", "accessToken": "eyJ…", "accessTokenExpiresAt": "2026-11-10T12:15:00Z", "refreshToken": "q8Zk…", "refreshTokenExpiresAt": "2027-01-09T12:00:00Z" }`

### Requirement: Guest sign-in
The system SHALL let a client without a session create a new player with a `GUEST` auth identity in one call,
without user input. The response SHALL be the session response plus `guestCredential` (string), returned only once.

#### Scenario: First launch creates a player
- **WHEN** the client calls `POST /api/v1/auth/guest` with no body and no token
- **THEN** the response is `201` with the session fields and a non-empty `guestCredential`
- **AND** a new player exists with exactly one auth identity, provider `GUEST`

#### Scenario: Each call creates a separate player
- **WHEN** `POST /api/v1/auth/guest` is called twice
- **THEN** two different `playerId` values are returned

### Requirement: Guest credential restores the account
The guest credential SHALL not expire, SHALL not rotate and SHALL stay valid when refresh tokens expire, are
revoked or are lost. The server SHALL store only its hash. Presenting it SHALL start a new session for its player.

#### Scenario: Restore after losing the refresh token
- **WHEN** the client calls `POST /api/v1/auth/guest/restore` with `{ "guestCredential": "<credential>" }`
- **THEN** the response is `200` with the session response for the same `playerId` as at sign-in

#### Scenario: Restore after reuse revocation
- **GIVEN** the player's refresh token family was revoked by reuse detection
- **WHEN** the client calls `POST /api/v1/auth/guest/restore` with the guest credential
- **THEN** the response is `200` with a new session for the same player

#### Scenario: Unknown credential
- **WHEN** the client calls `POST /api/v1/auth/guest/restore` with a credential that was never issued
- **THEN** the response is `401` with content type `application/problem+json`

### Requirement: Access token
The system SHALL issue a signed access token valid for 15 minutes that identifies the player, and SHALL accept it
as `Authorization: Bearer <token>` on protected endpoints.

#### Scenario: Valid token is accepted
- **WHEN** the client calls `GET /api/v1/players/me` with a valid, unexpired access token
- **THEN** the response is `200`

#### Scenario: Expired token is rejected
- **WHEN** the client calls `GET /api/v1/players/me` with an access token older than 15 minutes
- **THEN** the response is `401` with content type `application/problem+json`

### Requirement: Protected API by default
Every endpoint under `/api/v1/**` SHALL require a valid access token, except `POST /api/v1/auth/guest`,
`POST /api/v1/auth/guest/restore`, `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout`.

#### Scenario: Missing token
- **WHEN** the client calls `GET /api/v1/players/me` without an `Authorization` header
- **THEN** the response is `401` with content type `application/problem+json`

#### Scenario: Tampered token
- **WHEN** the client sends an access token whose signature does not verify
- **THEN** the response is `401` with content type `application/problem+json`

### Requirement: Refresh token rotation
The system SHALL issue opaque refresh tokens valid for 60 days, store only their hash, and on each successful
refresh SHALL invalidate the presented token and return a new session response.

#### Scenario: Successful refresh
- **WHEN** the client calls `POST /api/v1/auth/refresh` with `{ "refreshToken": "<current>" }`
- **THEN** the response is `200` with a new `accessToken` and a new `refreshToken` for the same `playerId`
- **AND** the presented refresh token can no longer be used after the grace window

#### Scenario: Unknown or expired refresh token
- **WHEN** the client calls `POST /api/v1/auth/refresh` with a token that was never issued or is past its expiry
- **THEN** the response is `401` with content type `application/problem+json`

### Requirement: Refresh retry grace window
If a refresh token was rotated less than 30 seconds ago and its successor has not been used yet, presenting it
again SHALL succeed: the unused successor is invalidated and a new session is returned. This makes a retry after a
lost response safe.

#### Scenario: Retry after a lost response
- **GIVEN** refresh token A was exchanged for B 5 seconds ago and B was never used
- **WHEN** the client calls `POST /api/v1/auth/refresh` with A
- **THEN** the response is `200` with a new refresh token C
- **AND** B can no longer be used, C can

### Requirement: Refresh token reuse detection
Presenting a refresh token that was already rotated, outside the grace window or after its successor was used, SHALL
revoke every refresh token of the same sign-in (the token family). The guest credential is not affected.

#### Scenario: Old token replayed
- **GIVEN** refresh token A was exchanged for B and B was then used
- **WHEN** the client calls `POST /api/v1/auth/refresh` with A
- **THEN** the response is `401`
- **AND** every refresh token of that family now returns `401`

### Requirement: Logout
`POST /api/v1/auth/logout` with `{ "refreshToken": "<token>" }` SHALL revoke that token's family and respond `204`,
also when the token is unknown, so clients can always clear local state. It SHALL not revoke the guest credential.

#### Scenario: Logout revokes the session
- **WHEN** the client calls `POST /api/v1/auth/logout` with its current refresh token
- **THEN** the response is `204`
- **AND** `POST /api/v1/auth/refresh` with that token returns `401`

#### Scenario: Logout with unknown token
- **WHEN** the client calls `POST /api/v1/auth/logout` with a token that does not exist
- **THEN** the response is `204`

### Requirement: Auth identities
A player SHALL be able to own several auth identities, each a pair of provider and provider subject; a given pair
SHALL belong to at most one player. Implemented provider: `GUEST`. Reserved for later: `GAME_CENTER`, `PLAY_GAMES`,
`APPLE`.

#### Scenario: Identity uniqueness
- **WHEN** an identity with the same provider and subject as an existing one is stored for another player
- **THEN** the system rejects it and the existing identity is unchanged

### Requirement: Malformed auth requests
Auth endpoints SHALL validate request bodies and respond `400` with problem+json listing the invalid fields.

#### Scenario: Missing refresh token field
- **WHEN** the client calls `POST /api/v1/auth/refresh` with `{}`
- **THEN** the response is `400` with content type `application/problem+json`

#### Scenario: Missing guest credential field
- **WHEN** the client calls `POST /api/v1/auth/guest/restore` with `{}`
- **THEN** the response is `400` with content type `application/problem+json`
