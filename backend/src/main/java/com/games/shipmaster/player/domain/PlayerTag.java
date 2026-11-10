package com.games.shipmaster.player.domain;

import java.util.regex.Pattern;

import com.games.shipmaster.common.domain.InvalidInputException;

/** Public id players share with each other, e.g. {@code #8QK2LP9V}. Never changes. */
public record PlayerTag(String value) {

    /** No 0/O/1/I: tags are read aloud and typed by hand. */
    public static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    public static final int LENGTH = 8;
    public static final String PREFIX = "#";
    private static final Pattern FORMAT = Pattern.compile("^#[" + ALPHABET + "]{" + LENGTH + "}$");

    public PlayerTag {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidInputException("Invalid player tag: " + value);
        }
    }
}
