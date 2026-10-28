package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import es.uvigo.dagss.recetas.validation.InputNormalizer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Unlike the other users, an administrator's initial password is chosen by whoever creates them
 * and travels in the request body, never in the URL (where it would end up in logs and history).
 */
public record NewAdministratorRequest(
        @NotBlank(message = NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String name,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email,
        @NotBlank(message = USERNAME_REQUIRED)
        @Pattern(regexp = USERNAME_REGEX, message = USERNAME_INVALID) String username,
        @NotBlank(message = PASSWORD_REQUIRED)
        @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_INVALID)
        @Schema(description = "Contraseña inicial elegida por quien crea al administrador") String password) {

    public NewAdministratorRequest {
        name = InputNormalizer.trim(name);
        email = InputNormalizer.trim(email);
        username = InputNormalizer.trim(username);
        // the password is deliberately not trimmed: spaces may be part of it
    }

    public AdministratorDTO toDto() {
        return new AdministratorDTO(null, name, email, username, true, null, null);
    }
}
