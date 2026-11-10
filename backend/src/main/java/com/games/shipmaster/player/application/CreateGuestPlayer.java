package com.games.shipmaster.player.application;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.games.shipmaster.player.domain.DisplayName;
import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerRepository;
import com.games.shipmaster.player.domain.PlayerTag;
import com.games.shipmaster.player.domain.PlayerTagGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateGuestPlayer {

    static final int MAX_TAG_ATTEMPTS = 5;
    static final String DEFAULT_NAME_PREFIX = "Sailor";

    private final PlayerRepository players;
    private final PlayerTagGenerator tags;
    private final Clock clock;

    /** Creates a player with a unique tag and a default name; returns its id. */
    @Transactional
    public UUID execute() {
        Player player = Player.create(UUID.randomUUID(), uniqueTag(), defaultName(), clock.instant());
        return players.save(player).getId();
    }

    private PlayerTag uniqueTag() {
        for (int attempt = 0; attempt < MAX_TAG_ATTEMPTS; attempt++) {
            PlayerTag tag = tags.next();
            if (!players.existsByTag(tag.value())) {
                return tag;
            }
        }
        // ~1e12 tag space: reaching this means the generator is broken, not that tags ran out.
        throw new IllegalStateException("Could not generate a unique player tag in " + MAX_TAG_ATTEMPTS + " attempts");
    }

    private static DisplayName defaultName() {
        return new DisplayName(DEFAULT_NAME_PREFIX + ThreadLocalRandom.current().nextInt(1000, 10000));
    }
}
