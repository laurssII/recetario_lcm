package es.uvigo.dagss.recetas.validation;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for Spanish identity documents. Pure functions (no framework
 * dependencies), so they are easy to unit test.
 *
 * <ul>
 *   <li><b>DNI</b>: 8 digits + control letter (the number modulo 23 indexes {@link #CONTROL_LETTERS}).</li>
 *   <li><b>NIE</b>: X, Y or Z + 7 digits + control letter (X/Y/Z are replaced by 0/1/2 and the DNI rule applies).</li>
 *   <li><b>CIF</b>: only the format is checked (organization letter + 7 digits + control digit/letter),
 *       not the control character.</li>
 * </ul>
 */
public final class SpanishIds {

    private static final String CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    private static final Pattern DNI = Pattern.compile("(\\d{8})([A-Z])");
    private static final Pattern NIE = Pattern.compile("([XYZ])(\\d{7})([A-Z])");
    private static final Pattern CIF = Pattern.compile("[ABCDEFGHJNPQRSUVW]\\d{7}[0-9A-J]");

    private SpanishIds() {
    }

    public static char dniControlLetter(int number) {
        return CONTROL_LETTERS.charAt(number % 23);
    }

    public static boolean isValidDni(String value) {
        Matcher m = DNI.matcher(clean(value));
        return m.matches() && dniControlLetter(Integer.parseInt(m.group(1))) == m.group(2).charAt(0);
    }

    public static boolean isValidNie(String value) {
        Matcher m = NIE.matcher(clean(value));
        if (!m.matches()) {
            return false;
        }
        int prefix = "XYZ".indexOf(m.group(1).charAt(0));
        int number = Integer.parseInt(prefix + m.group(2));
        return dniControlLetter(number) == m.group(3).charAt(0);
    }

    public static boolean isValidPersonalId(String value) {
        return isValidDni(value) || isValidNie(value);
    }

    public static boolean hasCifFormat(String value) {
        return CIF.matcher(clean(value)).matches();
    }

    public static boolean isValidTaxId(String value) {
        return isValidPersonalId(value) || hasCifFormat(value);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
