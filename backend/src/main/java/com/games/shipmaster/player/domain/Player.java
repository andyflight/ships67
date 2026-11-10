package com.games.shipmaster.player.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** A player account: identity and public profile. Game progress lives in other domains, keyed by id. */
@Entity
@Table(name = "player")
public class Player {

    @Id
    private UUID id;

    @Column(name = "tag", nullable = false, unique = true, length = 9)
    private String tag;

    @Column(name = "display_name", nullable = false, length = 16)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Player() {
        // for JPA
    }

    private Player(UUID id, PlayerTag tag, DisplayName displayName, Instant createdAt) {
        this.id = id;
        this.tag = tag.value();
        this.displayName = displayName.value();
        this.createdAt = createdAt;
    }

    public static Player create(UUID id, PlayerTag tag, DisplayName displayName, Instant now) {
        return new Player(id, tag, displayName, now);
    }

    public void rename(DisplayName newName) {
        this.displayName = newName.value();
    }

    public UUID getId() {
        return id;
    }

    public PlayerTag getTag() {
        return new PlayerTag(tag);
    }

    public DisplayName getDisplayName() {
        return new DisplayName(displayName);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
