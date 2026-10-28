package es.uvigo.dagss.recetas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Schema(description = "Nombre de usuario (no distingue mayúsculas)", example = "lfernandez") String username,
        @NotBlank(message = "La contraseña es obligatoria")
        @Schema(example = "LIC-4501") String password) {
}
