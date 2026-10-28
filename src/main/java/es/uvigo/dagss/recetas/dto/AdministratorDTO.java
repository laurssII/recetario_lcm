package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import java.time.LocalDateTime;

import es.uvigo.dagss.recetas.entities.Administrator;
import es.uvigo.dagss.recetas.validation.InputNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdministratorDTO(
        Long id,
        @NotBlank(message = NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String name,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email,
        @NotBlank(message = USERNAME_REQUIRED)
        @Pattern(regexp = USERNAME_REGEX, message = USERNAME_INVALID) String username,
        boolean active,
        LocalDateTime registrationDate,
        LocalDateTime lastAccess) {

    public AdministratorDTO {
        name = InputNormalizer.trim(name);
        email = InputNormalizer.trim(email);
        username = InputNormalizer.trim(username);
    }

    public static AdministratorDTO from(Administrator a) {
        return new AdministratorDTO(a.getId(), a.getName(), a.getEmail(), a.getUsername(),
                Boolean.TRUE.equals(a.getActive()), a.getRegistrationDate(), a.getLastAccess());
    }
}
