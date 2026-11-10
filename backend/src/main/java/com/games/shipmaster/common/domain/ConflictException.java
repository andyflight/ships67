package com.games.shipmaster.common.domain;

public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super("conflict", message);
    }
}
