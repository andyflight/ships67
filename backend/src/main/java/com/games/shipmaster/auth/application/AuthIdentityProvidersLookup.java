package com.games.shipmaster.auth.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.games.shipmaster.auth.domain.AuthIdentityRepository;
import com.games.shipmaster.player.application.PlayerProvidersLookup;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthIdentityProvidersLookup implements PlayerProvidersLookup {

    private final AuthIdentityRepository identities;

    @Override
    public List<String> providersOf(UUID playerId) {
        return identities.findByPlayerId(playerId).stream()
                .map(i -> i.getProvider().name())
                .distinct()
                .sorted()
                .toList();
    }
}
