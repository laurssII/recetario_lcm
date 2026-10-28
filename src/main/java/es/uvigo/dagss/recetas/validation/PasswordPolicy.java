package es.uvigo.dagss.recetas.validation;

import java.nio.charset.StandardCharsets;

import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;

/**
 * Rules for a password chosen by a person (as opposed to the initial
 * passwords, which are DNI / license numbers already validated elsewhere):
 * 4-64 characters and at most 72 bytes in UTF-8, because BCrypt only looks at
 * the first 72 bytes (accented characters take 2 bytes each).
 */
public final class PasswordPolicy {

    public static final int BCRYPT_MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && password.length() >= ValidationRules.PASSWORD_MIN_LENGTH
                && password.length() <= ValidationRules.PASSWORD_MAX_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    public static void check(String password) {
        if (!isValid(password)) {
            throw new InvalidOperationException("La contraseña debe tener entre " + ValidationRules.PASSWORD_MIN_LENGTH
                    + " y " + ValidationRules.PASSWORD_MAX_LENGTH + " caracteres (y no superar " + BCRYPT_MAX_BYTES
                    + " bytes en UTF-8)");
        }
    }
}
