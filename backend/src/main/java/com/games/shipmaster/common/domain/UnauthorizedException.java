package com.games.shipmaster.common.domain;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super("unauthorized", message);
    }
}
