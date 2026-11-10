package com.games.shipmaster.auth.application;

/** Result of guest sign-in: the session plus the guest credential, which is shown only this once. */
public record GuestSession(Session session, String guestCredential) {
}
