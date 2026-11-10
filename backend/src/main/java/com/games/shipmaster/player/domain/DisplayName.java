package com.games.shipmaster.player.domain;

import java.util.regex.Pattern;

import com.games.shipmaster.common.domain.InvalidInputException;

/** Player-chosen name: 3-16 Unicode letters, digits, spaces, '_' or '-', trimmed. Not unique. */
public record DisplayName(String value) {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 16;
    private static final Pattern ALLOWED =
            Pattern.compile("^[\\p{L}\\p{Nd} _-]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$");

    public DisplayName {
        if (value == null) {
            throw new InvalidInputException("Display name is required");
        }
        value = value.strip();
        // Length counts code points so names in any script get the same limit.
        int length = value.codePointCount(0, value.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH || !ALLOWED.matcher(value).matches()) {
            throw new InvalidInputException("Display name must be " + MIN_LENGTH + "-" + MAX_LENGTH
                    + " letters, digits, spaces, '_' or '-'");
        }
    }
}
