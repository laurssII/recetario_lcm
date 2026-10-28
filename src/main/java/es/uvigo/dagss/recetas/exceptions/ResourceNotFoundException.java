package es.uvigo.dagss.recetas.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Long id) {
        return new ResourceNotFoundException(entity + " with id " + id + " not found");
    }
}
