package es.uvigo.dagss.recetas.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record NewAppointmentRequest(
        @NotNull(message = "El paciente es obligatorio")
        @Schema(description = "Debe ser el propio paciente que reserva") Long patientId,
        @NotNull(message = "La fecha y hora de la cita son obligatorias")
        @Schema(description = "Inicio del hueco: múltiplo de 15 minutos entre las 08:30 y las 15:15", example = "2030-03-04T09:00:00") LocalDateTime dateTime) {
}
