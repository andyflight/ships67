package com.games.shipmaster.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.games.shipmaster.auth.domain.AuthIdentity;
import com.games.shipmaster.auth.domain.AuthIdentityRepository;

public final class InMemoryAuthIdentities implements AuthIdentityRepository {

    public final List<AuthIdentity> all = new ArrayList<>();

    @Override
    public <S extends AuthIdentity> S save(S identity) {
        if (!all.contains(identity)) {
            all.add(identity);
        }
        return identity;
    }

    @Override
    public Optional<AuthIdentity> findByCredentialHash(String credentialHash) {
        return all.stream().filter(i -> Objects.equals(i.getCredentialHash(), credentialHash)).findFirst();
    }

    @Override
    public List<AuthIdentity> findByPlayerId(UUID playerId) {
        return all.stream().filter(i -> Objects.equals(i.getPlayerId(), playerId)).toList();
    }
}
