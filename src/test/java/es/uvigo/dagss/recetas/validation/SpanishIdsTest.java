package es.uvigo.dagss.recetas.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpanishIdsTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345678Z", "00000000T", "10000001S", "20000001Y", "12345678z", " 12345678Z "})
    void shouldAcceptDniWithCorrectControlLetter(String dni) {
        assertThat(SpanishIds.isValidDni(dni)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678A", "1234567Z", "123456789Z", "ABCDEFGHZ", "", "12345678"})
    void shouldRejectDniWithWrongControlLetterOrFormat(String dni) {
        assertThat(SpanishIds.isValidDni(dni)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"X1234567L", "x1234567l", "Y0000000Z", "Z0000000M"})
    void shouldAcceptNieWithCorrectControlLetter(String nie) {
        assertThat(SpanishIds.isValidNie(nie)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"X1234567A", "W1234567L", "X123456L", "12345678Z"})
    void shouldRejectNieWithWrongControlLetterOrFormat(String nie) {
        assertThat(SpanishIds.isValidNie(nie)).isFalse();
    }

    @Test
    void shouldAcceptDniNieOrCifAsTaxId() {
        assertThat(SpanishIds.isValidTaxId("12345678Z")).isTrue();
        assertThat(SpanishIds.isValidTaxId("X1234567L")).isTrue();
        assertThat(SpanishIds.isValidTaxId("B12345674")).isTrue();
        assertThat(SpanishIds.isValidTaxId("TAX-9988")).isFalse();
        assertThat(SpanishIds.isValidTaxId(null)).isFalse();
    }
}
