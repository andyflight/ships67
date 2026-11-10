package com.games.shipmaster.player.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.games.shipmaster.common.domain.InvalidInputException;

class DisplayNameTest {

    @ParameterizedTest
    @ValueSource(strings = { "abc", "Captain Hook", "sixteen_chars-16", "Капітан", "Sailor4821", "海賊王さん" })
    void acceptsValidNames(String value) {
        assertThat(new DisplayName(value).value()).isEqualTo(value);
    }

    @Test
    void trimsSurroundingSpaces() {
        assertThat(new DisplayName("  Captain Hook  ").value()).isEqualTo("Captain Hook");
    }

    @ParameterizedTest
    @ValueSource(strings = { "x", "ab", "  ab  ", "seventeen_chars17", "bad!name", "no.dots", "emoji🙂ok", "tab\tname" })
    void rejectsInvalidNames(String value) {
        assertThatThrownBy(() -> new DisplayName(value)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new DisplayName(null)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void boundaryLengths() {
        assertThat(new DisplayName("a".repeat(3)).value()).hasSize(3);
        assertThat(new DisplayName("a".repeat(16)).value()).hasSize(16);
        assertThatThrownBy(() -> new DisplayName("a".repeat(2))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new DisplayName("a".repeat(17))).isInstanceOf(InvalidInputException.class);
    }
}
