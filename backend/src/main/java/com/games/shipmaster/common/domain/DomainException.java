package com.games.shipmaster.common.domain;

/**
 * Base for business errors. The API layer maps each subclass to an HTTP status and a problem type slug.
 */
public abstract class DomainException extends RuntimeException {

    private final String problemSlug;

    protected DomainException(String problemSlug, String message) {
        super(message);
        this.problemSlug = problemSlug;
    }

    public String problemSlug() {
        return problemSlug;
    }
}
