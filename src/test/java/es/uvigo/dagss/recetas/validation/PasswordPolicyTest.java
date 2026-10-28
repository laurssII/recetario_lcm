package es.uvigo.dagss.recetas.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;

class PasswordPolicyTest {

    @Test
    void acceptsBetween4And64Characters() {
        assertThat(PasswordPolicy.isValid("abcd")).isTrue();
        assertThat(PasswordPolicy.isValid("x".repeat(64))).isTrue();
    }

    @Test
    void rejectsTooShortTooLongOrMissing() {
        assertThat(PasswordPolicy.isValid("abc")).isFalse();
        assertThat(PasswordPolicy.isValid("x".repeat(65))).isFalse();
        assertThat(PasswordPolicy.isValid(null)).isFalse();
    }

    @Test
    void rejectsPasswordsBcryptWouldSilentlyTruncate() {
        assertThat(PasswordPolicy.isValid("é".repeat(36))).isTrue();    // 72 bytes in UTF-8
        assertThat(PasswordPolicy.isValid("é".repeat(37))).isFalse();   // 74 bytes
    }

    @Test
    void checkThrowsAnInvalidOperation() {
        assertThatThrownBy(() -> PasswordPolicy.check("ab")).isInstanceOf(InvalidOperationException.class);
    }
}
