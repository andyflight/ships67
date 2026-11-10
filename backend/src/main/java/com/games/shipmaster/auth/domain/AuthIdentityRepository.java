package com.games.shipmaster.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository {

    <S extends AuthIdentity> S save(S identity);

    Optional<AuthIdentity> findByCredentialHash(String credentialHash);

    List<AuthIdentity> findByPlayerId(UUID playerId);
}
