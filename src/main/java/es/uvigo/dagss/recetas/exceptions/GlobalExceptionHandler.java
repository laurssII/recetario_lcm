package es.uvigo.dagss.recetas.exceptions;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates exceptions and Bean Validation errors into consistent JSON responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailed(AuthenticationFailedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        ResponseEntity<Map<String, Object>> response = build(HttpStatus.CONFLICT, ex.getMessage());
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getField(), ex.getMessage());
        response.getBody().put("fieldErrors", fieldErrors);
        return response;
    }

    /**
     * Last line of defence for the unique constraints of the database, in case two
     * requests with the same value pass the (earlier) check at the same time.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT,
                "Los datos entran en conflicto con otro registro existente (valor duplicado) o están incompletos");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOperation(InvalidOperationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Bean Validation errors. The "message" is the list of (self-explanatory,
     * Spanish) messages; "fieldErrors" maps each invalid field (e.g.
     * "phone", "address.postalCode") to its first message, so that a client
     * can highlight the offending inputs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        Set<String> messages = new LinkedHashSet<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> {
            fieldErrors.putIfAbsent(err.getField(), err.getDefaultMessage());
            messages.add(err.getDefaultMessage());
        });
        ex.getBindingResult().getGlobalErrors().forEach(err -> messages.add(err.getDefaultMessage()));

        ResponseEntity<Map<String, Object>> response =
                build(HttpStatus.BAD_REQUEST, messages.isEmpty() ? "Datos de entrada no válidos" : String.join("; ", messages));
        response.getBody().put("fieldErrors", fieldErrors);
        return response;
    }

    /** Malformed JSON, or a value of the wrong type (e.g. a date that is not in YYYY-MM-DD format). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Los datos enviados no tienen un formato válido (JSON incorrecto o valor de tipo erróneo; "
                        + "las fechas deben tener el formato AAAA-MM-DD)");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
