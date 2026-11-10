package com.games.shipmaster.auth.domain;

/** Outcome of presenting a refresh token (design D4). */
public enum RefreshDecision {
    /** Expired or revoked: reject without side effects. */
    INVALID,
    /** First use: rotate to a new token. */
    ROTATE,
    /** Retried within the grace window and the successor was never used: replace the successor. */
    RETRY_IN_GRACE,
    /** Replayed after rotation: treat as theft and revoke the family. */
    REUSE_DETECTED
}
