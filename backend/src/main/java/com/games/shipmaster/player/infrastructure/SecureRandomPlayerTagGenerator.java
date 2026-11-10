package com.games.shipmaster.player.infrastructure;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.games.shipmaster.player.domain.PlayerTag;
import com.games.shipmaster.player.domain.PlayerTagGenerator;

@Component
public class SecureRandomPlayerTagGenerator implements PlayerTagGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public PlayerTag next() {
        StringBuilder tag = new StringBuilder(PlayerTag.PREFIX);
        for (int i = 0; i < PlayerTag.LENGTH; i++) {
            tag.append(PlayerTag.ALPHABET.charAt(random.nextInt(PlayerTag.ALPHABET.length())));
        }
        return new PlayerTag(tag.toString());
    }
}
