package com.games.shipmaster.common.domain;

public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super("not-found", message);
    }
}
