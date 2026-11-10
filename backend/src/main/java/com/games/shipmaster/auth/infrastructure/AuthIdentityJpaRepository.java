package com.games.shipmaster.auth.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.games.shipmaster.auth.domain.AuthIdentity;
import com.games.shipmaster.auth.domain.AuthIdentityRepository;

public interface AuthIdentityJpaRepository extends JpaRepository<AuthIdentity, UUID>, AuthIdentityRepository {

    @Override
    <S extends AuthIdentity> S save(S identity);
}
