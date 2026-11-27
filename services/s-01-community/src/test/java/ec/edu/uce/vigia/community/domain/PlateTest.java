package ec.edu.uce.vigia.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlateTest {

    @ParameterizedTest
    @ValueSource(strings = {"PBA-1234", "pba1234", " pba 1234 ", "PBA-1234"})
    void normalizesFourDigitPlates(String text) {
        assertThat(Plate.of(text).value()).isEqualTo("PBA-1234");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GYE-123", "gye123"})
    void acceptsThreeDigitPlates(String text) {
        assertThat(Plate.of(text).value()).isEqualTo("GYE-123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "PB-1234", "PBAA-1234", "PBA-12", "PBA-12345", "123-ABCD"})
    void rejectsOtherFormats(String text) {
        assertThatThrownBy(() -> Plate.of(text))
                .isInstanceOf(DomainException.class)
                .extracting(error -> ((DomainException) error).code())
                .isEqualTo(ErrorCode.INVALID_PLATE);
    }

    @org.junit.jupiter.api.Test
    void rejectsNull() {
        assertThatThrownBy(() -> Plate.of(null)).isInstanceOf(DomainException.class);
    }
}
