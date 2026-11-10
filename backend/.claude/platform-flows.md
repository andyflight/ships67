# Platform flows (reminder, not a spec)

Agreed direction for auth, purchases, ads, refunds and progress. Endpoints, tables and statuses are NOT decided yet;
they go into OpenSpec changes (`/opsx:propose`) per capability: player-auth, player-account, game-config, economy,
player-progress, store-purchases, ad-rewards.

The client holds no authoritative state. It starts actions and shows results; the backend never treats what the
client says as proof.

## Auth

1. On start, if the client has a refresh token (iOS Keychain / Android Keystore), it exchanges it for a session.
2. Otherwise it signs in silently with the platform: Game Center on iOS, Play Games Services v2 on Android.
   - Game Center: client sends the identity verification signature (publicKeyURL, signature, salt, timestamp,
     teamPlayerID). Backend downloads the cert (apple.com hosts only), verifies the signature over
     teamPlayerID + bundleId + timestamp + salt, rejects stale timestamps.
   - Play Games v2: client sends a one-time server auth code. Backend exchanges it with Google OAuth and reads the
     stable playerId from the Games API.
   - Optional Sign in with Apple: verify the identity token JWT against Apple JWKS (aud = bundle id).
3. If platform sign-in is refused or unavailable, the backend creates a guest account; the UI nudges linking.
4. After any login the backend issues its own short-lived access JWT (~15 min) and an opaque, rotating refresh token
   (stored hashed; reuse of an old token revokes the whole family).
5. One player has many auth identities (provider + subject, unique). Linking a platform id that already belongs to
   another player is a conflict: the player explicitly chooses which account to keep.
6. Store rules: in-app account deletion is required (Apple 5.1.1(v)), plus a web deletion URL for Google Play.
   Adding Google Sign-In on iOS would require Sign in with Apple too (Apple 4.8).
7. Later: device attestation (App Attest / Play Integrity) on login and purchases.

## Rewarded ad

1. Player taps "watch ad for reward". Client asks the backend for permission for that placement.
2. Backend checks limits (e.g. daily cap), creates an ad intent bound to the player, returns its id.
3. Client shows the ad via the ad SDK, passing the intent id as custom data.
4. After the full view, the ad network calls the backend callback server-to-server with the intent id and a signature.
5. Backend verifies the signature, checks the intent exists, belongs to the player and is unused, grants the reward
   from its own reward table, marks the intent used.
6. Client shows "reward coming..." and polls the intent status (or picks it up on the next state refresh).
Ad closed early -> no callback -> intent expires. Retried callbacks -> "used" flag prevents double grant.
Interstitials/banners: no reward, no callback; client follows server config (frequency caps, "no ads" entitlement).

## Purchase: client path (fast)

1. Client starts the purchase, attaching the player id: appAccountToken (Apple), obfuscatedAccountId (Google).
2. Store takes payment and returns a transaction to the client. Client does NOT finish/consume it yet.
3. Client sends the transaction to the backend.
4. Backend verifies it with the store (App Store Server API / Play Developer API): real, paid (not pending),
   right app and product, right player.
5. Backend records the transaction id (unique). Already recorded -> return current state, grant nothing.
6. New -> grant what the product gives according to the SERVER catalog (client never sends amounts), write ledger
   entries, return new state.
7. Only then the client finishes (Apple) / consumes or acknowledges (Google) the transaction.
Crash between 2 and 7 -> store redelivers the unfinished transaction on next launch -> step 3 again, step 5 dedupes.
Google auto-refunds purchases not acknowledged within 3 days.

## Purchase: webhook path (safety net)

1. Store sends a notification: App Store Server Notifications V2 (signed) / Google RTDN via Pub/Sub
   (only purchase token + product id).
2. Backend verifies it (Apple signature / Pub/Sub token, then Play API call).
3. Finds the player via appAccountToken / obfuscatedAccountId.
4. Runs the same idempotent grant keyed by the same transaction id.
Whichever path arrives first grants; the other is a no-op.

## Refund

1. Store sends a refund notification (Apple REFUND / Google voided purchase) to the webhook.
2. Backend verifies it, finds the original transaction.
3. Applies the refund policy: negative ledger entry (balance may go below zero), flag repeat offenders.
The client is never involved.

## Progress

- Action-based endpoints only; server validates and applies.
- Idempotency-Key header on every mutating request; per-player state version (optimistic lock).
- Server time for all timers; minimum app version check (force update).
- Content catalog and balance served by the server; client caches it for display only.
- Open question: if gameplay (battles/levels) yields currency, add server-issued session tokens, server reward
  tables, caps and plausibility checks.

## Trust summary

- Ad rewards: only the ad network's signed callback.
- Purchase grants: only the backend's own verification with Apple/Google (triggered by client or webhook).
- Refunds: only store notifications.
- Webhook endpoints are public, outside player JWT auth, each with its own verification.

## Future multiplayer (prepare now, build later)

- Currency as an append-only ledger (gifts/trades = paired entries), not a bare balance column.
- Public player tag + display name separate from auth ids and UUIDs.
- Ship loadout as structured data (enables async friend battles over REST).
- Real-time PvP needs separate infra; REST backend would only issue match tickets.
