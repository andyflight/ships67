package com.games.shipmaster.auth.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.games.shipmaster.auth.domain.RefreshToken;
import com.games.shipmaster.auth.domain.RefreshTokenRepository;
import com.games.shipmaster.auth.domain.SecretCodec;
import com.games.shipmaster.common.domain.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshSession {

    private final RefreshTokenRepository refreshTokens;
    private final SecretCodec codec;
    private final SessionIssuer sessions;
    private final SessionPolicy policy;
    private final Clock clock;

    // The family revocation on reuse must commit even though the call ends in 401.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Session execute(String refreshTokenValue) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByTokenHashForUpdate(codec.hash(refreshTokenValue))
                .orElseThrow(RefreshSession::invalid);
        RefreshToken successor = token.getReplacedBy() == null
                ? null
                : refreshTokens.findById(token.getReplacedBy()).orElse(null);

        return switch (token.decide(now, policy.refreshGrace(), successor)) {
            case ROTATE -> rotate(token, now);
            case RETRY_IN_GRACE -> {
                successor.revoke(now);
                refreshTokens.save(successor);
                yield rotate(token, now);
            }
            case REUSE_DETECTED -> {
                refreshTokens.revokeFamily(token.getFamilyId(), now);
                throw invalid();
            }
            case INVALID -> throw invalid();
        };
    }

    private Session rotate(RefreshToken token, Instant now) {
        SessionIssuer.Issued issued = sessions.issueInFamily(token.getPlayerId(), token.getFamilyId(), now);
        token.rotatedTo(issued.refreshTokenId(), now);
        refreshTokens.save(token);
        return issued.session();
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException("Invalid refresh token");
    }
}
