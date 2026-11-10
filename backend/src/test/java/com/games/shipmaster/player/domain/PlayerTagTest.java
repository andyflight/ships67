package com.games.shipmaster.player.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.games.shipmaster.common.domain.InvalidInputException;
import com.games.shipmaster.player.infrastructure.SecureRandomPlayerTagGenerator;

class PlayerTagTest {

    @ParameterizedTest
    @ValueSource(strings = { "#8QK2LP9V", "#22222222", "#ZZZZZZZZ" })
    void acceptsWellFormedTags(String value) {
        assertThat(new PlayerTag(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = { "8QK2LP9V", "#8QK2LP9", "#8QK2LP9VX", "#8QK2LP9O", "#8QK2LP90", "#8QK2LP91", "#8QK2LP9I",
            "#8qk2lp9v", "" })
    void rejectsMalformedTags(String value) {
        assertThatThrownBy(() -> new PlayerTag(value)).isInstanceOf(InvalidInputException.class);
    }

    @RepeatedTest(50)
    void generatorProducesValidTags() {
        assertThat(new SecureRandomPlayerTagGenerator().next().value())
                .matches("^#[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}$");
    }
}
