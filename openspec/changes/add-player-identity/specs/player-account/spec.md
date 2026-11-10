# Spec Delta

## Purpose

Defines the player record the client can see and change: internal id, public tag, display name and which
sign-in providers are linked, separate from auth secrets and future game progress.

## ADDED Requirements

### Requirement: Player record
Every player SHALL have a UUID id, a creation time (UTC), a public tag and a display name. The id SHALL never
change; the tag SHALL be unique and never change.

#### Scenario: New player defaults
- **WHEN** a player is created by `POST /api/v1/auth/guest`
- **THEN** `GET /api/v1/players/me` returns a `tag` and a non-empty default `displayName`

### Requirement: Public player tag
The tag SHALL be `#` followed by 8 characters from the alphabet `23456789ABCDEFGHJKLMNPQRSTUVWXYZ` (no `0 O 1 I` that are easy to
confuse), generated randomly and unique across players. It is the id players share with each other; the UUID is
not shown to other players.

#### Scenario: Tag format
- **WHEN** a player is created
- **THEN** their tag matches `^#[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}$`

### Requirement: Read own profile
The system SHALL return the authenticated player's profile as JSON: `id` (UUID string), `tag` (string),
`displayName` (string), `createdAt` (ISO-8601 UTC), `providers` (array of linked provider names). The same body is
returned by every endpoint that changes the profile.

#### Scenario: Get profile
- **WHEN** the client calls `GET /api/v1/players/me` with a valid access token
- **THEN** the response is `200` with `{ "id": "3f1c…", "tag": "#8QK2LP9V", "displayName": "Sailor4821", "createdAt": "2026-11-10T12:00:00Z", "providers": ["GUEST"] }`

#### Scenario: Profile of a player that no longer exists
- **WHEN** the access token is valid but its player was removed
- **THEN** the response is `401` with content type `application/problem+json`

### Requirement: Rename
The player SHALL be able to change their display name to 3-16 characters of Unicode letters, digits, spaces, `_` or `-`,
with leading and trailing spaces trimmed. Names are not unique.

#### Scenario: Valid rename
- **WHEN** the client calls `PATCH /api/v1/players/me` with `{ "displayName": "Captain Hook" }`
- **THEN** the response is `200` with the updated profile showing `displayName` `Captain Hook`

#### Scenario: Invalid name
- **WHEN** the client calls `PATCH /api/v1/players/me` with `{ "displayName": "x" }` or a name with characters outside the allowed set
- **THEN** the response is `400` with content type `application/problem+json` and the name is unchanged
