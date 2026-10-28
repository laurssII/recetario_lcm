package es.uvigo.dagss.recetas.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpanishProvincesTest {

    @Test
    void shouldHaveTheFiftyTwoProvincesWithConsecutiveCodes() {
        assertThat(SpanishProvinces.all()).hasSize(52);
        for (int i = 0; i < 52; i++) {
            assertThat(SpanishProvinces.all().get(i).code()).isEqualTo(String.format("%02d", i + 1));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pontevedra", "pontevedra", "  PONTEVEDRA ", "A Coruña", "a coruna", "La Coruña", "Coruña, A",
            "Orense", "Gerona", "Lérida", "Vizcaya", "Baleares", "Valencia/València", "Alacant"})
    void shouldRecognizeProvincesAndTheirAlternativeSpellings(String name) {
        assertThat(SpanishProvinces.find(name)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Atlantis", "Vigo", "Galicia", "Pontevedraa", "123", ""})
    void shouldRejectNamesThatAreNotProvinces(String name) {
        assertThat(SpanishProvinces.find(name)).isEmpty();
    }

    @Test
    void shouldMapNamesToTheirCodeAndCanonicalName() {
        assertThat(SpanishProvinces.find("la coruña").orElseThrow().code()).isEqualTo("15");
        assertThat(SpanishProvinces.canonicalNameOrSame("la coruña")).isEqualTo("A Coruña");
        assertThat(SpanishProvinces.canonicalNameOrSame("Atlantis")).isEqualTo("Atlantis");
        assertThat(SpanishProvinces.findByCode("36").orElseThrow().name()).isEqualTo("Pontevedra");
        assertThat(SpanishProvinces.findByCode("99")).isEmpty();
    }
}
