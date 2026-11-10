package com.games.shipmaster.auth.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.games.shipmaster.auth.domain.RefreshToken;
import com.games.shipmaster.auth.domain.RefreshTokenRepository;

import jakarta.persistence.LockModeType;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID>, RefreshTokenRepository {

    @Override
    <S extends RefreshToken> S save(S token);

    @Override
    Optional<RefreshToken> findById(UUID id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("hash") String tokenHash);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :now where r.familyId = :familyId and r.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}
