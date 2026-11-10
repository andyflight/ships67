package com.games.shipmaster.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stored refresh token (hash only). Tokens rotated from one sign-in share a family; reuse revokes the family.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken() {
        // for JPA
    }

    private RefreshToken(UUID playerId, UUID familyId, String tokenHash, Instant now, Duration ttl) {
        this.id = UUID.randomUUID();
        this.playerId = playerId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = now.plus(ttl);
    }

    public static RefreshToken issue(UUID playerId, UUID familyId, String tokenHash, Instant now, Duration ttl) {
        return new RefreshToken(playerId, familyId, tokenHash, now, ttl);
    }

    /**
     * Decides what presenting this token means.
     *
     * @param successor the token this one was rotated into, or null
     */
    public RefreshDecision decide(Instant now, Duration grace, RefreshToken successor) {
        if (isRevoked() || !now.isBefore(expiresAt)) {
            return RefreshDecision.INVALID;
        }
        if (usedAt == null) {
            return RefreshDecision.ROTATE;
        }
        boolean withinGrace = now.isBefore(usedAt.plus(grace));
        boolean successorUntouched = successor != null && successor.usedAt == null && !successor.isRevoked();
        return withinGrace && successorUntouched ? RefreshDecision.RETRY_IN_GRACE : RefreshDecision.REUSE_DETECTED;
    }

    /** Records rotation into {@code successorId}; the first use time anchors the grace window. */
    public void rotatedTo(UUID successorId, Instant now) {
        if (usedAt == null) {
            usedAt = now;
        }
        replacedBy = successorId;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
