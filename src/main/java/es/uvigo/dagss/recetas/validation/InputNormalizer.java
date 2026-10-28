package es.uvigo.dagss.recetas.validation;

import java.util.Locale;

/**
 * Cleans text coming from the clients before it is validated and stored:
 * surrounding whitespace is removed and, for codes that are compared
 * literally (DNI, license number, health card number...), the text is
 * upper-cased. This matters because the initial password of a new user is
 * their DNI / license number, and the login compares passwords literally.
 */
public final class InputNormalizer {

    private InputNormalizer() {
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
