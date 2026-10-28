package es.uvigo.dagss.recetas.dto;

import java.time.Instant;

import es.uvigo.dagss.recetas.entities.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        @Schema(description = "Id del usuario, dentro de su tipo (paciente, médico...)") Long id,
        UserType role,
        String name,
        String lastName,
        @Schema(description = "Token JWT; se envía como `Authorization: Bearer <token>`") String token,
        @Schema(description = "Instante en que el token deja de ser válido") Instant expiresAt) {
}
