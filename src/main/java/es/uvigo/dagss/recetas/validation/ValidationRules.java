package es.uvigo.dagss.recetas.validation;

/**
 * Single place where the input rules of the application live: regular
 * expressions, limits and the (Spanish) messages shown to the user.
 *
 * <p>The full, human-readable description of every rule is in
 * {@code doc/validaciones.md}; the demo client ({@code cliente-demo.html})
 * mirrors these same rules so that errors are caught before sending the form.
 *
 * <p><b>Convention:</b> every regular expression accepts the empty string
 * (note the leading {@code |}). That way an empty field is reported only once,
 * by {@code @NotBlank} ("es obligatorio"), instead of also being reported as
 * "formato incorrecto" by {@code @Pattern}.
 */
public final class ValidationRules {

    private ValidationRules() {
    }

    /** Spanish phone number: 9 digits, starting with 6, 7, 8 or 9. */
    public static final String PHONE_REGEX = "|[6-9]\\d{8}";

    /** Spanish postal code: 5 digits whose first two (01-52) are the province code. */
    public static final String POSTAL_CODE_REGEX = "|(0[1-9]|[1-4]\\d|5[0-2])\\d{3}";

    /** Person name / surname: starts with a letter; letters, spaces, hyphens, dots and apostrophes; 2-60 chars. */
    public static final String PERSON_NAME_REGEX = "|\\p{L}[\\p{L} .'’-]{1,59}";

    /** City: like a person name, plus "/" (e.g. "Alicante/Alacant"). */
    public static final String CITY_REGEX = "|\\p{L}[\\p{L} .'’/-]{1,59}";

    /** Street address: 3-120 characters. */
    public static final String STREET_REGEX = "|.{3,120}";

    /** E-mail address (max. 100 chars) with a domain that has a TLD of 2+ letters. */
    public static final String EMAIL_REGEX =
            "|(?=.{1,100}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}";

    /** Login name: 3-30 characters among letters, digits, dot, hyphen and underscore. */
    public static final String USERNAME_REGEX = "|[A-Za-z0-9._-]{3,30}";

    /**
     * Health card number (tarjeta sanitaria): 4 uppercase letters followed by
     * 10 digits (14 characters, modelled on the regional "CIP" code).
     */
    public static final String HEALTH_CARD_REGEX = "|[A-Z]{4}\\d{10}";

    /** Social Security number: 12 digits (check digits are not verified). */
    public static final String SOCIAL_SECURITY_REGEX = "|\\d{12}";

    /** Professional license number (nº de colegiado): 4-15 uppercase letters, digits or hyphens. */
    public static final String LICENSE_NUMBER_REGEX = "|[A-Z0-9][A-Z0-9-]{3,14}";

    /** Password: 4-64 characters. */
    public static final String PASSWORD_REGEX = "|.{4,64}";

    /** Length limits of a password (used by the checks that are not annotation based). */
    public static final int PASSWORD_MIN_LENGTH = 4;
    public static final int PASSWORD_MAX_LENGTH = 64;

    public static final String NAME_REQUIRED = "El nombre es obligatorio";
    public static final String NAME_INVALID =
            "El nombre solo puede contener letras, espacios, guiones y apóstrofos (2-60 caracteres)";

    public static final String LAST_NAME_REQUIRED = "Los apellidos son obligatorios";
    public static final String LAST_NAME_INVALID =
            "Los apellidos solo pueden contener letras, espacios, guiones y apóstrofos (2-60 caracteres)";

    public static final String NATIONAL_ID_REQUIRED = "El DNI/NIE es obligatorio";
    public static final String NATIONAL_ID_INVALID =
            "El DNI/NIE no es válido (DNI: 8 dígitos y letra correcta; NIE: X, Y o Z, 7 dígitos y letra correcta)";

    public static final String TAX_ID_REQUIRED = "El DNI/NIF es obligatorio";
    public static final String TAX_ID_INVALID =
            "El DNI/NIF no es válido (DNI o NIE con letra correcta, o CIF con formato correcto)";

    public static final String HEALTH_CARD_REQUIRED = "El número de tarjeta sanitaria es obligatorio";
    public static final String HEALTH_CARD_INVALID =
            "La tarjeta sanitaria debe tener 4 letras mayúsculas seguidas de 10 dígitos (ej.: ABCD1234567890)";

    public static final String SOCIAL_SECURITY_REQUIRED = "El número de la Seguridad Social es obligatorio";
    public static final String SOCIAL_SECURITY_INVALID =
            "El número de la Seguridad Social debe tener 12 dígitos";

    public static final String LICENSE_REQUIRED = "El número de colegiado es obligatorio";
    public static final String LICENSE_INVALID =
            "El número de colegiado debe tener entre 4 y 15 caracteres: letras mayúsculas, dígitos o guiones";

    public static final String PHONE_REQUIRED = "El teléfono es obligatorio";
    public static final String PHONE_INVALID = "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9";

    public static final String EMAIL_REQUIRED = "El email es obligatorio";
    public static final String EMAIL_INVALID =
            "El email no es válido (ej.: nombre@dominio.es; máximo 100 caracteres)";

    public static final String USERNAME_REQUIRED = "El usuario (login) es obligatorio";
    public static final String USERNAME_INVALID =
            "El usuario debe tener entre 3 y 30 caracteres: letras, dígitos, punto, guion o guion bajo";

    public static final String STREET_REQUIRED = "El domicilio es obligatorio";
    public static final String STREET_INVALID = "El domicilio debe tener entre 3 y 120 caracteres";

    public static final String CITY_REQUIRED = "La localidad es obligatoria";
    public static final String CITY_INVALID =
            "La localidad solo puede contener letras, espacios, guiones, apóstrofos y barras (2-60 caracteres)";

    public static final String POSTAL_CODE_REQUIRED = "El código postal es obligatorio";
    public static final String POSTAL_CODE_INVALID =
            "El código postal debe tener 5 dígitos y empezar por un código de provincia válido (01-52)";

    public static final String PROVINCE_REQUIRED = "La provincia es obligatoria";
    public static final String PROVINCE_INVALID = "La provincia no es una provincia española válida";

    public static final String BIRTH_DATE_REQUIRED = "La fecha de nacimiento es obligatoria";
    public static final String BIRTH_DATE_INVALID =
            "La fecha de nacimiento debe estar entre el 01/01/1900 y hoy";

    public static final String PASSWORD_REQUIRED = "La contraseña es obligatoria";
    public static final String PASSWORD_INVALID = "La contraseña debe tener entre 4 y 64 caracteres";
}
