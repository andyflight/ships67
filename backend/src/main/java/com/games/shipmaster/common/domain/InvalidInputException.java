package com.games.shipmaster.common.domain;

public class InvalidInputException extends DomainException {

    public InvalidInputException(String message) {
        super("invalid-input", message);
    }
}
