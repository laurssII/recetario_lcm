package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordChangeRequest(
        @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_INVALID)
        @Schema(description = "De 4 a 64 caracteres y como máximo 72 bytes en UTF-8") String newPassword) {
}
