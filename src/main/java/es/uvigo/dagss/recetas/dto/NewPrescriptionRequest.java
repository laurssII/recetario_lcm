package es.uvigo.dagss.recetas.dto;

import java.time.LocalDate;

import es.uvigo.dagss.recetas.validation.InputNormalizer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NewPrescriptionRequest(
        @NotNull(message = "El paciente es obligatorio") Long patientId,
        @NotNull(message = "El médico es obligatorio")
        @Schema(description = "Debe ser el propio médico que prescribe") Long doctorId,
        @NotNull(message = "El medicamento es obligatorio") Long medicationId,
        @Positive(message = "La dosis diaria debe ser mayor que 0")
        @DecimalMax(value = "100", message = "La dosis diaria no puede superar 100 unidades")
        @Schema(description = "Unidades de medicamento al día", example = "2") double dailyDose,
        @NotBlank(message = "Las indicaciones son obligatorias")
        @Size(max = 255, message = "Las indicaciones no pueden superar los 255 caracteres")
        @Schema(example = "1 comprimido cada 12 horas, después de las comidas") String instructions,
        @NotNull(message = "La fecha de fin es obligatoria")
        @Future(message = "La fecha de fin debe ser posterior a hoy")
        @Schema(description = "Posterior a hoy; la prescripción empieza hoy", example = "2030-03-04") LocalDate endDate) {

    public NewPrescriptionRequest {
        instructions = InputNormalizer.trim(instructions);
    }
}
