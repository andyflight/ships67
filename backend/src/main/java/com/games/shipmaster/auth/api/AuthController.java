package com.games.shipmaster.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.games.shipmaster.auth.api.dto.GuestSessionResponse;
import com.games.shipmaster.auth.api.dto.RefreshTokenRequest;
import com.games.shipmaster.auth.api.dto.RestoreGuestRequest;
import com.games.shipmaster.auth.api.dto.SessionResponse;
import com.games.shipmaster.auth.application.Logout;
import com.games.shipmaster.auth.application.RefreshSession;
import com.games.shipmaster.auth.application.RestoreGuestSession;
import com.games.shipmaster.auth.application.SignInAsGuest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignInAsGuest signInAsGuest;
    private final RestoreGuestSession restoreGuestSession;
    private final RefreshSession refreshSession;
    private final Logout logout;

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestSessionResponse guest() {
        return GuestSessionResponse.from(signInAsGuest.execute());
    }

    @PostMapping("/guest/restore")
    public SessionResponse restore(@Valid @RequestBody RestoreGuestRequest request) {
        return SessionResponse.from(restoreGuestSession.execute(request.guestCredential()));
    }

    @PostMapping("/refresh")
    public SessionResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return SessionResponse.from(refreshSession.execute(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.execute(request.refreshToken());
    }
}
