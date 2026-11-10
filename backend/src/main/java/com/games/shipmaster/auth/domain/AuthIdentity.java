package com.games.shipmaster.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One way a player signs in: (provider, subject), unique across players. GUEST identities also hold the hash of
 * the guest credential that restores the account.
 */
@Entity
@Table(name = "auth_identity")
public class AuthIdentity {

    @Id
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private AuthProvider provider;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "credential_hash", length = 64, unique = true)
    private String credentialHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected AuthIdentity() {
        // for JPA
    }

    private AuthIdentity(UUID id, UUID playerId, AuthProvider provider, String subject, String credentialHash,
            Instant now) {
        this.id = id;
        this.playerId = playerId;
        this.provider = provider;
        this.subject = subject;
        this.credentialHash = credentialHash;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    /** Guest subject is a server-generated random id, never a client-supplied device id. */
    public static AuthIdentity guest(UUID playerId, String credentialHash, Instant now) {
        return new AuthIdentity(UUID.randomUUID(), playerId, AuthProvider.GUEST, UUID.randomUUID().toString(),
                credentialHash, now);
    }

    public void markUsed(Instant now) {
        this.lastUsedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getSubject() {
        return subject;
    }

    public String getCredentialHash() {
        return credentialHash;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}
