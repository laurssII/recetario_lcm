package es.uvigo.dagss.recetas.exceptions;

/**
 * Thrown when a value that must be unique already belongs to another record. getField() is the name of the
 * offending field in the request body, so that a client can highlight it.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
